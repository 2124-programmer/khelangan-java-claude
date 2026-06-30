package com.turfbook.backend.service.subscription;

import com.turfbook.backend.dto.ActivateChangeRequest;
import com.turfbook.backend.dto.CourtChangeRequest;
import com.turfbook.backend.dto.CreateCourtChangeRequestBody;
import com.turfbook.backend.dto.CourtSelectionBody;
import com.turfbook.backend.dto.PaidRequestBody;
import com.turfbook.backend.dto.PendingRequestRef;
import com.turfbook.backend.dto.PlanOption;
import com.turfbook.backend.dto.RejectChangeRequest;
import com.turfbook.backend.dto.SelectableCourt;
import com.turfbook.backend.dto.Subscription;
import com.turfbook.backend.dto.SubscriptionChangeRequest;
import com.turfbook.backend.dto.SubscriptionRequestView;
import com.turfbook.backend.dto.VenueSubscriptionState;
import com.turfbook.backend.dto.SubscriptionCreateRequest;
import com.turfbook.backend.dto.SubscriptionEditRequest;
import com.turfbook.backend.dto.SubscriptionPage;
import com.turfbook.backend.dto.SubscriptionPlan;
import com.turfbook.backend.dto.UpdatePlanRequest;
import com.turfbook.backend.dto.StageKey;
import com.turfbook.backend.dto.StageState;
import com.turfbook.backend.dto.SubscriptionOwnerRef;
import com.turfbook.backend.dto.SubscriptionTimeline;
import com.turfbook.backend.dto.SubscriptionVenueRef;
import com.turfbook.backend.dto.TimelineStage;
import com.turfbook.backend.dto.UpgradeRequestCreate;
import com.turfbook.backend.dto.VenueSubscriptionPage;
import com.turfbook.backend.dto.VenueSubscriptionRow;
import com.turfbook.backend.dto.VenueSubscriptionView;
import com.turfbook.backend.entity.ActivationSource;
import com.turfbook.backend.entity.BillingCycle;
import com.turfbook.backend.entity.CourtChangeRequestEntity;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.FeatureCode;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.PlanCode;
import com.turfbook.backend.entity.SubscriptionChangeRequestEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionPaymentEntity;
import com.turfbook.backend.entity.SubscriptionPlanEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.entity.VenueLifecycleEventEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.CourtLimitExceededException;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.SubscriptionEligibilityException;
import com.turfbook.backend.repository.CourtChangeRequestRepository;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.SubscriptionChangeRequestRepository;
import com.turfbook.backend.repository.SubscriptionPaymentRepository;
import com.turfbook.backend.repository.SubscriptionPlanRepository;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueLifecycleEventRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.service.AdminPermissionService;
import com.turfbook.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService, SubscriptionGate {

    /** Subscriptions that count as "current" (only one allowed per venue). */
    private static final List<SubscriptionStatus> NON_TERMINAL =
            List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private static final int OWNER_HISTORY_LIMIT = 5;
    private static final int ADMIN_HISTORY_LIMIT = 10;
    /** A subscription within this many days of its end date rolls up as EXPIRING in the table. */
    private static final int EXPIRING_SOON_DAYS = 7;
    /** The self-serve trial covers at most this many courts, regardless of plan tuning. */
    private static final int TRIAL_COURT_LIMIT = 2;
    /** India Standard Time has no DST, so a fixed offset is exact. */
    private static final ZoneOffset IST_OFFSET = ZoneOffset.of("+05:30");

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionChangeRequestRepository changeRequestRepository;
    private final CourtChangeRequestRepository courtChangeRequestRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final VenueLifecycleEventRepository lifecycleEventRepository;
    private final SubscriptionMapper mapper;
    private final SubscriptionDateCalculator dates;
    private final NotificationService notificationService;
    private final AdminPermissionService adminPermissionService;

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

        // Court-coverage model: the plan limit caps the COVERED courts (defaulted below, capped at
        // maxCourts), not the venue's total court count — a venue may have more courts than the plan
        // covers (the extras simply stay unbookable). So there is no total-court guard here.

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
        // Admin manual create carries no court picker — default coverage to the venue's active
        // courts (capped at the plan limit) so the venue stays bookable as it did pre-coverage.
        sub.setCoveredCourtIds(defaultCoverage(venue, plan.getMaxCourts()));
        sub = subscriptionRepository.save(sub);

        recordPayment(sub, asTrial ? 0 : sub.getPrice(), req.getPaymentMethod(), req.getPaymentReference(), adminId);
        recomputeVenueLive(venue);
        recordLifecycle(venue, asTrial
                ? VenueLifecycleEventEntity.Type.TRIAL_ACTIVATED
                : VenueLifecycleEventEntity.Type.SUBSCRIPTION_STARTED, plan.getName());

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

        // Court-coverage model: maxCourts caps covered courts, not total courts. Coverage is
        // trimmed to the new plan's limit below, so no total-court guard is needed here.

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
        // Keep the owner's court selection, but never exceed the (possibly new) plan limit; seed a
        // default if this subscription predates court coverage. If a downgrade forces some live
        // courts to be locked, courts are NEVER deleted and existing bookings are untouched — the
        // newest-id excess are locked (no new bookings) and the owner is told which + why, so they
        // can re-pick a different live set or upgrade. (Owner-initiated changes choose coverage up
        // front via the subscription-request flow; this branch only covers admin-side edits.)
        List<String> before = new ArrayList<>(sub.getCoveredCourtIds() == null ? List.of() : sub.getCoveredCourtIds());
        List<String> cov = new ArrayList<>(before);
        if (cov.isEmpty()) cov = defaultCoverage(sub.getVenue(), plan.getMaxCourts());
        else if (cov.size() > plan.getMaxCourts()) cov = new ArrayList<>(cov.subList(0, plan.getMaxCourts()));
        sub.setCoveredCourtIds(cov);
        sub = subscriptionRepository.save(sub);

        recomputeVenueLive(sub.getVenue());
        recordLifecycle(sub.getVenue(), VenueLifecycleEventEntity.Type.SUBSCRIPTION_CHANGED, plan.getName());
        notifyIfCourtsLocked(sub.getVenue(), before, cov, plan.getName());
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
        recordLifecycle(sub.getVenue(), VenueLifecycleEventEntity.Type.SUSPENDED, sub.getPlanName());
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
        recordLifecycle(sub.getVenue(), VenueLifecycleEventEntity.Type.SUBSCRIPTION_RENEWED, sub.getPlanName());
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
        VenueSubscriptionView view = buildView(venue, ADMIN_HISTORY_LIMIT);
        // Admin detail adds venue/owner context + the lifecycle timeline (owner view leaves these null).
        UserEntity owner = venue.getOwner();
        view.setVenue(new SubscriptionVenueRef()
                .id(venue.getId())
                .name(venue.getName())
                .city(venue.getCity())
                .courtCount((int) courtRepository.countByVenue(venue))
                .registeredAt(odt(venue.getCreatedAt()))
                .approvedAt(odt(venue.getApprovedAt())));
        if (owner != null) {
            view.setOwner(new SubscriptionOwnerRef()
                    .id(owner.getId())
                    .name(owner.getName())
                    .mobile(owner.getPhone())
                    .email(owner.getEmail()));
        }
        view.setTimeline(buildTimeline(venue));
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSubscriptionPage adminListVenueSubscriptions(String q, String status, int page, int size) {
        String term = (q == null || q.isBlank()) ? null : q.trim();
        String filter = (status == null || status.isBlank()) ? "ALL" : status.trim().toUpperCase();
        int sz = size <= 0 ? 15 : size;
        int pg = Math.max(0, page);

        List<VenueSubscriptionRow> rows = new ArrayList<>();
        for (VenueEntity venue : venueRepository.searchForSubscriptionTable(term, VenueEntity.VenueStatus.DRAFT)) {
            VenueSubscriptionRow row = buildRow(venue);
            if (matchesStatusFilter(row.getCurrentStatus(), filter)) {
                rows.add(row);
            }
        }

        int total = rows.size();
        int from = Math.min(pg * sz, total);
        int to = Math.min(from + sz, total);
        int totalPages = (int) Math.ceil((double) total / sz);
        return new VenueSubscriptionPage()
                .content(new ArrayList<>(rows.subList(from, to)))
                .totalElements((long) total)
                .totalPages(totalPages)
                .size(sz)
                .number(pg);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Admin: change requests
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionChangeRequest> adminListChangeRequests(String status) {
        SubscriptionChangeRequestEntity.Status s = parseChangeStatus(status);
        return mapper.toChangeRequestDtos(changeRequestRepository.findByStatusWithRefs(s));
    }

    @Override
    @Transactional
    public Subscription adminActivateChangeRequest(Long requestId, ActivateChangeRequest req, Long adminId) {
        SubscriptionChangeRequestEntity request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionChangeRequest", "id", requestId));
        if (request.getStatus() != SubscriptionChangeRequestEntity.Status.PENDING) {
            throw new ConflictException("This change request has already been decided.");
        }
        VenueEntity venue = request.getVenue();
        SubscriptionPlanEntity plan = request.getRequestedPlan();
        BillingCycle cycle = request.getRequestedCycle();

        // Court-coverage model: the plan limit caps the COVERED courts — NOT the venue's total court
        // count. A 2-court Starter covering 2 of 4 courts is valid; the other courts stay unbookable.
        // The admin may override the owner's selection at activation time (e.g. after confirming which
        // courts the cash payment covers); otherwise the owner's requested selection is used (capped).
        SubscriptionEntity currentSub = currentEntity(venue).orElse(null);
        List<String> coverage;
        if (req != null && req.getCourtIds() != null && !req.getCourtIds().isEmpty()) {
            coverage = validateSelection(req.getCourtIds(), courtRepository.findByVenue(venue), plan.getMaxCourts());
        } else {
            coverage = request.getCoveredCourtIds() == null
                    ? new ArrayList<>() : new ArrayList<>(request.getCoveredCourtIds());
            if (coverage.size() > plan.getMaxCourts()) {
                coverage = new ArrayList<>(coverage.subList(0, plan.getMaxCourts()));
            }
        }

        // Re-request on the SAME (already-paid) plan + cycle is a pure court-coverage change:
        // update the live subscription's courts in place — keep the billing period and record no
        // new charge. (A trial → paid, or any plan/cycle change, supersedes instead — see below.)
        boolean coverageOnly = currentSub != null
                && (currentSub.getStatus() == SubscriptionStatus.ACTIVE
                    || currentSub.getStatus() == SubscriptionStatus.PAST_DUE)
                && currentSub.getPlanCode() == plan.getCode()
                && currentSub.getBillingCycle() == cycle;

        if (coverageOnly) {
            currentSub.setCoveredCourtIds(coverage);
            SubscriptionEntity saved = subscriptionRepository.save(currentSub);
            request.setStatus(SubscriptionChangeRequestEntity.Status.APPROVED);
            request.setDecidedAt(dates.now());
            changeRequestRepository.save(request);
            recomputeVenueLive(venue);
            recordLifecycle(venue, VenueLifecycleEventEntity.Type.SUBSCRIPTION_CHANGED, plan.getName() + " — courts updated");
            notifyOwner(request.getOwner(), venue, String.format(
                    "The courts covered by your %s plan for '%s' have been updated.", plan.getName(), venue.getName()));
            log.info("Activated change request {} as in-place coverage update on subscription {} (plan {})",
                    requestId, saved.getId(), plan.getCode());
            return mapper.toDto(saved);
        }

        // Otherwise (trial → paid, or a different plan/cycle): supersede the current subscription
        // and activate the requested plan fresh, with the selected courts as its coverage.
        if (currentSub != null) {
            currentSub.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(currentSub);
        }
        SubscriptionEntity sub = SubscriptionEntity.builder()
                .owner(request.getOwner())
                .venue(venue)
                .plan(plan)
                .billingCycle(cycle)
                .currency(plan.getCurrency())
                .activationSource(ActivationSource.ADMIN_MANUAL)
                .build();
        applySnapshotAndActivate(sub, plan, cycle, false);
        sub.setCoveredCourtIds(coverage);
        sub = subscriptionRepository.save(sub);

        request.setStatus(SubscriptionChangeRequestEntity.Status.APPROVED);
        request.setDecidedAt(dates.now());
        changeRequestRepository.save(request);

        recordPayment(sub, sub.getPrice(), null, "UPGRADE", adminId);
        recomputeVenueLive(venue);
        recordLifecycle(venue, VenueLifecycleEventEntity.Type.SUBSCRIPTION_CHANGED, plan.getName());
        notifyOwner(request.getOwner(), venue,
                String.format("Your %s plan for '%s' is now active.", plan.getName(), venue.getName()));
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

    @Override
    @Transactional(readOnly = true)
    public List<SelectableCourt> adminListChangeRequestCourts(Long requestId) {
        SubscriptionChangeRequestEntity request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionChangeRequest", "id", requestId));
        VenueEntity venue = request.getVenue();
        // Pre-mark the courts the OWNER asked to cover (not the venue's current coverage) so the admin
        // edits from the owner's request as the starting point.
        Set<String> requested = new HashSet<>(
                request.getCoveredCourtIds() == null ? List.of() : request.getCoveredCourtIds());
        return courtRepository.findByVenue(venue).stream()
                .sorted(Comparator.comparing(CourtEntity::getId))
                .map(c -> new SelectableCourt()
                        .courtId(String.valueOf(c.getId()))
                        .name(c.getName())
                        .sport(c.getSport() != null ? c.getSport().getName() : null)
                        .isActive(c.isActive())
                        .isCovered(requested.contains(String.valueOf(c.getId()))))
                .toList();
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

        // Surface the request to admins (the dashboard "Subscription Requests" queue).
        notificationService.notifyAdmins(
                "New subscription request",
                String.format("%s requested '%s' for venue '%s'.",
                        venue.getOwner() != null ? venue.getOwner().getName() : "An owner",
                        plan.getName(), venue.getName()),
                NotificationEntity.NotificationType.SYSTEM);

        return mapper.toChangeRequestDto(entity);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Owner: court-coverage purchase (self-serve trial + paid request)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<PlanOption> ownerListPlanOptions(Long venueId, Long ownerId) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        int totalCourts = (int) courtRepository.countByVenue(venue);
        boolean hasCurrent = currentEntity(venue).isPresent();
        boolean trialUsed = trialUsed(venue);
        boolean pending = changeRequestRepository.existsByVenueAndStatus(
                venue, SubscriptionChangeRequestEntity.Status.PENDING);

        List<PlanOption> options = new ArrayList<>();

        // Trial (self-serve), shown first.
        SubscriptionPlanEntity starter = planRepository.findByCode(PlanCode.STARTER).orElse(null);
        int trialDays = starter != null ? starter.getTrialDays() : 30;
        PlanOption trial = new PlanOption()
                .code(PlanOption.CodeEnum.TRIAL)
                .name("Trial")
                .kind(PlanOption.KindEnum.TRIAL)
                .price(0)
                .courtLimit(TRIAL_COURT_LIMIT)
                .durationDays(trialDays)
                .oncePerVenue(true)
                .available(totalCourts > 0 && !hasCurrent && !trialUsed);
        if (totalCourts == 0) trial.setUnavailableReason("Add at least one court first.");
        else if (trialUsed) trial.setUnavailableReason("Trial already used.");
        else if (hasCurrent) trial.setUnavailableReason("A subscription is already active.");
        options.add(trial);

        // Paid tiers, in catalog order.
        for (SubscriptionPlanEntity plan : planRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()) {
            PlanOption opt = new PlanOption()
                    .code(PlanOption.CodeEnum.fromValue(plan.getCode().name()))
                    .name(plan.getName())
                    .kind(PlanOption.KindEnum.PAID)
                    .price(plan.getPriceMonthly())
                    .courtLimit(plan.getMaxCourts())
                    .durationDays(30)
                    .oncePerVenue(false)
                    .available(totalCourts > 0 && !pending);
            if (totalCourts == 0) opt.setUnavailableReason("Add at least one court first.");
            else if (pending) opt.setUnavailableReason("A plan request is already pending.");
            options.add(opt);
        }
        return options;
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSubscriptionState ownerGetVenueSubscriptionState(Long venueId, Long ownerId) {
        return buildState(requireOwnedVenue(venueId, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SelectableCourt> ownerListSelectableCourts(Long venueId, Long ownerId) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        Set<String> covered = currentEntity(venue)
                .map(s -> new HashSet<>(s.getCoveredCourtIds()))
                .orElseGet(HashSet::new);
        return courtRepository.findByVenue(venue).stream()
                .sorted(Comparator.comparing(CourtEntity::getId))
                .map(c -> new SelectableCourt()
                        .courtId(String.valueOf(c.getId()))
                        .name(c.getName())
                        .sport(c.getSport() != null ? c.getSport().getName() : null)
                        .isActive(c.isActive())
                        .isCovered(covered.contains(String.valueOf(c.getId()))))
                .toList();
    }

    @Override
    @Transactional
    public VenueSubscriptionState ownerStartTrial(Long venueId, Long ownerId, CourtSelectionBody body) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        List<CourtEntity> courts = courtRepository.findByVenue(venue);
        if (courts.isEmpty()) {
            throw new SubscriptionEligibilityException("NO_COURTS",
                    "Add at least one court before purchasing a subscription.");
        }
        if (currentEntity(venue).isPresent()) {
            throw new SubscriptionEligibilityException("ACTIVE_SUBSCRIPTION_EXISTS",
                    "This venue already has an active subscription.");
        }
        if (trialUsed(venue)) {
            throw new SubscriptionEligibilityException("TRIAL_ALREADY_USED",
                    "The free trial has already been used for this venue.");
        }
        List<String> selected = validateSelection(body.getCourtIds(), courts, TRIAL_COURT_LIMIT);

        SubscriptionPlanEntity plan = planRepository.findByCode(PlanCode.STARTER)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "code", PlanCode.STARTER));
        SubscriptionEntity sub = SubscriptionEntity.builder()
                .owner(venue.getOwner())
                .venue(venue)
                .plan(plan)
                .billingCycle(BillingCycle.MONTHLY)
                .currency(plan.getCurrency())
                .activationSource(ActivationSource.ADMIN_MANUAL)
                .build();
        applySnapshotAndActivate(sub, plan, BillingCycle.MONTHLY, true); // trial
        sub.setMaxCourts(TRIAL_COURT_LIMIT);
        sub.setCoveredCourtIds(new ArrayList<>(selected));
        subscriptionRepository.save(sub);

        recomputeVenueLive(venue);
        recordLifecycle(venue, VenueLifecycleEventEntity.Type.TRIAL_ACTIVATED, "Trial");
        notifyOwner(venue.getOwner(), venue, String.format(
                "Your free trial for '%s' has started — %d court(s) are now live for players.",
                venue.getName(), selected.size()));
        log.info("Owner {} started trial for venue {} covering {} court(s)", ownerId, venueId, selected.size());
        return buildState(venue);
    }

    @Override
    @Transactional
    public SubscriptionRequestView ownerCreateSubscriptionRequest(Long venueId, Long ownerId, PaidRequestBody body) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        List<CourtEntity> courts = courtRepository.findByVenue(venue);
        if (courts.isEmpty()) {
            throw new SubscriptionEligibilityException("NO_COURTS",
                    "Add at least one court before purchasing a subscription.");
        }
        if (changeRequestRepository.existsByVenueAndStatus(venue, SubscriptionChangeRequestEntity.Status.PENDING)) {
            throw new SubscriptionEligibilityException("REQUEST_ALREADY_PENDING",
                    "There is already a pending plan request for this venue.");
        }
        PlanCode code = PlanCode.valueOf(body.getPlanCode().getValue());
        SubscriptionPlanEntity plan = planRepository.findByCode(code)
                .filter(SubscriptionPlanEntity::isActive)
                .orElseThrow(() -> new BadRequestException("That plan is not available."));
        List<String> selected = validateSelection(body.getCourtIds(), courts, plan.getMaxCourts());

        SubscriptionChangeRequestEntity entity = SubscriptionChangeRequestEntity.builder()
                .owner(venue.getOwner())
                .venue(venue)
                .currentSubscription(currentEntity(venue).orElse(null))
                .requestedPlan(plan)
                .requestedCycle(BillingCycle.MONTHLY)
                .coveredCourtIds(new ArrayList<>(selected))
                .status(SubscriptionChangeRequestEntity.Status.PENDING)
                .build();
        entity = changeRequestRepository.save(entity);

        notificationService.notifyAdmins(
                "New subscription request",
                String.format("%s requested '%s' (%d court(s)) for venue '%s'.",
                        venue.getOwner() != null ? venue.getOwner().getName() : "An owner",
                        plan.getName(), selected.size(), venue.getName()),
                NotificationEntity.NotificationType.SYSTEM);

        log.info("Owner {} requested paid plan {} for venue {} covering {} court(s)",
                ownerId, plan.getCode(), venueId, selected.size());
        return buildRequestView(entity);
    }

    @Override
    @Transactional
    public VenueSubscriptionState ownerCancelSubscriptionRequest(Long venueId, Long ownerId) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        SubscriptionChangeRequestEntity pending = changeRequestRepository
                .findFirstByVenueAndStatusOrderByCreatedAtDesc(venue, SubscriptionChangeRequestEntity.Status.PENDING)
                .orElseThrow(() -> new SubscriptionEligibilityException(
                        "NO_PENDING_REQUEST", "There is no pending plan request to cancel."));
        pending.setStatus(SubscriptionChangeRequestEntity.Status.CANCELLED);
        pending.setDecidedAt(dates.now());
        changeRequestRepository.save(pending);
        log.info("Owner {} cancelled subscription request {} for venue {}", ownerId, pending.getId(), venueId);
        return buildState(venue);
    }

    @Override
    @Transactional
    public VenueSubscriptionState ownerSetCourtLive(Long venueId, Long courtId, Long ownerId, boolean live) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        CourtEntity court = courtRepository.findByIdAndVenue(courtId, venue)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));
        // A live-gating subscription (trial/active, within period) must exist to cover courts.
        SubscriptionEntity sub = currentEntity(venue)
                .filter(s -> s.getStatus().isLiveGating())
                .orElseThrow(() -> new SubscriptionEligibilityException("NO_ACTIVE_SUBSCRIPTION",
                        "Start a plan or trial before making courts live for players."));

        List<String> covered = new ArrayList<>(
                sub.getCoveredCourtIds() == null ? List.of() : sub.getCoveredCourtIds());
        String cid = String.valueOf(courtId);

        if (live) {
            // DRAFT → LIVE: owners may activate a court into a FREE subscription slot themselves.
            if (covered.contains(cid)) return buildState(venue); // already live — idempotent
            if (!court.isActive()) {
                throw new SubscriptionEligibilityException("COURT_INACTIVE",
                        "Turn the court Active before making it live for players.");
            }
            if (covered.size() >= sub.getMaxCourts()) {
                // Single typed 409 — the owner must free a live slot or upgrade. No dead-end.
                throw new SubscriptionEligibilityException("COURT_LIVE_LIMIT", String.format(
                        "Your %s plan allows %d live court(s). Submit a court-change request or upgrade to add more.",
                        sub.getPlanName(), sub.getMaxCourts()));
            }
            covered.add(cid);
        } else {
            // LIVE → not-live: LOCKED for owners. Once a court holds a subscription slot, only a
            // super-admin may free/swap it (via a court-change request). Idempotent if already not live.
            if (!covered.contains(cid)) return buildState(venue);
            throw new SubscriptionEligibilityException("COURT_LIVE_LOCKED",
                    "A live court can't be deactivated directly. Submit a court-change request for an admin to action.");
        }

        sub.setCoveredCourtIds(covered);
        subscriptionRepository.save(sub);
        recomputeVenueLive(venue);
        log.info("Owner {} set court {} live={} on venue {} ({} of {} live)",
                ownerId, courtId, live, venueId, covered.size(), sub.getMaxCourts());
        return buildState(venue);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Court-change requests (owner files; super-admin approves/rejects)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<CourtChangeRequest> ownerListCourtChangeRequests(Long venueId, Long ownerId) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        return courtChangeRequestRepository.findByOwnerOrderByCreatedAtDesc(venue.getOwner()).stream()
                .filter(r -> r.getVenue() != null && r.getVenue().getId().equals(venueId))
                .map(this::toCourtChangeDto)
                .toList();
    }

    @Override
    @Transactional
    public CourtChangeRequest ownerCreateCourtChangeRequest(Long venueId, Long ownerId, CreateCourtChangeRequestBody body) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        SubscriptionEntity sub = currentEntity(venue)
                .filter(s -> s.getStatus().isLiveGating())
                .orElseThrow(() -> new SubscriptionEligibilityException("NO_ACTIVE_SUBSCRIPTION",
                        "This venue has no active subscription."));
        if (courtChangeRequestRepository.existsByVenueAndStatus(venue, CourtChangeRequestEntity.Status.PENDING)) {
            throw new SubscriptionEligibilityException("REQUEST_ALREADY_PENDING",
                    "There's already a pending court-change request for this venue.");
        }
        Long liveCourtId = body.getLiveCourtId();
        Long draftCourtId = body.getDraftCourtId();
        Set<String> covered = new HashSet<>(sub.getCoveredCourtIds());

        courtRepository.findByIdAndVenue(liveCourtId, venue)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", liveCourtId));
        if (!covered.contains(String.valueOf(liveCourtId))) {
            throw new SubscriptionEligibilityException("COURT_NOT_LIVE",
                    "The selected court isn't live, so there's nothing to free.");
        }
        if (draftCourtId != null) {
            if (draftCourtId.equals(liveCourtId)) {
                throw new SubscriptionEligibilityException("INVALID_SWAP",
                        "The court to free and the court to make live must be different.");
            }
            CourtEntity draftCourt = courtRepository.findByIdAndVenue(draftCourtId, venue)
                    .orElseThrow(() -> new ResourceNotFoundException("Court", "id", draftCourtId));
            if (covered.contains(String.valueOf(draftCourtId))) {
                throw new SubscriptionEligibilityException("COURT_ALREADY_LIVE",
                        "The court you want to make live is already live.");
            }
            if (!draftCourt.isActive()) {
                throw new SubscriptionEligibilityException("COURT_INACTIVE",
                        "Turn the replacement court Active before requesting to make it live.");
            }
        }

        CourtChangeRequestEntity req = courtChangeRequestRepository.save(CourtChangeRequestEntity.builder()
                .owner(venue.getOwner())
                .venue(venue)
                .liveCourtId(liveCourtId)
                .draftCourtId(draftCourtId)
                .reason(body.getReason())
                .status(CourtChangeRequestEntity.Status.PENDING)
                .build());

        notificationService.notifyAdmins("Court-change request",
                String.format("%s requested a court change for venue '%s'.",
                        venue.getOwner() != null ? venue.getOwner().getName() : "An owner", venue.getName()),
                NotificationEntity.NotificationType.SYSTEM);
        log.info("Owner {} filed court-change request {} for venue {} (free {}, swap-in {})",
                ownerId, req.getId(), venueId, liveCourtId, draftCourtId);
        return toCourtChangeDto(req);
    }

    @Override
    @Transactional
    public CourtChangeRequest ownerCancelCourtChangeRequest(Long venueId, Long requestId, Long ownerId) {
        VenueEntity venue = requireOwnedVenue(venueId, ownerId);
        CourtChangeRequestEntity req = courtChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CourtChangeRequest", "id", requestId));
        if (req.getVenue() == null || !req.getVenue().getId().equals(venue.getId())) {
            throw new ForbiddenException("This request does not belong to that venue.");
        }
        if (req.getStatus() != CourtChangeRequestEntity.Status.PENDING) {
            throw new SubscriptionEligibilityException("NOT_PENDING", "Only a pending request can be cancelled.");
        }
        req.setStatus(CourtChangeRequestEntity.Status.CANCELLED);
        req.setDecidedAt(dates.now());
        courtChangeRequestRepository.save(req);
        return toCourtChangeDto(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtChangeRequest> adminListCourtChangeRequests(String status) {
        adminPermissionService.requireSuperAdmin(adminPermissionService.currentActorId());
        CourtChangeRequestEntity.Status st;
        try {
            st = (status == null || status.isBlank())
                    ? CourtChangeRequestEntity.Status.PENDING
                    : CourtChangeRequestEntity.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }
        return courtChangeRequestRepository.findByStatusWithRefs(st).stream()
                .map(this::toCourtChangeDto)
                .toList();
    }

    @Override
    @Transactional
    public CourtChangeRequest adminApproveCourtChangeRequest(Long id) {
        Long actorId = adminPermissionService.currentActorId();
        adminPermissionService.requireSuperAdmin(actorId);
        CourtChangeRequestEntity req = courtChangeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CourtChangeRequest", "id", id));
        if (req.getStatus() != CourtChangeRequestEntity.Status.PENDING) {
            throw new ConflictException("This request has already been decided.");
        }
        VenueEntity venue = req.getVenue();

        // Re-validate against CURRENT state — a request overtaken by another change is auto-rejected stale.
        SubscriptionEntity sub = currentEntity(venue).filter(s -> s.getStatus().isLiveGating()).orElse(null);
        if (sub == null) {
            return staleReject(req, actorId, "The venue no longer has an active subscription.");
        }
        List<String> covered = new ArrayList<>(sub.getCoveredCourtIds());
        String freeId = String.valueOf(req.getLiveCourtId());
        if (!covered.contains(freeId)) {
            return staleReject(req, actorId, "The court to free is no longer live (state changed).");
        }
        Long draftId = req.getDraftCourtId();
        if (draftId != null) {
            CourtEntity draftCourt = courtRepository.findByIdAndVenue(draftId, venue).orElse(null);
            if (draftCourt == null || !draftCourt.isActive() || covered.contains(String.valueOf(draftId))) {
                return staleReject(req, actorId, "The replacement court is no longer available (state changed).");
            }
        }

        covered.remove(freeId);
        if (draftId != null && covered.size() < sub.getMaxCourts()) {
            covered.add(String.valueOf(draftId));
        }
        sub.setCoveredCourtIds(covered);
        subscriptionRepository.save(sub);
        recomputeVenueLive(venue);

        req.setStatus(CourtChangeRequestEntity.Status.APPROVED);
        req.setDecidedBy(actorId);
        req.setDecidedAt(dates.now());
        courtChangeRequestRepository.save(req);

        String msg = draftId != null
                ? String.format("Your court-change request was approved: '%s' is now locked and '%s' is now live.",
                        courtName(req.getLiveCourtId()), courtName(draftId))
                : String.format("Your court-change request was approved: '%s' is now locked.", courtName(req.getLiveCourtId()));
        notifyOwner(venue.getOwner(), venue, msg);
        log.info("Super-admin {} approved court-change request {} (venue {})", actorId, id, venue.getId());
        return toCourtChangeDto(req);
    }

    @Override
    @Transactional
    public CourtChangeRequest adminRejectCourtChangeRequest(Long id, String reason) {
        Long actorId = adminPermissionService.currentActorId();
        adminPermissionService.requireSuperAdmin(actorId);
        CourtChangeRequestEntity req = courtChangeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CourtChangeRequest", "id", id));
        if (req.getStatus() != CourtChangeRequestEntity.Status.PENDING) {
            throw new ConflictException("This request has already been decided.");
        }
        req.setStatus(CourtChangeRequestEntity.Status.REJECTED);
        req.setDecisionNote(reason);
        req.setDecidedBy(actorId);
        req.setDecidedAt(dates.now());
        courtChangeRequestRepository.save(req);
        notifyOwner(req.getVenue().getOwner(), req.getVenue(),
                String.format("Your court-change request for '%s' was declined.%s", req.getVenue().getName(),
                        reason != null && !reason.isBlank() ? " Reason: " + reason : ""));
        return toCourtChangeDto(req);
    }

    /** Mark a request rejected because the state it targeted changed before approval; notify the owner. */
    private CourtChangeRequest staleReject(CourtChangeRequestEntity req, Long actorId, String note) {
        req.setStatus(CourtChangeRequestEntity.Status.REJECTED);
        req.setDecisionNote(note);
        req.setDecidedBy(actorId);
        req.setDecidedAt(dates.now());
        courtChangeRequestRepository.save(req);
        notifyOwner(req.getVenue().getOwner(), req.getVenue(),
                String.format("Your court-change request for '%s' couldn't be applied: %s",
                        req.getVenue().getName(), note));
        return toCourtChangeDto(req);
    }

    private String courtName(Long courtId) {
        return courtId == null ? null
                : courtRepository.findById(courtId).map(CourtEntity::getName).orElse(null);
    }

    private CourtChangeRequest toCourtChangeDto(CourtChangeRequestEntity e) {
        CourtChangeRequest dto = new CourtChangeRequest();
        dto.setId(e.getId());
        dto.setVenueId(e.getVenue().getId());
        dto.setVenueName(e.getVenue().getName());
        dto.setOwnerName(e.getOwner() != null ? e.getOwner().getName() : null);
        dto.setLiveCourtId(e.getLiveCourtId());
        dto.setLiveCourtName(courtName(e.getLiveCourtId()));
        dto.setDraftCourtId(e.getDraftCourtId());
        dto.setDraftCourtName(courtName(e.getDraftCourtId()));
        dto.setReason(e.getReason());
        dto.setStatus(CourtChangeRequest.StatusEnum.fromValue(e.getStatus().name()));
        dto.setDecisionNote(e.getDecisionNote());
        dto.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().atOffset(IST_OFFSET) : null);
        dto.setDecidedAt(e.getDecidedAt() != null ? e.getDecidedAt().atOffset(IST_OFFSET) : null);
        return dto;
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
        // Pre-go-live (no subscription yet) venues build courts freely — the submit-for-approval
        // gate sizes the tier by court count. Enforce the plan's maxCourts only once a subscription
        // (trial or paid) exists.
        Optional<SubscriptionEntity> subOpt = currentEntity(venue);
        if (subOpt.isEmpty()) return;
        SubscriptionEntity sub = subOpt.get();
        int current = (int) courtRepository.countByVenue(venue);
        if (current >= sub.getMaxCourts()) {
            throw new CourtLimitExceededException(
                    String.format("Your %s plan allows %d courts. Upgrade to add more.",
                            sub.getPlanName(), sub.getMaxCourts()),
                    sub.getPlanName(), sub.getMaxCourts(), current);
        }
    }

    @Override
    @Transactional
    public void recomputeVenueLiveFlag(Long venueId) {
        venueRepository.findById(venueId).ifPresent(this::recomputeVenueLive);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> bookableCourtIds(Long venueId) {
        VenueEntity venue = venueRepository.findById(venueId).orElse(null);
        if (venue == null || venue.getStatus() != VenueEntity.VenueStatus.LIVE) {
            return List.of();
        }
        return liveSubscription(venue).map(sub -> {
            Set<String> covered = new HashSet<>(sub.getCoveredCourtIds());
            if (covered.isEmpty()) return List.<Long>of();
            return courtRepository.findByVenue(venue).stream()
                    .filter(CourtEntity::isActive)
                    .map(CourtEntity::getId)
                    .filter(id -> covered.contains(String.valueOf(id)))
                    .toList();
        }).orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourtBookable(Long venueId, Long courtId) {
        return courtId != null && bookableCourtIds(venueId).contains(courtId);
    }

    @Override
    @Transactional
    public void startTrialForApprovedVenue(Long venueId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        // Idempotent: if a current subscription already exists (e.g. re-approval after a send-back
        // round-trip, or a migration comp trial), just refresh the live flag and return.
        if (currentEntity(venue).isPresent()) {
            recomputeVenueLive(venue);
            return;
        }
        PlanCode tier = venue.getIntendedPlanCode() != null ? venue.getIntendedPlanCode() : PlanCode.STARTER;
        SubscriptionPlanEntity plan = planRepository.findByCode(tier)
                .or(() -> planRepository.findByCode(PlanCode.STARTER))
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "code", tier));

        SubscriptionEntity sub = SubscriptionEntity.builder()
                .owner(venue.getOwner())
                .venue(venue)
                .plan(plan)
                .billingCycle(BillingCycle.MONTHLY)
                .currency(plan.getCurrency())
                .activationSource(ActivationSource.ADMIN_MANUAL)
                .build();
        applySnapshotAndActivate(sub, plan, BillingCycle.MONTHLY, true); // trial
        subscriptionRepository.save(sub);
        recomputeVenueLive(venue);
        recordLifecycle(venue, VenueLifecycleEventEntity.Type.TRIAL_ACTIVATED, plan.getName());
        notifyOwner(venue.getOwner(), venue, String.format(
                "Your venue '%s' is approved and live on a %d-day free trial of %s. "
                        + "Activate a paid plan before the trial ends to stay live.",
                venue.getName(), plan.getTrialDays(), plan.getName()));
        log.info("Auto-started {}-day trial (plan {}) for approved venue {}",
                plan.getTrialDays(), plan.getCode(), venueId);
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
        // Every (re)activation/edit/renew gives the subscription a fresh period, so re-arm the
        // expiry reminders (7/3/1) — the notification job will start escalating again from scratch.
        sub.setExpiryNotifiedThreshold(null);
    }

    /** Recompute the venue's denormalized live flag + placement weight + bookable-court count. */
    private void recomputeVenueLive(VenueEntity venue) {
        Optional<SubscriptionEntity> live = liveSubscription(venue);
        boolean active = live.isPresent();
        venue.setSubscriptionActive(active);
        int weight = 0;
        boolean featured = false;
        int bookable = 0;
        if (active) {
            SubscriptionEntity sub = live.get();
            List<FeatureCode> features = sub.getFeatureCodes();
            if (features.contains(FeatureCode.PRIORITY_PLACEMENT)) {
                weight = sub.getPlan().getPlacementWeight();
            }
            featured = features.contains(FeatureCode.FEATURED_BADGE);
            bookable = countBookableCourts(venue, sub);
        }
        venue.setPlacementWeight(weight);
        venue.setFeatured(featured);
        venue.setBookableCourtCount(bookable);
        venueRepository.save(venue);
    }

    /** Active courts of the venue whose id is covered by the given subscription. */
    private int countBookableCourts(VenueEntity venue, SubscriptionEntity sub) {
        Set<String> covered = new HashSet<>(sub.getCoveredCourtIds());
        if (covered.isEmpty()) return 0;
        return (int) courtRepository.findByVenue(venue).stream()
                .filter(CourtEntity::isActive)
                .filter(c -> covered.contains(String.valueOf(c.getId())))
                .count();
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

    private OffsetDateTime odt(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(IST_OFFSET);
    }

    /** Build one admin-table row for a venue from its current subscription + pending request. */
    private VenueSubscriptionRow buildRow(VenueEntity venue) {
        UserEntity owner = venue.getOwner();
        SubscriptionEntity current = currentEntity(venue).orElse(null);
        VenueSubscriptionRow row = new VenueSubscriptionRow()
                .venueId(venue.getId())
                .venueName(venue.getName())
                .venueCity(venue.getCity())
                .ownerName(owner != null ? owner.getName() : null)
                .ownerMobile(owner != null ? owner.getPhone() : null)
                .courtsUsed((int) courtRepository.countByVenue(venue));

        if (current != null) {
            LocalDateTime end = current.getStatus() == SubscriptionStatus.TRIALING && current.getTrialEnd() != null
                    ? current.getTrialEnd() : current.getPeriodEnd();
            row.currentPlanCode(current.getPlanCode() != null ? current.getPlanCode().name() : null)
               .currentPlanName(current.getPlanName())
               .endDate(odt(end))
               .courtLimit(current.getMaxCourts())
               .currentStatus(rollupStatus(current, end));
        } else {
            row.currentStatus(subscriptionRepository.existsByVenue(venue) ? "EXPIRED" : "NONE");
        }

        changeRequestRepository
                .findFirstByVenueAndStatusOrderByCreatedAtDesc(venue, SubscriptionChangeRequestEntity.Status.PENDING)
                .ifPresent(req -> row
                        .pendingRequestId(req.getId())
                        .pendingCurrentPlanName(current != null ? current.getPlanName() : null)
                        .pendingRequestedPlanName(req.getRequestedPlan().getName()));
        return row;
    }

    /** Row-level status rollup: ACTIVE / TRIAL / EXPIRING / EXPIRED. */
    private String rollupStatus(SubscriptionEntity current, LocalDateTime end) {
        if (current.getStatus() == SubscriptionStatus.TRIALING) return "TRIAL";
        if (current.getStatus() == SubscriptionStatus.PAST_DUE) return "EXPIRING";
        if (end != null) {
            long days = ChronoUnit.DAYS.between(dates.now(), end);
            if (days < 0) return "EXPIRED";
            if (days <= EXPIRING_SOON_DAYS) return "EXPIRING";
        }
        return "ACTIVE";
    }

    private boolean matchesStatusFilter(String rowStatus, String filter) {
        if ("ALL".equals(filter)) return true;
        return filter.equals(rowStatus);
    }

    /** Compute the fixed-milestone lifecycle timeline with exactly one LIVE stage. */
    private SubscriptionTimeline buildTimeline(VenueEntity venue) {
        List<SubscriptionEntity> all = subscriptionRepository.findByVenueOrderByIdDesc(venue);
        // Earliest trial start = the oldest subscription that carried a trial period.
        LocalDateTime trialStart = all.stream()
                .filter(s -> s.getTrialEnd() != null)
                .map(SubscriptionEntity::getPeriodStart)
                .reduce((a, b) -> a.isBefore(b) ? a : b)
                .orElse(null);
        // Paid-subscription start = oldest non-trial ACTIVE/PAST_DUE period.
        LocalDateTime subStart = all.stream()
                .filter(s -> s.getTrialEnd() == null
                        && (s.getStatus() == SubscriptionStatus.ACTIVE
                            || s.getStatus() == SubscriptionStatus.PAST_DUE
                            || s.getStatus() == SubscriptionStatus.CANCELED
                            || s.getStatus() == SubscriptionStatus.EXPIRED))
                .map(SubscriptionEntity::getPeriodStart)
                .reduce((a, b) -> a.isBefore(b) ? a : b)
                .orElse(null);

        SubscriptionEntity current = currentEntity(venue).orElse(null);
        boolean approved = venue.getApprovedAt() != null || venue.getStatus() == VenueEntity.VenueStatus.LIVE;

        // Index into [REGISTERED, APPROVED, TRIAL_ACTIVATED, SUBSCRIPTION] that is currently LIVE.
        int liveIdx;
        if (current != null && current.getStatus() == SubscriptionStatus.ACTIVE) liveIdx = 3;
        else if (current != null && current.getStatus() == SubscriptionStatus.PAST_DUE) liveIdx = 3;
        else if (current != null && current.getStatus() == SubscriptionStatus.TRIALING) liveIdx = 2;
        else if (approved) liveIdx = 1;
        else liveIdx = 0;

        StageKey[] keys = { StageKey.REGISTERED, StageKey.APPROVED, StageKey.TRIAL_ACTIVATED, StageKey.SUBSCRIPTION };
        String[] labels = { "Registered", "Approved", "Trial activated", "Subscription" };
        OffsetDateTime[] occurred = {
                odt(venue.getCreatedAt()),
                odt(venue.getApprovedAt()),
                odt(trialStart),
                odt(subStart != null ? subStart : (current != null && current.getStatus() == SubscriptionStatus.ACTIVE
                        ? current.getPeriodStart() : null)),
        };

        List<TimelineStage> stages = new ArrayList<>(4);
        for (int i = 0; i < keys.length; i++) {
            StageState state = i < liveIdx ? StageState.COMPLETED : (i == liveIdx ? StageState.LIVE : StageState.PENDING);
            stages.add(new TimelineStage().key(keys[i]).label(labels[i]).occurredAt(occurred[i]).state(state));
        }
        return new SubscriptionTimeline().stages(stages).liveStageKey(keys[liveIdx]);
    }

    /** Append a lifecycle audit event for the venue (best-effort; never blocks the main flow). */
    private void recordLifecycle(VenueEntity venue, VenueLifecycleEventEntity.Type type, String meta) {
        try {
            lifecycleEventRepository.save(VenueLifecycleEventEntity.builder()
                    .venue(venue)
                    .type(type)
                    .occurredAt(dates.now())
                    .meta(meta)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record lifecycle event {} for venue {}: {}", type, venue.getId(), e.getMessage());
        }
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

    /**
     * Default court coverage = the venue's active courts (capped at {@code maxCourts}), oldest id
     * first. Used when an admin manually creates/edits a subscription with no explicit court pick.
     */
    private List<String> defaultCoverage(VenueEntity venue, int maxCourts) {
        return courtRepository.findByVenue(venue).stream()
                .filter(CourtEntity::isActive)
                .sorted(Comparator.comparing(CourtEntity::getId))
                .limit(Math.max(0, maxCourts))
                .map(c -> String.valueOf(c.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    /** True when the venue has ever held a subscription that carried a trial period (once-per-venue). */
    private boolean trialUsed(VenueEntity venue) {
        return subscriptionRepository.findByVenueOrderByIdDesc(venue).stream()
                .anyMatch(s -> s.getTrialEnd() != null);
    }

    /**
     * Validate a court-coverage selection: non-empty, within the limit, all belonging to the venue.
     * Returns the de-duplicated ids (order preserved). Throws a typed 409 on each failure mode.
     */
    private List<String> validateSelection(List<String> courtIds, List<CourtEntity> venueCourts, int limit) {
        if (courtIds == null || courtIds.isEmpty()) {
            throw new SubscriptionEligibilityException("NO_COURTS_SELECTED", "Select at least one court.");
        }
        List<String> ids = new ArrayList<>(new LinkedHashSet<>(courtIds));
        if (ids.size() > limit) {
            throw new SubscriptionEligibilityException("TOO_MANY_COURTS",
                    "You can cover at most " + limit + " court(s) on this plan.");
        }
        Set<String> venueCourtIds = new HashSet<>();
        for (CourtEntity c : venueCourts) venueCourtIds.add(String.valueOf(c.getId()));
        for (String id : ids) {
            if (!venueCourtIds.contains(id)) {
                throw new SubscriptionEligibilityException("COURT_NOT_IN_VENUE",
                        "A selected court does not belong to this venue.");
            }
        }
        return ids;
    }

    /** Build the owner court-coverage subscription state + purchase eligibility for a venue. */
    private VenueSubscriptionState buildState(VenueEntity venue) {
        SubscriptionEntity current = currentEntity(venue).orElse(null);
        List<CourtEntity> courtList = courtRepository.findByVenue(venue);
        Map<String, String> nameById = new HashMap<>();
        for (CourtEntity c : courtList) nameById.put(String.valueOf(c.getId()), c.getName());
        int totalCourts = courtList.size();
        boolean trialUsed = trialUsed(venue);
        SubscriptionChangeRequestEntity pending = changeRequestRepository
                .findFirstByVenueAndStatusOrderByCreatedAtDesc(venue, SubscriptionChangeRequestEntity.Status.PENDING)
                .orElse(null);

        List<String> currentCovered = current != null
                ? new ArrayList<>(current.getCoveredCourtIds()) : new ArrayList<>();
        VenueSubscriptionState s = new VenueSubscriptionState()
                .venueId(String.valueOf(venue.getId()))
                .totalCourts(totalCourts)
                .bookableCourts(venue.getBookableCourtCount())
                .trialUsed(trialUsed)
                .coveredCourtIds(currentCovered)
                .coveredCourtNames(namesFor(nameById, currentCovered))
                .blockReason(totalCourts == 0
                        ? VenueSubscriptionState.BlockReasonEnum.NO_COURTS
                        : VenueSubscriptionState.BlockReasonEnum.NONE)
                .canStartTrial(totalCourts > 0 && current == null && !trialUsed)
                .canPurchasePaid(totalCourts > 0 && pending == null);

        if (current == null) {
            s.setStatus(subscriptionRepository.existsByVenue(venue)
                    ? VenueSubscriptionState.StatusEnum.EXPIRED
                    : VenueSubscriptionState.StatusEnum.NONE);
        } else {
            boolean isTrial = current.getStatus() == SubscriptionStatus.TRIALING;
            s.setKind(isTrial ? VenueSubscriptionState.KindEnum.TRIAL : VenueSubscriptionState.KindEnum.PAID);
            s.setPlanCode(current.getPlanCode() != null ? current.getPlanCode().name() : null);
            s.setPlanName(current.getPlanName());
            s.setStatus(mapStateStatus(current.getStatus()));
            s.setCourtLimit(current.getMaxCourts());
            s.setStartDate(current.getPeriodStart() != null ? current.getPeriodStart().toLocalDate() : null);
            LocalDateTime end = isTrial && current.getTrialEnd() != null ? current.getTrialEnd() : current.getPeriodEnd();
            s.setEndDate(end != null ? end.toLocalDate() : null);
            s.setUpdatedAt(odt(current.getUpdatedAt()));
        }

        if (pending != null) {
            List<String> pendingCovered = new ArrayList<>(pending.getCoveredCourtIds());
            s.setPendingRequest(new PendingRequestRef()
                    .requestId(String.valueOf(pending.getId()))
                    .planCode(pending.getRequestedPlan().getCode().name())
                    .planName(pending.getRequestedPlan().getName())
                    .coveredCourtIds(pendingCovered)
                    .coveredCourtNames(namesFor(nameById, pendingCovered))
                    .requestedAt(odt(pending.getCreatedAt())));
        }
        return s;
    }

    /** Map a list of court ids to readable names (falls back to "Court {id}" for stale ids). */
    private List<String> namesFor(Map<String, String> nameById, List<String> ids) {
        List<String> names = new ArrayList<>(ids.size());
        for (String id : ids) names.add(nameById.getOrDefault(id, "Court " + id));
        return names;
    }

    private VenueSubscriptionState.StatusEnum mapStateStatus(SubscriptionStatus status) {
        return switch (status) {
            case TRIALING -> VenueSubscriptionState.StatusEnum.TRIAL;
            case ACTIVE, PAST_DUE -> VenueSubscriptionState.StatusEnum.ACTIVE;
            case EXPIRED -> VenueSubscriptionState.StatusEnum.EXPIRED;
            case CANCELED, VOIDED -> VenueSubscriptionState.StatusEnum.CANCELED;
        };
    }

    private SubscriptionRequestView buildRequestView(SubscriptionChangeRequestEntity entity) {
        return new SubscriptionRequestView()
                .requestId(String.valueOf(entity.getId()))
                .venueId(String.valueOf(entity.getVenue().getId()))
                .planCode(entity.getRequestedPlan().getCode().name())
                .coveredCourtIds(new ArrayList<>(entity.getCoveredCourtIds()))
                .status(SubscriptionRequestView.StatusEnum.fromValue(entity.getStatus().name()))
                .requestedAt(odt(entity.getCreatedAt()));
    }

    private void notifyOwner(UserEntity owner, VenueEntity venue, String message) {
        try {
            notificationService.createNotification(owner, "Subscription update", message,
                    NotificationEntity.NotificationType.SYSTEM);
        } catch (Exception e) {
            log.warn("Failed to send subscription notification for venue {}: {}", venue.getId(), e.getMessage());
        }
    }

    /**
     * If a coverage change locked some previously-live courts (e.g. an admin downgrade to a smaller
     * plan), tell the owner exactly which courts were locked and why. Courts are never deleted and
     * existing bookings are honored; the owner can re-pick their live set or upgrade.
     */
    private void notifyIfCourtsLocked(VenueEntity venue, List<String> before, List<String> after, String planName) {
        Set<String> stillLive = new HashSet<>(after);
        List<String> locked = before.stream().filter(id -> !stillLive.contains(id)).toList();
        if (locked.isEmpty()) return;
        Map<String, String> nameById = new HashMap<>();
        for (CourtEntity c : courtRepository.findByVenue(venue)) nameById.put(String.valueOf(c.getId()), c.getName());
        String names = String.join(", ", namesFor(nameById, locked));
        notifyOwner(venue.getOwner(), venue, String.format(
                "Your %s plan covers fewer courts, so these are now locked (not bookable by players): %s. "
                        + "Existing bookings are unaffected. Make another court live or upgrade to re-enable them.",
                planName, names));
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
