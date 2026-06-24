package com.turfbook.backend.subscription;

import com.turfbook.backend.dto.Subscription;
import com.turfbook.backend.dto.SubscriptionChangeRequest;
import com.turfbook.backend.dto.SubscriptionCreateRequest;
import com.turfbook.backend.dto.SubscriptionEditRequest;
import com.turfbook.backend.dto.UpgradeRequestCreate;
import com.turfbook.backend.dto.VenueSubscriptionView;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.FeatureCode;
import com.turfbook.backend.entity.PlanCode;
import com.turfbook.backend.entity.SportEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionPlanEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.SportRepository;
import com.turfbook.backend.repository.SubscriptionPlanRepository;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.scheduler.SubscriptionExpiryTask;
import com.turfbook.backend.service.subscription.SubscriptionDateCalculator;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import com.turfbook.backend.service.subscription.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Data-level integration tests for the venue subscription system. Plans are seeded by the
 * {@code SubscriptionPlanSeeder} ApplicationRunner at context start, so they exist here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class SubscriptionServiceTest {

    @Autowired private SubscriptionService subscriptionService;
    @Autowired private SubscriptionGate subscriptionGate;
    @Autowired private SubscriptionExpiryTask expiryTask;
    @Autowired private SubscriptionDateCalculator dates;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionPlanRepository planRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CourtRepository courtRepository;
    @Autowired private SportRepository sportRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private com.turfbook.backend.repository.SlotRepository slotRepository;

    private UserEntity owner;
    private Long adminId;
    private VenueEntity venue;
    private SportEntity sport;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(UserEntity.builder()
                .name("Owner").email("owner+" + System.nanoTime() + "@t.com").phone("9999999999")
                .passwordHash("h").role(UserEntity.Role.OWNER).build());
        UserEntity admin = userRepository.save(UserEntity.builder()
                .name("Admin").email("admin+" + System.nanoTime() + "@t.com").phone("8888888888")
                .passwordHash("h").role(UserEntity.Role.ADMIN).build());
        adminId = admin.getId();
        sport = sportRepository.save(SportEntity.builder().name("Football").icon("⚽").build());
        venue = venueRepository.save(VenueEntity.builder()
                .owner(owner).name("Test Turf " + System.nanoTime()).address("1 St").city("Mumbai")
                .status(VenueEntity.VenueStatus.LIVE).pricePerHour(500).build());
    }

    private Long planId(PlanCode code) {
        return planRepository.findByCode(code).orElseThrow().getId();
    }

    private SubscriptionCreateRequest createReq(PlanCode code,
                                                SubscriptionCreateRequest.BillingCycleEnum cycle, boolean trial) {
        return new SubscriptionCreateRequest()
                .ownerId(owner.getId()).venueId(venue.getId()).planId(planId(code))
                .billingCycle(cycle).asTrial(trial)
                .paymentMethod(SubscriptionCreateRequest.PaymentMethodEnum.CASH);
    }

    private CourtEntity addCourt(String name) {
        return courtRepository.save(CourtEntity.builder()
                .venue(venue).sport(sport).name(name).pricePerHour(500).slotDurationMins(60).build());
    }

    private VenueEntity reloadVenue() {
        return venueRepository.findById(venue.getId()).orElseThrow();
    }

    private com.turfbook.backend.entity.SlotEntity bookingSlot(CourtEntity court) {
        return slotRepository.save(com.turfbook.backend.entity.SlotEntity.builder()
                .court(court).date(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .status(com.turfbook.backend.entity.SlotEntity.SlotStatus.BOOKED).price(500).build());
    }

    // ── Scenario 1: manual activate (paid) → ACTIVE, server dates, venue live & listed ──
    @Test
    @DisplayName("Admin manual-activate (paid): ACTIVE with backend dates; venue live and in player query")
    void manualActivatePaid() {
        Subscription sub = subscriptionService.adminCreateSubscription(
                createReq(PlanCode.GROWTH, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);

        assertThat(sub.getStatus()).isEqualTo("ACTIVE");
        assertThat(sub.getPeriodStart()).isNotNull();
        assertThat(sub.getPeriodEnd()).isNotNull();
        // periodEnd = periodStart + 1 month for MONTHLY
        assertThat(sub.getPeriodEnd().toLocalDateTime())
                .isEqualTo(sub.getPeriodStart().toLocalDateTime().plusMonths(1));
        assertThat(sub.getPrice()).isEqualTo(899);

        assertThat(reloadVenue().isSubscriptionActive()).isTrue();
        boolean listed = venueRepository.findLiveVenues(VenueEntity.VenueStatus.LIVE, null, null, null,
                PageRequest.of(0, 50)).stream().anyMatch(v -> v.getId().equals(venue.getId()));
        assertThat(listed).as("live venue appears in player query").isTrue();
    }

    // ── Scenario 2: activate as trial → TRIALING, trialEnd set, live during trial ──
    @Test
    @DisplayName("Activate asTrial: TRIALING with trialEnd; venue live during trial")
    void activateTrial() {
        Subscription sub = subscriptionService.adminCreateSubscription(
                createReq(PlanCode.STARTER, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, true), adminId);

        assertThat(sub.getStatus()).isEqualTo("TRIALING");
        assertThat(sub.getTrialEnd()).isNotNull();
        assertThat(sub.getPeriodEnd()).isEqualTo(sub.getTrialEnd());
        assertThat(reloadVenue().isSubscriptionActive()).isTrue();
    }

    // ── Scenario 3: edit plan/cycle → snapshots updated, periodEnd recomputed, start preserved ──
    @Test
    @DisplayName("Edit: plan/features/maxCourts updated, periodEnd recomputed, periodStart preserved")
    void editRecomputesPreservingStart() {
        Subscription created = subscriptionService.adminCreateSubscription(
                createReq(PlanCode.GROWTH, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);
        var start = created.getPeriodStart();

        Subscription edited = subscriptionService.adminEditSubscription(created.getId(),
                new SubscriptionEditRequest().planId(planId(PlanCode.PRO))
                        .billingCycle(SubscriptionEditRequest.BillingCycleEnum.ANNUAL));

        assertThat(edited.getPlanCode()).isEqualTo("PRO");
        assertThat(edited.getMaxCourts()).isEqualTo(6);
        assertThat(edited.getFeatures()).contains(FeatureCode.PRIORITY_PLACEMENT.name());
        assertThat(edited.getPrice()).isEqualTo(12990); // PRO annual
        assertThat(edited.getPeriodStart()).isEqualTo(start); // preserved
        assertThat(edited.getPeriodEnd().toLocalDateTime())
                .isEqualTo(start.toLocalDateTime().plusYears(1)); // recomputed for ANNUAL
    }

    // ── Scenario 4: void → venue suspended; record remains in history ──
    @Test
    @DisplayName("Void: venue suspended; VOIDED record remains")
    void voidSuspends() {
        Subscription sub = subscriptionService.adminCreateSubscription(
                createReq(PlanCode.GROWTH, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);
        subscriptionService.adminVoidSubscription(sub.getId());

        SubscriptionEntity reloaded = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.VOIDED);
        assertThat(reloadVenue().isSubscriptionActive()).isFalse();
    }

    // ── Scenario 5a: maxCourts — adding beyond the limit is blocked ──
    @Test
    @DisplayName("maxCourts: adding a court beyond the plan limit is rejected")
    void courtLimitBlocksBeyondMax() {
        subscriptionService.adminCreateSubscription(
                createReq(PlanCode.STARTER, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);
        addCourt("C1");
        addCourt("C2"); // now at the STARTER limit of 2

        assertThatThrownBy(() -> subscriptionGate.assertCanAddCourt(venue.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Upgrade to add more");
    }

    // ── Scenario 5b: downgrade below current court count is rejected ──
    @Test
    @DisplayName("maxCourts: downgrade below current court count is rejected")
    void downgradeBelowCourtCountRejected() {
        Subscription sub = subscriptionService.adminCreateSubscription(
                createReq(PlanCode.PRO, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);
        addCourt("C1");
        addCourt("C2");
        addCourt("C3"); // 3 courts; STARTER allows only 2

        assertThatThrownBy(() -> subscriptionService.adminEditSubscription(sub.getId(),
                new SubscriptionEditRequest().planId(planId(PlanCode.STARTER))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("allows only");
    }

    // ── Scenario 6: feature gating ──
    @Test
    @DisplayName("Feature gating: STARTER lacks AUTO_ACCEPT (403); GROWTH grants it")
    void featureGating() {
        subscriptionService.adminCreateSubscription(
                createReq(PlanCode.STARTER, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);
        assertThat(subscriptionGate.hasFeature(venue.getId(), FeatureCode.AUTO_ACCEPT)).isFalse();
        assertThatThrownBy(() -> subscriptionGate.assertFeature(venue.getId(), FeatureCode.AUTO_ACCEPT, "nope"))
                .isInstanceOf(ForbiddenException.class);
        assertThat(subscriptionGate.ownerHasFeatureOnAnyVenue(owner.getId(), FeatureCode.AUTO_ACCEPT)).isFalse();

        // Upgrade to GROWTH via edit; AUTO_ACCEPT now granted.
        SubscriptionEntity cur = subscriptionRepository
                .findFirstByVenueAndStatusInOrderByIdDesc(venue,
                        java.util.List.of(SubscriptionStatus.ACTIVE)).orElseThrow();
        subscriptionService.adminEditSubscription(cur.getId(),
                new SubscriptionEditRequest().planId(planId(PlanCode.GROWTH)));
        assertThat(subscriptionGate.hasFeature(venue.getId(), FeatureCode.AUTO_ACCEPT)).isTrue();
        assertThat(subscriptionGate.ownerHasFeatureOnAnyVenue(owner.getId(), FeatureCode.AUTO_ACCEPT)).isTrue();
    }

    // ── Scenario 7: upgrade request → admin activate ──
    @Test
    @DisplayName("Upgrade: owner request PENDING → admin activate → new plan applied, old superseded")
    void upgradeFlow() {
        Subscription starter = subscriptionService.adminCreateSubscription(
                createReq(PlanCode.STARTER, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);

        SubscriptionChangeRequest req = subscriptionService.ownerCreateUpgradeRequest(venue.getId(), owner.getId(),
                new UpgradeRequestCreate().requestedPlanId(planId(PlanCode.PRO))
                        .billingCycle(UpgradeRequestCreate.BillingCycleEnum.MONTHLY));
        assertThat(req.getStatus()).isEqualTo("PENDING");

        // null = no admin court override → activate with the owner's requested coverage (legacy behavior)
        Subscription activated = subscriptionService.adminActivateChangeRequest(req.getId(), null, adminId);
        assertThat(activated.getPlanCode()).isEqualTo("PRO");
        assertThat(activated.getStatus()).isEqualTo("ACTIVE");
        assertThat(activated.getPeriodEnd()).isNotNull();

        // Old starter subscription is superseded (CANCELED).
        assertThat(subscriptionRepository.findById(starter.getId()).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.CANCELED);
    }

    // ── Scenario 8: expiry job suspends + honors confirmed bookings; renew restores ──
    @Test
    @DisplayName("Expiry: past periodEnd → PAST_DUE + suspended, confirmed booking preserved; renew restores")
    void expiryThenRenew() {
        Subscription sub = subscriptionService.adminCreateSubscription(
                createReq(PlanCode.GROWTH, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);

        // A confirmed future booking the expiry job must never touch.
        CourtEntity court = addCourt("C1");
        com.turfbook.backend.entity.SlotEntity slot = bookingSlot(court);
        BookingEntity booking = bookingRepository.save(BookingEntity.builder()
                .player(owner).venue(venue).court(court).slot(slot).sport("Football")
                .date(slot.getDate()).startTime(slot.getStartTime()).endTime(slot.getEndTime())
                .amount(500).convenienceFee(0).discount(0).commission(0).hasReview(false)
                .status(BookingEntity.BookingStatus.CONFIRMED)
                .paymentStatus(BookingEntity.PaymentStatus.SUCCESS).build());

        // Force the period to have ended, then run the lifecycle job.
        SubscriptionEntity e = subscriptionRepository.findById(sub.getId()).orElseThrow();
        e.setPeriodEnd(dates.now().minusHours(1));
        subscriptionRepository.save(e);
        expiryTask.markPastDue();

        assertThat(subscriptionRepository.findById(sub.getId()).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(reloadVenue().isSubscriptionActive()).isFalse();
        // Confirmed booking untouched.
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingEntity.BookingStatus.CONFIRMED);

        // Admin renew restores live.
        Subscription renewed = subscriptionService.adminRenewSubscription(sub.getId(), adminId);
        assertThat(renewed.getStatus()).isEqualTo("ACTIVE");
        assertThat(renewed.getPeriodEnd().toLocalDateTime()).isAfter(dates.now());
        assertThat(reloadVenue().isSubscriptionActive()).isTrue();
    }

    // ── Scenario 9: history endpoint ordered with accurate snapshots ──
    @Test
    @DisplayName("History: returns recent subscriptions newest-first with accurate snapshots")
    void historyOrderedWithSnapshots() {
        Subscription starter = subscriptionService.adminCreateSubscription(
                createReq(PlanCode.STARTER, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);
        subscriptionService.adminVoidSubscription(starter.getId());
        subscriptionService.adminCreateSubscription(
                createReq(PlanCode.GROWTH, SubscriptionCreateRequest.BillingCycleEnum.ANNUAL, false), adminId);

        VenueSubscriptionView view = subscriptionService.adminGetVenueSubscription(venue.getId());
        assertThat(view.getCurrent()).isNotNull();
        assertThat(view.getCurrent().getPlanCode()).isEqualTo("GROWTH");
        assertThat(view.getCourtsAllowed()).isEqualTo(4);
        assertThat(view.getHistory()).hasSize(2);
        // newest first
        assertThat(view.getHistory().get(0).getPlanCode()).isEqualTo("GROWTH");
        assertThat(view.getHistory().get(1).getPlanCode()).isEqualTo("STARTER");
    }

    // ── Single-current invariant ──
    @Test
    @DisplayName("Create is rejected when a non-terminal subscription already exists")
    void singleCurrentInvariant() {
        subscriptionService.adminCreateSubscription(
                createReq(PlanCode.STARTER, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId);
        assertThatThrownBy(() -> subscriptionService.adminCreateSubscription(
                createReq(PlanCode.GROWTH, SubscriptionCreateRequest.BillingCycleEnum.MONTHLY, false), adminId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has an active subscription");
    }

    // ── Plan seeding ──
    @Test
    @DisplayName("Seeder: the four plans exist with exact catalog values")
    void plansSeeded() {
        SubscriptionPlanEntity pro = planRepository.findByCode(PlanCode.PRO).orElseThrow();
        assertThat(pro.getMaxCourts()).isEqualTo(6);
        assertThat(pro.getPriceMonthly()).isEqualTo(1299);
        assertThat(pro.getPriceAnnual()).isEqualTo(12990);
        assertThat(pro.getPhotoLimit()).isEqualTo(15);
        assertThat(pro.getPlacementWeight()).isEqualTo(30);
        assertThat(planRepository.findAllByOrderByDisplayOrderAscIdAsc()).hasSize(4);
    }
}
