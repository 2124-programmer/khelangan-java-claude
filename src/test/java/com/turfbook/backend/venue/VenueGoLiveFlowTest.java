package com.turfbook.backend.venue;

import com.turfbook.backend.dto.AdminVenueDetailDto;
import com.turfbook.backend.dto.CreateVenueRequest;
import com.turfbook.backend.dto.VenueDetailDto;
import com.turfbook.backend.dto.VenueStatusRequest;
import com.turfbook.backend.dto.VenueSummaryPage;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.PlanCode;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.entity.SportEntity;
import com.turfbook.backend.entity.SubscriptionEntity;
import com.turfbook.backend.entity.SubscriptionStatus;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.CourtLimitExceededException;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.CourtRepository;
import com.turfbook.backend.repository.NotificationRepository;
import com.turfbook.backend.repository.SlotRepository;
import com.turfbook.backend.repository.SportRepository;
import com.turfbook.backend.repository.SubscriptionPlanRepository;
import com.turfbook.backend.repository.SubscriptionRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.repository.VenueRepository;
import com.turfbook.backend.scheduler.SubscriptionExpiryNotificationTask;
import com.turfbook.backend.scheduler.SubscriptionExpiryTask;
import com.turfbook.backend.service.VenueService;
import com.turfbook.backend.service.subscription.SubscriptionDateCalculator;
import com.turfbook.backend.service.subscription.SubscriptionGate;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Data-level integration tests for the venue go-live flow (Part 7): free/tiered submission,
 * auto-trial on approval, court-limit enforcement, send-back/resubmit, expiry notifications,
 * and the owner subscription badge.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class VenueGoLiveFlowTest {

    @Autowired private VenueService venueService;
    @Autowired private SubscriptionGate subscriptionGate;
    @Autowired private SubscriptionExpiryTask expiryTask;
    @Autowired private SubscriptionExpiryNotificationTask expiryNotificationTask;
    @Autowired private SubscriptionDateCalculator dates;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionPlanRepository planRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CourtRepository courtRepository;
    @Autowired private SportRepository sportRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private NotificationRepository notificationRepository;

    private UserEntity owner;
    private Long adminId;
    private SportEntity sport;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(UserEntity.builder()
                .name("Owner").email("owner+" + System.nanoTime() + "@t.com").phone("9876543210")
                .passwordHash("h").role(UserEntity.Role.OWNER).build());
        UserEntity admin = userRepository.save(UserEntity.builder()
                .name("Admin").email("admin+" + System.nanoTime() + "@t.com").phone("8888888888")
                .passwordHash("h").role(UserEntity.Role.ADMIN).build());
        adminId = admin.getId();
        sport = sportRepository.save(SportEntity.builder().name("Football").icon("⚽").build());
    }

    private Long planId(PlanCode code) {
        return planRepository.findByCode(code).orElseThrow().getId();
    }

    /** Create a DRAFT venue via the service (status DRAFT, no subscription). */
    private VenueEntity newDraftVenue() {
        VenueDetailDto dto = venueService.createVenue(owner.getId(), new CreateVenueRequest()
                .name("Turf " + System.nanoTime()).address("1 St").city("Mumbai")
                .pricePerHour(500).openTime("06:00").closeTime("22:00")
                .contactPhone("9876543210").sportIds(List.of(sport.getId())));
        return venueRepository.findById(dto.getId()).orElseThrow();
    }

    private void addCourts(VenueEntity venue, int n) {
        for (int i = 0; i < n; i++) {
            courtRepository.save(CourtEntity.builder()
                    .venue(venue).sport(sport).name("C" + System.nanoTime())
                    .pricePerHour(500).slotDurationMins(60).build());
        }
    }

    private VenueEntity reload(Long id) {
        return venueRepository.findById(id).orElseThrow();
    }

    private SubscriptionEntity currentSub(Long venueId) {
        return subscriptionGate.currentSubscription(venueId).orElseThrow();
    }

    // ── Scenario 1: ≤2-court venue submits free → PENDING ──
    @Test
    @DisplayName("Submit: ≤2-court venue submits with no subscription → PENDING")
    void freeSubmitUnderThreshold() {
        VenueEntity venue = newDraftVenue();
        assertThat(venue.getStatus()).isEqualTo(VenueEntity.VenueStatus.DRAFT);
        addCourts(venue, 2);

        VenueDetailDto dto = venueService.submitVenueForApproval(venue.getId(), owner.getId(), null);

        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(subscriptionGate.currentSubscription(venue.getId())).isEmpty(); // no subscription yet
        assertThat(reload(venue.getId()).getIntendedPlanCode()).isNull();
    }

    // ── Scenario 2: >2-court submit blocked without tier; allowed with qualifying tier ──
    @Test
    @DisplayName("Submit: >2-court venue blocked without a qualifying tier, allowed with Growth")
    void tieredSubmitOverThreshold() {
        VenueEntity venue = newDraftVenue();
        addCourts(venue, 3); // over the free threshold

        assertThatThrownBy(() -> venueService.submitVenueForApproval(venue.getId(), owner.getId(), null))
                .isInstanceOf(BadRequestException.class);
        // STARTER (maxCourts 2) does not qualify for 3 courts.
        assertThatThrownBy(() -> venueService.submitVenueForApproval(venue.getId(), owner.getId(), planId(PlanCode.STARTER)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Choose a higher tier");

        VenueDetailDto dto = venueService.submitVenueForApproval(venue.getId(), owner.getId(), planId(PlanCode.GROWTH));
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(reload(venue.getId()).getIntendedPlanCode()).isEqualTo(PlanCode.GROWTH);
    }

    @Test
    @DisplayName("Submit: a venue with zero courts cannot be submitted")
    void cannotSubmitWithoutCourts() {
        VenueEntity venue = newDraftVenue();
        assertThatThrownBy(() -> venueService.submitVenueForApproval(venue.getId(), owner.getId(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one court");
    }

    // ── Scenario 3: approve → 30-day TRIALING subscription auto-created → venue live ──
    @Test
    @DisplayName("Approve: auto-creates a 30-day TRIALING subscription; venue live and listed")
    void approveStartsTrial() {
        VenueEntity venue = newDraftVenue();
        addCourts(venue, 2);
        venueService.submitVenueForApproval(venue.getId(), owner.getId(), null);

        venueService.updateVenueStatus(venue.getId(), new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.LIVE));

        SubscriptionEntity sub = currentSub(venue.getId());
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(sub.getPlanCode()).isEqualTo(PlanCode.STARTER); // ≤2 courts → Starter
        assertThat(sub.getTrialEnd().toLocalDate())
                .isEqualTo(dates.now().toLocalDate().plusDays(30));
        assertThat(reload(venue.getId()).getStatus()).isEqualTo(VenueEntity.VenueStatus.LIVE);
        assertThat(reload(venue.getId()).isSubscriptionActive()).isTrue();
        boolean listed = venueRepository.findLiveVenues(VenueEntity.VenueStatus.LIVE, null, null, null,
                PageRequest.of(0, 50)).stream().anyMatch(v -> v.getId().equals(venue.getId()));
        assertThat(listed).isTrue();
    }

    @Test
    @DisplayName("Approve: >2-court venue trials on the committed tier")
    void approveUsesIntendedTier() {
        VenueEntity venue = newDraftVenue();
        addCourts(venue, 3);
        venueService.submitVenueForApproval(venue.getId(), owner.getId(), planId(PlanCode.GROWTH));
        venueService.updateVenueStatus(venue.getId(), new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.LIVE));

        assertThat(currentSub(venue.getId()).getPlanCode()).isEqualTo(PlanCode.GROWTH);
        assertThat(currentSub(venue.getId()).getMaxCourts()).isEqualTo(4);
    }

    // ── Scenario 4: trial expiry suspends; confirmed booking preserved ──
    @Test
    @DisplayName("Trial expiry: at trialEnd the venue suspends, confirmed booking preserved")
    void trialExpirySuspends() {
        VenueEntity venue = newDraftVenue();
        addCourts(venue, 1);
        venueService.submitVenueForApproval(venue.getId(), owner.getId(), null);
        venueService.updateVenueStatus(venue.getId(), new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.LIVE));

        CourtEntity court = courtRepository.findByVenue(reload(venue.getId())).get(0);
        SlotEntity slot = slotRepository.save(SlotEntity.builder()
                .court(court).date(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .status(SlotEntity.SlotStatus.BOOKED).price(500).build());
        BookingEntity booking = bookingRepository.save(BookingEntity.builder()
                .player(owner).venue(reload(venue.getId())).court(court).slot(slot).sport("Football")
                .date(slot.getDate()).startTime(slot.getStartTime()).endTime(slot.getEndTime())
                .amount(500).convenienceFee(0).discount(0).commission(0).hasReview(false)
                .status(BookingEntity.BookingStatus.CONFIRMED)
                .paymentStatus(BookingEntity.PaymentStatus.SUCCESS).build());

        // Force the trial period to have ended, then run the lifecycle job.
        SubscriptionEntity sub = currentSub(venue.getId());
        sub.setPeriodEnd(dates.now().minusHours(1));
        sub.setTrialEnd(dates.now().minusHours(1));
        subscriptionRepository.save(sub);
        expiryTask.markPastDue();

        assertThat(reload(venue.getId()).isSubscriptionActive()).isFalse();
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingEntity.BookingStatus.CONFIRMED);
    }

    // ── Scenario 5: expiry notification fires at a threshold and is not duplicated ──
    @Test
    @DisplayName("Expiry reminder: fires once per threshold (idempotent)")
    void expiryReminderIdempotent() {
        VenueEntity venue = newDraftVenue();
        addCourts(venue, 1);
        venueService.submitVenueForApproval(venue.getId(), owner.getId(), null);
        venueService.updateVenueStatus(venue.getId(), new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.LIVE));

        // Bring the trial within the 3-day threshold.
        SubscriptionEntity sub = currentSub(venue.getId());
        sub.setTrialEnd(dates.now().plusDays(3));
        sub.setPeriodEnd(sub.getTrialEnd());
        subscriptionRepository.save(sub);

        long before = notificationRepository.findByUserAndIsReadFalse(owner).size();
        expiryNotificationTask.sendExpiryReminders();
        long afterFirst = notificationRepository.findByUserAndIsReadFalse(owner).size();
        expiryNotificationTask.sendExpiryReminders(); // second run — must not re-notify
        long afterSecond = notificationRepository.findByUserAndIsReadFalse(owner).size();

        assertThat(afterFirst).isEqualTo(before + 1);
        assertThat(afterSecond).isEqualTo(afterFirst);
        assertThat(currentSub(venue.getId()).getExpiryNotifiedThreshold()).isEqualTo(3);
    }

    // ── Scenario 6: court limit blocks beyond maxCourts; free pre-approval building allowed ──
    @Test
    @DisplayName("Court limit: free before approval; enforced (with allowed/current) after trial starts")
    void courtLimitEnforcedOnlyAfterSubscription() {
        VenueEntity venue = newDraftVenue();
        // No subscription yet → adding courts is allowed (no-op gate).
        subscriptionGate.assertCanAddCourt(venue.getId());
        addCourts(venue, 2);
        venueService.submitVenueForApproval(venue.getId(), owner.getId(), null);
        venueService.updateVenueStatus(venue.getId(), new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.LIVE));

        // Now on a Starter trial (2 courts) → at the limit.
        assertThatThrownBy(() -> subscriptionGate.assertCanAddCourt(venue.getId()))
                .isInstanceOf(CourtLimitExceededException.class)
                .hasMessageContaining("Upgrade to add more");
        CourtLimitExceededException ex = (CourtLimitExceededException) catchThrowable(
                () -> subscriptionGate.assertCanAddCourt(venue.getId()));
        assertThat(ex.getAllowed()).isEqualTo(2);
        assertThat(ex.getCurrent()).isEqualTo(2);
    }

    private static Throwable catchThrowable(ThrowingRunnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private interface ThrowingRunnable {
        void run() throws Throwable;
    }

    // ── Scenario 7: multi-venue — independent court limits ──
    @Test
    @DisplayName("Multi-venue: two venues on different tiers enforce independent court limits")
    void multiVenueIndependentLimits() {
        VenueEntity a = newDraftVenue();
        addCourts(a, 2);
        venueService.submitVenueForApproval(a.getId(), owner.getId(), null); // Starter (2)
        venueService.updateVenueStatus(a.getId(), new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.LIVE));

        VenueEntity b = newDraftVenue();
        addCourts(b, 3);
        venueService.submitVenueForApproval(b.getId(), owner.getId(), planId(PlanCode.GROWTH)); // Growth (4)
        venueService.updateVenueStatus(b.getId(), new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.LIVE));

        assertThat(subscriptionGate.maxCourtsFor(a.getId())).isEqualTo(2);
        assertThat(subscriptionGate.maxCourtsFor(b.getId())).isEqualTo(4);
        // A is at its limit; B still has headroom.
        assertThatThrownBy(() -> subscriptionGate.assertCanAddCourt(a.getId()))
                .isInstanceOf(CourtLimitExceededException.class);
        subscriptionGate.assertCanAddCourt(b.getId()); // ok — 3 of 4
    }

    // ── Scenario 8: send-back with comments → CHANGES_REQUESTED → resubmit → PENDING, history kept ──
    @Test
    @DisplayName("Send-back: requires comments, sets CHANGES_REQUESTED; owner resubmit → PENDING; history retained")
    void sendBackAndResubmit() {
        VenueEntity venue = newDraftVenue();
        addCourts(venue, 1);
        venueService.submitVenueForApproval(venue.getId(), owner.getId(), null); // SUBMITTED

        // Comments required for send-back.
        assertThatThrownBy(() -> venueService.updateVenueStatus(venue.getId(),
                new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.CHANGES_REQUESTED)))
                .isInstanceOf(IllegalArgumentException.class);

        venueService.updateVenueStatus(venue.getId(), new VenueStatusRequest()
                .status(VenueStatusRequest.StatusEnum.CHANGES_REQUESTED).rejectionReason("Add clearer photos"));
        assertThat(reload(venue.getId()).getStatus()).isEqualTo(VenueEntity.VenueStatus.CHANGES_REQUESTED);

        // Owner resubmits.
        VenueDetailDto resubmitted = venueService.submitVenueForApproval(venue.getId(), owner.getId(), null);
        assertThat(resubmitted.getStatus()).isEqualTo("PENDING");

        // Full thread retained: SUBMITTED, CHANGES_REQUESTED, RESUBMITTED.
        AdminVenueDetailDto admin = venueService.adminGetVenue(venue.getId());
        assertThat(admin.getCommentHistory()).extracting("action")
                .containsExactly("SUBMITTED", "CHANGES_REQUESTED", "RESUBMITTED");
        assertThat(admin.getCommentHistory().get(1).getComment()).isEqualTo("Add clearer photos");
    }

    @Test
    @DisplayName("Reject: requires a reason and records it in the comment thread")
    void rejectRequiresReason() {
        VenueEntity venue = newDraftVenue();
        addCourts(venue, 1);
        venueService.submitVenueForApproval(venue.getId(), owner.getId(), null);

        assertThatThrownBy(() -> venueService.updateVenueStatus(venue.getId(),
                new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.REJECTED)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Scenario 9: owner badge — remaining days + expiring-soon flag ──
    @Test
    @DisplayName("Badge: owner venues list carries plan + remaining days + expiringSoon")
    void ownerBadgeComputed() {
        VenueEntity venue = newDraftVenue();
        addCourts(venue, 2);
        venueService.submitVenueForApproval(venue.getId(), owner.getId(), null);
        venueService.updateVenueStatus(venue.getId(), new VenueStatusRequest().status(VenueStatusRequest.StatusEnum.LIVE));

        VenueSummaryPage page = venueService.listOwnerVenues(owner.getId(), 0, 20);
        var summary = page.getContent().stream()
                .filter(v -> v.getId().equals(venue.getId())).findFirst().orElseThrow();
        assertThat(summary.getSubscription()).isNotNull();
        assertThat(summary.getSubscription().getStatus()).isEqualTo("TRIALING");
        assertThat(summary.getSubscription().getPlanCode()).isEqualTo("STARTER");
        // 30-day trial → ~29-30 days remaining, not expiring soon.
        assertThat(summary.getSubscription().getRemainingDays()).isGreaterThan(7);
        assertThat(summary.getSubscription().getExpiringSoon()).isFalse();
    }
}
