package com.turfbook.backend.service.subscription;

import com.turfbook.backend.dto.RejectChangeRequest;
import com.turfbook.backend.dto.Subscription;
import com.turfbook.backend.dto.SubscriptionChangeRequest;
import com.turfbook.backend.dto.SubscriptionCreateRequest;
import com.turfbook.backend.dto.SubscriptionEditRequest;
import com.turfbook.backend.dto.SubscriptionPage;
import com.turfbook.backend.dto.SubscriptionPlan;
import com.turfbook.backend.dto.UpdatePlanRequest;
import com.turfbook.backend.dto.UpgradeRequestCreate;
import com.turfbook.backend.dto.VenueSubscriptionView;
import com.turfbook.backend.entity.ActivationSource;
import com.turfbook.backend.entity.BillingCycle;
import com.turfbook.backend.entity.FeatureCode;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SubscriptionChangeRequestEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionPaymentEntity;
import com.turfbook.backend.entity.SubscriptionPlanEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.SubscriptionChangeRequestRepository;
import com.turfbook.backend.repository.SubscriptionPaymentRepository;
import com.turfbook.backend.repository.SubscriptionPlanRepository;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService, SubscriptionGate {

    /** Subscriptions that count as "current" (only one allowed per venue). */
    private static final List<SubscriptionStatus> NON_TERMINAL =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private static final int OWNER_HISTORY_LIMIT = 5;
    private static final int ADMIN_HISTORY_LIMIT = 10;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionChangeRequestRepository changeRequestRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final SubscriptionMapper mapper;
    private final SubscriptionDateCalculator dates;
    private final NotificationService notificationService;

    // ═══════════════════════════════════════════════════════════════════════
    //  Admin: plan catalog
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlan> adminListPlans() {
        return mapper.toPlanDtos(planRepository.findAllByOrderByDisplayOrderAscIdAsc());
    }

    @Override
    @Transactional
    public SubscriptionPlan adminUpdatePlan(Long planId, UpdatePlanRequest req) {
        SubscriptionPlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", planId));

        if (req.getName() != null) plan.setName(req.getName());
        if (req.getMaxCourts() != null) plan.setMaxCourts(req.getMaxCourts());
        if (req.getPriceMonthly() != null) plan.setPriceMonthly(req.getPriceMonthly());
        if (req.getPriceAnnual() != null) plan.setPriceAnnual(req.getPriceAnnual());
        if (req.getFeatures() != null) plan.setFeatures(validateFeatures(req.getFeatures()));
        if (req.getPhotoLimit() != null) plan.setPhotoLimit(req.getPhotoLimit());
        if (req.getPlacementWeight() != null) plan.setPlacementWeight(req.getPlacementWeight());
        if (req.getTrialDays() != null) plan.setTrialDays(req.getTrialDays());
        if (req.getActive() != null) plan.setActive(req.getActive());
        if (req.getDisplayOrder() != null) plan.setDisplayOrder(req.getDisplayOrder());

        return mapper.toPlanDto(planRepository.save(plan));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Admin: subscriptions
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public Subscription adminCreateSubscription(SubscriptionCreateRequest req, Long adminId) {
        VenueEntity venue = venueRepository.findById(req.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", req.getVenueId()));
        UserEntity owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getOwnerId()));
        if (!venue.getOwner().getId().equals(owner.getId())) {
            throw new BadRequestException("Owner does not match the venue's owner");
        }
        SubscriptionPlanEntity plan = planRepository.findById(req.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", req.getPlanId()));

        if (currentEntity(venue).isPresent()) {
            throw new ConflictException(
                    "This venue already has an active subscription. Edit or renew it instead of creating a new one.");
        }

        BillingCycle cycle = parseCycle(req.getBillingCycle());
        boolean asTrial = Boolean.TRUE.equals(req.getAsTrial());

        // Downgrade-vs-existing-courts guard (relevant if courts were added under a prior plan).
        long courts = courtRepository.countByVenue(venue);
        if (courts > plan.getMaxCourts()) {
            throw new ConflictException(conflictMessage(plan, courts));
        }

        SubscriptionEntity sub = SubscriptionEntity.builder()
                .owner(owner)
                .venue(venue)
                .plan(plan)
                .billingCycle(cycle)
                .currency(plan.getCurrency())
                .activationSource(ActivationSource.ADMIN_MANUAL)
                .notes(req.getNotes())
                .build();
        applySnapshotAndActivate(sub, plan, cycle, asTrial);
        sub = subscriptionRepository.save(sub);

        recordPayment(sub, asTrial ? 0 : sub.getPrice(), req.getPaymentMethod(), req.getPaymentReference(), adminId);
        recomputeVenueLive(venue);

        notifyOwner(owner, venue, asTrial
                ? String.format("Your venue '%s' is now live on a %d-day trial of %s.",
                    venue.getName(), plan.getTrialDays(), plan.getName())
                : String.format("Your venue '%s' is now live on the %s plan.", venue.getName(), plan.getName()));

        log.info("Admin {} created subscription {} for venue {} (plan {}, trial={})",
                adminId, sub.getId(), venue.getId(), plan.getCode(), asTrial);
        return mapper.toDto(sub);
    }

    @Override
    @Transactional
    public Subscription adminEditSubscription(Long subscriptionId, SubscriptionEditRequest req) {
        SubscriptionEntity sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));
        if (sub.getStatus().isTerminal()) {
            throw new ConflictException("Cannot edit a terminal subscription. Create a new one instead.");
        }

        SubscriptionPlanEntity plan = sub.getPlan();
        if (req.getPlanId() != null && !req.getPlanId().equals(plan.getId())) {
            plan = planRepository.findById(req.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", req.getPlanId()));
        }
        BillingCycle cycle = req.getBillingCycle() != null ? parseCycle(req.getBillingCycle()) : sub.getBillingCycle();

        // Reject a downgrade that would strand existing courts.
        long courts = courtRepository.countByVenue(sub.getVenue());
        if (courts > plan.getMaxCourts()) {
            throw new ConflictException(conflictMessage(plan, courts));
        }

        // Preserve periodStart; recompute periodEnd for the (possibly new) plan/cycle.
        sub.setPlan(plan);
        sub.setBillingCycle(cycle);
        snapshot(sub, plan, cycle);
        LocalDateTime start = sub.getPeriodStart();
        if (sub.getStatus() == SubscriptionStatus.TRIALING) {
            LocalDateTime trialEnd = dates.trialEnd(start, plan.getTrialDays());
            sub.setTrialEnd(trialEnd);
            sub.setPeriodEnd(trialEnd);
        } else {
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setTrialEnd(null);
            sub.setPeriodEnd(dates.periodEnd(start, cycle));
        }
        if (req.getNotes() != null) sub.setNotes(req.getNotes());
        sub = subscriptionRepository.save(sub);

        recomputeVenueLive(sub.getVenue());
        log.info("Admin edited subscription {} -> plan {}, cycle {}", sub.getId(), plan.getCode(), cycle);
        return mapper.toDto(sub);
    }

    @Override
    @Transactional
    public Subscription adminVoidSubscription(Long subscriptionId) {
        SubscriptionEntity sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));
        sub.setStatus(SubscriptionStatus.VOIDED);
        sub = subscriptionRepository.save(sub);
        recomputeVenueLive(sub.getVenue());
        notifyOwner(sub.getOwner(), sub.getVenue(),
                String.format("Your venue '%s' subscription was voided and the venue is no longer live.",
                        sub.getVenue().getName()));
        log.info("Voided subscription {} (venue {})", sub.getId(), sub.getVenue().getId());
        return mapper.toDto(sub);
    }

    @Override
    @Transactional
    public Subscription adminRenewSubscription(Long subscriptionId, Long adminId) {
        SubscriptionEntity sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));
        if (sub.getStatus() == SubscriptionStatus.VOIDED) {
            throw new ConflictException("A voided subscription cannot be renewed. Create a new one.");
        }
        // A renew must remain the single current row for the venue: block if a *different*
        // non-terminal subscription exists.
        Optional<SubscriptionEntity> current = currentEntity(sub.getVenue());
        if (current.isPresent() && !current.get().getId().equals(sub.getId())) {
            throw new ConflictException("Another active subscription exists for this venue.");
        }

        SubscriptionPlanEntity plan = sub.getPlan();
        BillingCycle cycle = sub.getBillingCycle();
        snapshot(sub, plan, cycle);
        LocalDateTime now = dates.now();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setTrialEnd(null);
        sub.setPeriodStart(now);
        sub.setPeriodEnd(dates.periodEnd(now, cycle));
        sub = subscriptionRepository.save(sub);

        recordPayment(sub, sub.getPrice(), null, "RENEWAL", adminId);
        recomputeVenueLive(sub.getVenue());
        notifyOwner(sub.getOwner(), sub.getVenue(),
                String.format("Your venue '%s' subscription was renewed and is live again.", sub.getVenue().getName()));
        log.info("Renewed subscription {} (venue {}) to {}", sub.getId(), sub.getVenue().getId(), sub.getPeriodEnd());
        return mapper.toDto(sub);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPage adminListSubscriptions(Long venueId, Long ownerId, String status, int page, int size) {
        SubscriptionStatus statusFilter = parseStatusOrNull(status);
        Pageable pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 20 : size);
        Page<SubscriptionEntity> result = subscriptionRepository.search(venueId, ownerId, statusFilter, pageable);
        return new SubscriptionPage()
                .content(mapper.toDtos(result.getContent()))
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .size(result.getSize())
                .number(result.getNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSubscriptionView adminGetVenueSubscription(Long venueId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        return buildView(venue, ADMIN_HISTORY_LIMIT);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Admin: change requests
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionChangeRequest> adminListChangeRequests(String status) {
        SubscriptionChangeRequestEntity.Status s = parseChangeStatus(status);
        return mapper.toChangeRequestDtos(changeRequestRepository.findByStatusOrderByCreatedAtAsc(s));
    }

    @Override
    @Transactional
    public Subscription adminActivateChangeRequest(Long requestId, Long adminId) {
        SubscriptionChangeRequestEntity request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionChangeRequest", "id", requestId));
        if (request.getStatus() != SubscriptionChangeRequestEntity.Status.PENDING) {
            throw new ConflictException("This change request has already been decided.");
        }
        VenueEntity venue = request.getVenue();
        SubscriptionPlanEntity plan = request.getRequestedPlan();
        BillingCycle cycle = request.getRequestedCycle();

        long courts = courtRepository.countByVenue(venue);
        if (courts > plan.getMaxCourts()) {
            throw new ConflictException(conflictMessage(plan, courts));
        }

        // Supersede the current subscription, if any, then activate the requested plan fresh.
        currentEntity(venue).ifPresent(cur -> {
            cur.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(cur);
        });

        SubscriptionEntity sub = SubscriptionEntity.builder()
                .owner(request.getOwner())
                .venue(venue)
                .plan(plan)
                .billingCycle(cycle)
                .currency(plan.getCurrency())
                .activationSource(ActivationSource.ADMIN_MANUAL)
                .build();
        applySnapshotAndActivate(sub, plan, cycle, false);
        sub = subscriptionRepository.save(sub);

        request.setStatus(SubscriptionChangeRequestEntity.Status.APPROVED);
        request.setDecidedAt(dates.now());
        changeRequestRepository.save(request);

        recordPayment(sub, sub.getPrice(), null, "UPGRADE", adminId);
        recomputeVenueLive(venue);
        notifyOwner(request.getOwner(), venue,
                String.format("Your upgrade to the %s plan for '%s' is now active.", plan.getName(), venue.getName()));
        log.info("Activated change request {} -> subscription {} (plan {})", requestId, sub.getId(), plan.getCode());
        return mapper.toDto(sub);
    }

    @Override
    @Transactional
    public SubscriptionChangeRequest adminRejectChangeRequest(Long requestId, RejectChangeRequest req) {
        SubscriptionChangeRequestEntity request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionChangeRequest", "id", requestId));
        if (request.getStatus() != SubscriptionChangeRequestEntity.Status.PENDING) {
            throw new ConflictException("This change request has already been decided.");
        }
        request.setStatus(SubscriptionChangeRequestEntity.Status.REJECTED);
        request.setReason(req.getReason());
        request.setDecidedAt(dates.now());
        request = changeRequestRepository.save(request);
        notifyOwner(request.getOwner(), request.getVenue(),
                String.format("Your plan-change request for '%s' was declined: %s",
                        request.getVenue().getName(), req.getReason()));
        return mapper.toChangeRequestDto(request);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Owner
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlan> ownerListActivePlans() {
        return mapper.toPlanDtos(planRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSubscriptionView ownerGetVenueSubscription(Long venueId, Long ownerId) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        return buildView(venue, OWNER_HISTORY_LIMIT);
    }

    @Override
    @Transactional
    public SubscriptionChangeRequest ownerCreateUpgradeRequest(Long venueId, Long ownerId, UpgradeRequestCreate req) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        if (changeRequestRepository.existsByVenueAndStatus(venue, SubscriptionChangeRequestEntity.Status.PENDING)) {
            throw new ConflictException("There is already a pending plan-change request for this venue.");
        }
        SubscriptionPlanEntity plan = planRepository.findById(req.getRequestedPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", req.getRequestedPlanId()));
        if (!plan.isActive()) {
            throw new BadRequestException("That plan is not available.");
        }
        SubscriptionChangeRequestEntity entity = SubscriptionChangeRequestEntity.builder()
                .owner(venue.getOwner())
                .venue(venue)
                .currentSubscription(currentEntity(venue).orElse(null))
                .requestedPlan(plan)
                .requestedCycle(parseCycle(req.getBillingCycle()))
                .status(SubscriptionChangeRequestEntity.Status.PENDING)
                .build();
        entity = changeRequestRepository.save(entity);
        log.info("Owner {} requested upgrade of venue {} to plan {}", ownerId, venueId, plan.getCode());
        return mapper.toChangeRequestDto(entity);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SubscriptionGate (enforcement reads for other services)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionEntity> currentSubscription(Long venueId) {
        return venueRepository.findById(venueId).flatMap(this::currentEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVenueLive(Long venueId) {
        VenueEntity venue = venueRepository.findById(venueId).orElse(null);
        return venue != null
                && venue.getStatus() == VenueEntity.VenueStatus.LIVE
                && liveSubscription(venue).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasFeature(Long venueId, FeatureCode feature) {
        return venueRepository.findById(venueId)
                .flatMap(this::liveSubscription)
                .map(sub -> sub.getFeatureCodes().contains(feature))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ownerHasFeatureOnAnyVenue(Long ownerId, FeatureCode feature) {
        LocalDateTime now = dates.now();
        return subscriptionRepository
                .findByOwner_IdAndStatusIn(ownerId,
                        List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE))
                .stream()
                .anyMatch(sub -> !now.isAfter(sub.getPeriodEnd()) && sub.getFeatureCodes().contains(feature));
    }

    @Override
    public void assertFeature(Long venueId, FeatureCode feature, String message) {
        if (!hasFeature(venueId, feature)) {
            throw new ForbiddenException(message);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int maxCourtsFor(Long venueId) {
        return venueRepository.findById(venueId)
                .flatMap(this::currentEntity)
                .map(SubscriptionEntity::getMaxCourts)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public int photoLimitFor(Long venueId) {
        return venueRepository.findById(venueId)
                .flatMap(this::currentEntity)
                .map(sub -> sub.getPlan().getPhotoLimit())
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanAddCourt(Long venueId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        SubscriptionEntity sub = currentEntity(venue)
                .orElseThrow(() -> new ConflictException(
                        "This venue has no active subscription. Activate a plan to add courts."));
        long courts = courtRepository.countByVenue(venue);
        if (courts >= sub.getMaxCourts()) {
            throw new ConflictException(String.format(
                    "Court limit reached for the %s plan (%d courts). Upgrade to add more.",
                    sub.getPlanName(), sub.getMaxCourts()));
        }
    }

    @Override
    @Transactional
    public void recomputeVenueLiveFlag(Long venueId) {
        venueRepository.findById(venueId).ifPresent(this::recomputeVenueLive);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Internal helpers
    // ═══════════════════════════════════════════════════════════════════════

    /** The single non-terminal subscription for a venue (TRIALING/ACTIVE/PAST_DUE). */
    private Optional<SubscriptionEntity> currentEntity(VenueEntity venue) {
        return subscriptionRepository.findFirstByVenueAndStatusInOrderByIdDesc(venue, NON_TERMINAL);
    }

    /** A current subscription that is actually live-gating right now (TRIALING/ACTIVE within period). */
    private Optional<SubscriptionEntity> liveSubscription(VenueEntity venue) {
        return currentEntity(venue).filter(sub ->
                sub.getStatus().isLiveGating() && !dates.now().isAfter(sub.getPeriodEnd()));
    }

    /** Snapshot plan values onto the subscription and compute its period dates. */
    private void applySnapshotAndActivate(SubscriptionEntity sub, SubscriptionPlanEntity plan,
                                          BillingCycle cycle, boolean asTrial) {
        snapshot(sub, plan, cycle);
        LocalDateTime now = dates.now();
        sub.setPeriodStart(now);
        if (asTrial) {
            sub.setStatus(SubscriptionStatus.TRIALING);
            LocalDateTime trialEnd = dates.trialEnd(now, plan.getTrialDays());
            sub.setTrialEnd(trialEnd);
            sub.setPeriodEnd(trialEnd);
        } else {
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setTrialEnd(null);
            sub.setPeriodEnd(dates.periodEnd(now, cycle));
        }
    }

    /** Copy plan snapshot fields (planCode/planName/price/maxCourts/features/currency). */
    private void snapshot(SubscriptionEntity sub, SubscriptionPlanEntity plan, BillingCycle cycle) {
        sub.setPlanCode(plan.getCode());
        sub.setPlanName(plan.getName());
        sub.setMaxCourts(plan.getMaxCourts());
        sub.setPrice(cycle == BillingCycle.ANNUAL ? plan.getPriceAnnual() : plan.getPriceMonthly());
        sub.setCurrency(plan.getCurrency());
        sub.setFeatures(new ArrayList<>(plan.getFeatures()));
    }

    /** Recompute the venue's denormalized live flag + placement weight from its current sub. */
    private void recomputeVenueLive(VenueEntity venue) {
        Optional<SubscriptionEntity> live = liveSubscription(venue);
        boolean active = live.isPresent();
        venue.setSubscriptionActive(active);
        int weight = 0;
        boolean featured = false;
        if (active) {
            List<FeatureCode> features = live.get().getFeatureCodes();
            if (features.contains(FeatureCode.PRIORITY_PLACEMENT)) {
                weight = live.get().getPlan().getPlacementWeight();
            }
            featured = features.contains(FeatureCode.FEATURED_BADGE);
        }
        venue.setPlacementWeight(weight);
        venue.setFeatured(featured);
        venueRepository.save(venue);
    }

    private void recordPayment(SubscriptionEntity sub, int amount, Object methodEnum, String reference, Long adminId) {
        UserEntity admin = adminId == null ? null : userRepository.findById(adminId).orElse(null);
        SubscriptionPaymentEntity.Method method = SubscriptionPaymentEntity.Method.CASH;
        if (methodEnum != null) {
            try {
                method = SubscriptionPaymentEntity.Method.valueOf(methodEnum.toString());
            } catch (IllegalArgumentException ignored) {
                // fall back to CASH on an unrecognized method string
            }
        }
        SubscriptionPaymentEntity payment = SubscriptionPaymentEntity.builder()
                .subscription(sub)
                .amount(amount)
                .currency(sub.getCurrency())
                .method(method)
                .status(SubscriptionPaymentEntity.Status.RECORDED)
                .paidAt(dates.now())
                .recordedByAdmin(admin)
                .reference(reference)
                .build();
        paymentRepository.save(payment);
    }

    private VenueSubscriptionView buildView(VenueEntity venue, int historyLimit) {
        SubscriptionEntity current = currentEntity(venue).orElse(null);
        int courtsUsed = (int) courtRepository.countByVenue(venue);
        int courtsAllowed = current != null ? current.getMaxCourts() : 0;
        List<SubscriptionEntity> history = subscriptionRepository.findByVenue_IdOrderByIdDesc(
                venue.getId(), PageRequest.of(0, historyLimit));
        SubscriptionChangeRequestEntity pending = changeRequestRepository
                .findFirstByVenueAndStatusOrderByCreatedAtDesc(venue, SubscriptionChangeRequestEntity.Status.PENDING)
                .orElse(null);
        return mapper.toView(current, courtsUsed, courtsAllowed, history, pending);
    }

    private VenueEntity requireOwnedVenue(Long venueId, Long ownerId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("This venue does not belong to you.");
        }
        return venue;
    }

    private void notifyOwner(UserEntity owner, VenueEntity venue, String message) {
        try {
            notificationService.createNotification(owner, "Subscription update", message,
                    NotificationEntity.NotificationType.SYSTEM);
        } catch (Exception e) {
            log.warn("Failed to send subscription notification for venue {}: {}", venue.getId(), e.getMessage());
        }
    }

    private List<String> validateFeatures(List<String> features) {
        List<String> out = new ArrayList<>(features.size());
        for (String f : features) {
            try {
                out.add(FeatureCode.valueOf(f).name());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown feature code: " + f);
            }
        }
        return out;
    }

    private String conflictMessage(SubscriptionPlanEntity plan, long courts) {
        return String.format(
                "This venue has %d courts but the %s plan allows only %d. Remove courts or choose a higher plan.",
                courts, plan.getName(), plan.getMaxCourts());
    }

    private BillingCycle parseCycle(Object cycle) {
        if (cycle == null) throw new BadRequestException("billingCycle is required");
        try {
            return BillingCycle.valueOf(cycle.toString());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid billingCycle: " + cycle);
        }
    }

    private SubscriptionStatus parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return SubscriptionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid subscription status: " + status);
        }
    }

    private SubscriptionChangeRequestEntity.Status parseChangeStatus(String status) {
        if (status == null || status.isBlank()) return SubscriptionChangeRequestEntity.Status.PENDING;
        try {
            return SubscriptionChangeRequestEntity.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid change-request status: " + status);
        }
    }
}
