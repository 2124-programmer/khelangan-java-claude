package com.turfbook.backend.service.subscription;

import com.turfbook.backend.dto.Subscription;
import com.turfbook.backend.dto.SubscriptionChangeRequest;
import com.turfbook.backend.dto.SubscriptionPayment;
import com.turfbook.backend.dto.SubscriptionPlan;
import com.turfbook.backend.dto.VenueSubscriptionView;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.SubscriptionChangeRequestEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionPaymentEntity;
import com.turfbook.backend.entity.SubscriptionPlanEntity;
import com.turfbook.backend.repository.CourtRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-written entity → DTO mapping for subscriptions. Period dates are stored as
 * Asia/Kolkata wall-clock {@link LocalDateTime} and surfaced with the fixed IST offset
 * (+05:30) so clients receive the correct instant. Enum-valued fields map to their names.
 */
@Component
public class SubscriptionMapper {

    /** India Standard Time has no DST, so a fixed offset is exact. */
    private static final ZoneOffset IST_OFFSET = ZoneOffset.of("+05:30");

    private final CourtRepository courtRepository;

    public SubscriptionMapper(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    private OffsetDateTime odt(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(IST_OFFSET);
    }

    public SubscriptionPlan toPlanDto(SubscriptionPlanEntity e) {
        return new SubscriptionPlan()
                .id(e.getId())
                .code(e.getCode().name())
                .name(e.getName())
                .maxCourts(e.getMaxCourts())
                .priceMonthly(e.getPriceMonthly())
                .priceAnnual(e.getPriceAnnual())
                .currency(e.getCurrency())
                .features(new ArrayList<>(e.getFeatures()))
                .photoLimit(e.getPhotoLimit())
                .placementWeight(e.getPlacementWeight())
                .trialDays(e.getTrialDays())
                .active(e.isActive())
                .displayOrder(e.getDisplayOrder());
    }

    public List<SubscriptionPlan> toPlanDtos(List<SubscriptionPlanEntity> list) {
        List<SubscriptionPlan> out = new ArrayList<>(list.size());
        for (SubscriptionPlanEntity e : list) out.add(toPlanDto(e));
        return out;
    }

    public Subscription toDto(SubscriptionEntity e) {
        List<String> covered = e.getCoveredCourtIds() == null ? new ArrayList<>() : new ArrayList<>(e.getCoveredCourtIds());
        return new Subscription()
                .id(e.getId())
                .ownerId(e.getOwner().getId())
                .venueId(e.getVenue().getId())
                .planId(e.getPlan().getId())
                .planCode(e.getPlanCode().name())
                .planName(e.getPlanName())
                .billingCycle(e.getBillingCycle().name())
                .status(e.getStatus().name())
                .periodStart(odt(e.getPeriodStart()))
                .periodEnd(odt(e.getPeriodEnd()))
                .trialEnd(odt(e.getTrialEnd()))
                .price(e.getPrice())
                .currency(e.getCurrency())
                .maxCourts(e.getMaxCourts())
                .coveredCourtIds(covered)
                .coveredCourtNames(resolveCourtNames(e.getVenue(), covered))
                .features(new ArrayList<>(e.getFeatures()))
                .activationSource(e.getActivationSource().name())
                .createdAt(odt(e.getCreatedAt()))
                .updatedAt(odt(e.getUpdatedAt()));
    }

    public List<Subscription> toDtos(List<SubscriptionEntity> list) {
        List<Subscription> out = new ArrayList<>(list.size());
        for (SubscriptionEntity e : list) out.add(toDto(e));
        return out;
    }

    public SubscriptionPayment toPaymentDto(SubscriptionPaymentEntity e) {
        return new SubscriptionPayment()
                .id(e.getId())
                .subscriptionId(e.getSubscription().getId())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .method(e.getMethod().name())
                .status(e.getStatus().name())
                .paidAt(odt(e.getPaidAt()))
                .recordedByAdminId(e.getRecordedByAdmin() != null ? e.getRecordedByAdmin().getId() : null)
                .reference(e.getReference());
    }

    public SubscriptionChangeRequest toChangeRequestDto(SubscriptionChangeRequestEntity e) {
        SubscriptionPlanEntity plan = e.getRequestedPlan();
        boolean annual = e.getRequestedCycle() == com.turfbook.backend.entity.BillingCycle.ANNUAL;
        int requestedPrice = annual ? plan.getPriceAnnual() : plan.getPriceMonthly();
        return new SubscriptionChangeRequest()
                .id(e.getId())
                .ownerId(e.getOwner().getId())
                .ownerName(e.getOwner().getName())
                .ownerEmail(e.getOwner().getEmail())
                .venueId(e.getVenue().getId())
                .venueName(e.getVenue().getName())
                .venueCity(e.getVenue().getCity())
                .currentSubscriptionId(e.getCurrentSubscription() != null ? e.getCurrentSubscription().getId() : null)
                .currentPlanName(e.getCurrentSubscription() != null ? e.getCurrentSubscription().getPlanName() : null)
                .requestedPlanId(plan.getId())
                .requestedPlanCode(plan.getCode().name())
                .requestedPlanName(plan.getName())
                .requestedPlanPrice(requestedPrice)
                .requestedPlanMaxCourts(plan.getMaxCourts())
                .requestedCycle(e.getRequestedCycle().name())
                .status(e.getStatus().name())
                .coveredCourtIds(new ArrayList<>(e.getCoveredCourtIds() == null ? List.of() : e.getCoveredCourtIds()))
                .coveredCourtNames(resolveCourtNames(e))
                .createdAt(odt(e.getCreatedAt()))
                .decidedAt(odt(e.getDecidedAt()))
                .reason(e.getReason());
    }

    private List<String> resolveCourtNames(SubscriptionChangeRequestEntity e) {
        return resolveCourtNames(e.getVenue(), e.getCoveredCourtIds());
    }

    /** Map covered court ids to readable names via the venue's courts (falls back to "Court {id}"). */
    private List<String> resolveCourtNames(com.turfbook.backend.entity.VenueEntity venue, List<String> ids) {
        if (ids == null || ids.isEmpty() || venue == null) return new ArrayList<>();
        Map<String, String> byId = new HashMap<>();
        for (CourtEntity c : courtRepository.findByVenue(venue)) {
            byId.put(String.valueOf(c.getId()), c.getName());
        }
        List<String> names = new ArrayList<>(ids.size());
        for (String id : ids) names.add(byId.getOrDefault(id, "Court " + id));
        return names;
    }

    public List<SubscriptionChangeRequest> toChangeRequestDtos(List<SubscriptionChangeRequestEntity> list) {
        List<SubscriptionChangeRequest> out = new ArrayList<>(list.size());
        for (SubscriptionChangeRequestEntity e : list) out.add(toChangeRequestDto(e));
        return out;
    }

    public VenueSubscriptionView toView(SubscriptionEntity current,
                                        int courtsUsed,
                                        int courtsAllowed,
                                        List<SubscriptionEntity> history,
                                        SubscriptionChangeRequestEntity pending) {
        return new VenueSubscriptionView()
                .current(current != null ? toDto(current) : null)
                .courtsUsed(courtsUsed)
                .courtsAllowed(courtsAllowed)
                .history(toDtos(history))
                .pendingChangeRequest(pending != null ? toChangeRequestDto(pending) : null);
    }
}
