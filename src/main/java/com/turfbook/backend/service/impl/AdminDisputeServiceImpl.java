package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.AdminAuditEntity;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.DisputeEntity;
import com.turfbook.backend.entity.DisputeEventEntity;
import com.turfbook.backend.entity.DisputeMessageEntity;
import com.turfbook.backend.entity.DisputeNoteEntity;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.BadRequestException;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.AdminAuditRepository;
import com.turfbook.backend.repository.DisputeEventRepository;
import com.turfbook.backend.repository.DisputeMessageRepository;
import com.turfbook.backend.repository.DisputeNoteRepository;
import com.turfbook.backend.repository.DisputeRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.AdminDisputeService;
import com.turfbook.backend.service.AdminOwnerService;
import com.turfbook.backend.service.AdminPermissionService;
import com.turfbook.backend.service.AdminPlayerService;
import com.turfbook.backend.service.NotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin disputes. Triage list/detail mirror the Players/Owners siblings (dynamic JPQL, server
 * availableActions, audit). The resolution model is the locked one: NO refunds — only a ruling, an
 * optional informational "ask the owner to refund offline", and an optional account CONSEQUENCE
 * applied via the existing Player/Owner moderation service. At-fault rulings feed the party risk score.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDisputeServiceImpl implements AdminDisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository messageRepository;
    private final DisputeNoteRepository noteRepository;
    private final DisputeEventRepository eventRepository;
    private final UserRepository userRepository;
    private final AdminAuditRepository auditRepository;
    private final NotificationService notificationService;
    private final AdminPlayerService adminPlayerService;
    private final AdminOwnerService adminOwnerService;
    private final AdminPermissionService adminPermissionService;

    @PersistenceContext
    private EntityManager em;

    private static final List<DisputeEntity.DisputeStatus> ACTIVE_STATUSES = List.of(
            DisputeEntity.DisputeStatus.OPEN, DisputeEntity.DisputeStatus.UNDER_REVIEW,
            DisputeEntity.DisputeStatus.NEEDS_INFO);

    // ─── List ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminDisputePage list(String q, List<DisputeStatus> status, List<DisputeCategory> category,
                                 String priority, String assigned, String sort, int page, int size, Long actorId) {
        int pg = Math.max(0, page);
        int sz = size <= 0 ? 20 : size;
        String asg = StringUtils.hasText(assigned) ? assigned.trim().toUpperCase() : "ANYONE";
        String srt = StringUtils.hasText(sort) ? sort.trim().toUpperCase() : "PRIORITY";

        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder("1=1");

        if (StringUtils.hasText(q)) {
            where.append(" AND (LOWER(d.playerName) LIKE :q OR LOWER(d.ownerName) LIKE :q"
                    + " OR LOWER(d.venueName) LIKE :q OR CONCAT(d.booking.id, '') LIKE :qraw"
                    + " OR (d.booking.groupId IS NOT NULL AND d.booking.groupId LIKE :qraw))");
            params.put("q", "%" + q.trim().toLowerCase() + "%");
            params.put("qraw", "%" + q.trim() + "%");
        }
        if (status != null && !status.isEmpty()) {
            where.append(" AND d.status IN :statuses");
            params.put("statuses", status.stream().map(s -> DisputeEntity.DisputeStatus.valueOf(s.getValue())).toList());
        }
        if (category != null && !category.isEmpty()) {
            where.append(" AND d.category IN :categories");
            params.put("categories", category.stream().map(c -> DisputeEntity.Category.valueOf(c.getValue())).toList());
        }
        if (StringUtils.hasText(priority)) {
            where.append(" AND d.priority = :priority");
            params.put("priority", DisputeEntity.Priority.valueOf(priority.trim().toUpperCase()));
        }
        switch (asg) {
            case "ME" -> { where.append(" AND d.assignedTo.id = :actorId"); params.put("actorId", actorId); }
            case "UNASSIGNED" -> where.append(" AND d.assignedTo IS NULL");
            default -> { /* ANYONE */ }
        }

        String order = switch (srt) {
            case "OLDEST" -> "d.raisedAt ASC";
            case "NEWEST" -> "d.raisedAt DESC";
            case "LONGEST_WAITING" -> "CASE WHEN d.waitingOn <> 'NONE' THEN 0 ELSE 1 END ASC, d.raisedAt ASC";
            default -> "CASE d.priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC, d.raisedAt ASC"; // PRIORITY
        };

        TypedQuery<DisputeEntity> query = em.createQuery(
                "SELECT d FROM DisputeEntity d WHERE " + where + " ORDER BY " + order, DisputeEntity.class);
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(d) FROM DisputeEntity d WHERE " + where, Long.class);
        params.forEach((k, v) -> { query.setParameter(k, v); countQuery.setParameter(k, v); });

        long total = countQuery.getSingleResult();
        query.setFirstResult(pg * sz);
        query.setMaxResults(sz);
        List<DisputeRow> rows = query.getResultList().stream().map(this::toRow).toList();
        int totalPages = (int) Math.ceil((double) total / sz);
        return new AdminDisputePage().content(rows).totalElements(total).totalPages(totalPages).size(sz).number(pg);
    }

    @Override
    @Transactional(readOnly = true)
    public DisputeStats stats() {
        LocalDateTime now = LocalDateTime.now();
        long open = disputeRepository.countByStatusIn(List.of(
                DisputeEntity.DisputeStatus.OPEN, DisputeEntity.DisputeStatus.UNDER_REVIEW));
        long needsInfo = disputeRepository.countByStatus(DisputeEntity.DisputeStatus.NEEDS_INFO);
        long overdue = disputeRepository.countOverdue(ACTIVE_STATUSES, now.minusHours(48));
        long resolvedThisWeek = disputeRepository.countByStatusAndResolvedAtAfter(
                DisputeEntity.DisputeStatus.RESOLVED, now.minusDays(7));

        List<Object[]> durations = disputeRepository.findResolutionDurations(
                DisputeEntity.DisputeStatus.RESOLVED, now.minusDays(30));
        long avgHours = 0;
        if (!durations.isEmpty()) {
            long totalH = 0; int n = 0;
            for (Object[] r : durations) {
                if (r[0] instanceof LocalDateTime raised && r[1] instanceof LocalDateTime resolved) {
                    totalH += java.time.Duration.between(raised, resolved).toHours();
                    n++;
                }
            }
            avgHours = n > 0 ? totalH / n : 0;
        }
        return new DisputeStats()
                .open((int) open).needsInfo((int) needsInfo).overdue((int) overdue)
                .resolvedThisWeek((int) resolvedThisWeek).avgResolutionHours((int) avgHours);
    }

    // ─── Detail ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DisputeDetail detail(Long disputeId) {
        return toDetail(require(disputeId));
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DisputeDetail assign(Long disputeId, Long adminId, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        DisputeEntity d = require(disputeId);
        requireOpen(d);
        Long targetAdmin = adminId != null ? adminId : actorId;
        UserEntity admin = targetAdmin != null ? userRepository.findById(targetAdmin).orElse(null) : null;
        d.setAssignedTo(admin);
        if (d.getStatus() == DisputeEntity.DisputeStatus.OPEN) {
            d.setStatus(DisputeEntity.DisputeStatus.UNDER_REVIEW);
        }
        disputeRepository.save(d);
        event(d, "ASSIGN", actorId, "Assigned to " + (admin != null ? admin.getName() : "—"));
        return toDetail(d);
    }

    @Override
    @Transactional
    public DisputeDetail message(Long disputeId, String audience, List<String> channels, String body, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        if (!StringUtils.hasText(body)) throw new BadRequestException("Message body is required.");
        if (channels == null || channels.isEmpty()) throw new BadRequestException("At least one channel is required.");
        DisputeEntity d = require(disputeId);
        String aud = audience != null ? audience.toUpperCase() : "BOTH";
        UserEntity actor = actor(actorId);
        String actorName = actor != null ? actor.getName() : "Admin";

        messageRepository.save(DisputeMessageEntity.builder()
                .dispute(d).senderRole("ADMIN").senderName(actorName).body(body).build());

        if (aud.equals("PLAYER") || aud.equals("BOTH")) {
            notificationService.createNotification(d.getPlayer(), "Message about your dispute", body,
                    NotificationEntity.NotificationType.SYSTEM);
        }
        if (aud.equals("OWNER") || aud.equals("BOTH")) {
            notificationService.createNotification(d.getOwner(), "Message about a dispute", body,
                    NotificationEntity.NotificationType.SYSTEM);
        }
        event(d, "MESSAGE", actorId, "Messaged " + aud + " via " + String.join(",", channels));
        return toDetail(d);
    }

    @Override
    @Transactional
    public DisputeDetail requestInfo(Long disputeId, PartyRole party, String message, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        if (party == null) throw new BadRequestException("party is required.");
        if (!StringUtils.hasText(message)) throw new BadRequestException("A message is required.");
        DisputeEntity d = require(disputeId);
        requireOpen(d);
        UserEntity target = party == PartyRole.PLAYER ? d.getPlayer() : d.getOwner();
        UserEntity actor = actor(actorId);

        messageRepository.save(DisputeMessageEntity.builder()
                .dispute(d).senderRole("ADMIN").senderName(actor != null ? actor.getName() : "Admin")
                .body(message).build());
        d.setStatus(DisputeEntity.DisputeStatus.NEEDS_INFO);
        d.setWaitingOn(party == PartyRole.PLAYER ? DisputeEntity.WaitingOn.PLAYER : DisputeEntity.WaitingOn.OWNER);
        disputeRepository.save(d);

        notificationService.createNotification(target, "Information requested for your dispute", message,
                NotificationEntity.NotificationType.SYSTEM);
        event(d, "REQUEST_INFO", actorId, "Requested info from " + party.getValue());
        return toDetail(d);
    }

    @Override
    @Transactional
    public DisputeDetail addNote(Long disputeId, String body, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        if (!StringUtils.hasText(body)) throw new BadRequestException("Note body is required.");
        DisputeEntity d = require(disputeId);
        UserEntity actor = actor(actorId);
        noteRepository.save(DisputeNoteEntity.builder()
                .dispute(d).author(actor).authorName(actor != null ? actor.getName() : "Admin").body(body).build());
        event(d, "ADD_NOTE", actorId, "Added an internal note");
        return toDetail(d);
    }

    @Override
    @Transactional
    public DisputeDetail resolve(Long disputeId, DisputeResolveBody body, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        if (body == null || body.getOutcome() == null || body.getAtFault() == null
                || !StringUtils.hasText(body.getRulingNote())) {
            throw new BadRequestException("outcome, atFault and a ruling note are required.");
        }
        DisputeEntity d = require(disputeId);
        if (d.getStatus() == DisputeEntity.DisputeStatus.RESOLVED
                || d.getStatus() == DisputeEntity.DisputeStatus.DISMISSED) {
            throw new ConflictException("This dispute is already closed.");
        }
        DisputeEntity.FaultParty atFault = DisputeEntity.FaultParty.valueOf(body.getAtFault().toUpperCase());

        // Validate the consequence (RBAC + shape) BEFORE mutating anything.
        var c = body.getConsequence();
        boolean hasConsequence = c != null && c.getAction() != null && c.getAction() != ConsequenceAction.NONE;
        if (hasConsequence) {
            if (c.getTarget() == null) throw new BadRequestException("consequence.target is required.");
            if (!StringUtils.hasText(c.getReason())) throw new BadRequestException("consequence.reason is required.");
            if (c.getAction() == ConsequenceAction.BAN) requireCanBan(actorId);
        }

        d.setStatus(DisputeEntity.DisputeStatus.RESOLVED);
        d.setWaitingOn(DisputeEntity.WaitingOn.NONE);
        d.setOutcome(DisputeEntity.Outcome.valueOf(body.getOutcome().getValue()));
        d.setAtFault(atFault);
        d.setRulingNote(body.getRulingNote());
        d.setRecommendedRefundAmount(body.getRecommendedRefundAmount());
        d.setResolvedBy(actor(actorId));
        d.setResolvedAt(LocalDateTime.now());
        if (hasConsequence) {
            d.setConsequenceTarget(DisputeEntity.FaultParty.valueOf(c.getTarget().getValue()));
            d.setConsequenceAction(DisputeEntity.Consequence.valueOf(c.getAction().getValue()));
        }
        disputeRepository.save(d);

        // Informational refund — NEVER processed; the owner is asked to settle offline.
        if (body.getRecommendedRefundAmount() != null && body.getRecommendedRefundAmount() > 0) {
            notificationService.createNotification(d.getOwner(), "Please refund a player directly",
                    String.format("Following a dispute ruling, please refund ₹%d to %s directly. "
                            + "Score-Adda does not process this payment.",
                            body.getRecommendedRefundAmount(), d.getPlayerName()),
                    NotificationEntity.NotificationType.SYSTEM);
        }

        Long incremented = null;
        if (atFault != DisputeEntity.FaultParty.NONE) {
            UserEntity faulted = atFault == DisputeEntity.FaultParty.PLAYER ? d.getPlayer() : d.getOwner();
            bumpDisputeRisk(faulted);
            incremented = faulted.getId();
        }

        // Apply the optional account consequence via the existing moderation service.
        if (hasConsequence) {
            applyConsequence(d, c, actorId, incremented);
        }

        notificationService.createNotification(d.getPlayer(), "Your dispute was resolved", d.getRulingNote(),
                NotificationEntity.NotificationType.SYSTEM);
        notificationService.createNotification(d.getOwner(), "A dispute was resolved", d.getRulingNote(),
                NotificationEntity.NotificationType.SYSTEM);

        event(d, "RESOLVE", actorId, "Resolved: " + d.getOutcome().name()
                + (atFault != DisputeEntity.FaultParty.NONE ? " (at fault: " + atFault.name() + ")" : ""));
        if (hasConsequence) {
            event(d, "APPLY_CONSEQUENCE", actorId,
                    c.getAction().getValue() + " applied to " + c.getTarget().getValue());
        }
        return toDetail(d);
    }

    @Override
    @Transactional
    public DisputeDetail dismiss(Long disputeId, String reason, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        if (!StringUtils.hasText(reason)) throw new BadRequestException("A reason is required to dismiss.");
        DisputeEntity d = require(disputeId);
        if (d.getStatus() == DisputeEntity.DisputeStatus.RESOLVED
                || d.getStatus() == DisputeEntity.DisputeStatus.DISMISSED) {
            throw new ConflictException("This dispute is already closed.");
        }
        d.setStatus(DisputeEntity.DisputeStatus.DISMISSED);
        d.setWaitingOn(DisputeEntity.WaitingOn.NONE);
        d.setOutcome(DisputeEntity.Outcome.DISMISSED);
        d.setRulingNote(reason);
        d.setResolvedBy(actor(actorId));
        d.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(d);
        event(d, "DISMISS", actorId, "Dismissed: " + reason);
        return toDetail(d);
    }

    @Override
    @Transactional
    public DisputeDetail reopen(Long disputeId, String reason, Long actorId) {
        adminPermissionService.requireWrite(actorId);
        if (!StringUtils.hasText(reason)) throw new BadRequestException("A reason is required to reopen.");
        DisputeEntity d = require(disputeId);
        if (d.getStatus() != DisputeEntity.DisputeStatus.RESOLVED
                && d.getStatus() != DisputeEntity.DisputeStatus.DISMISSED) {
            throw new ConflictException("Only a resolved or dismissed dispute can be reopened.");
        }
        d.setStatus(DisputeEntity.DisputeStatus.UNDER_REVIEW);
        disputeRepository.save(d);
        event(d, "REOPEN", actorId, "Reopened: " + reason);
        return toDetail(d);
    }

    // ─── Consequence dispatch (reuses the moderation service) ─────────────────

    private void applyConsequence(DisputeEntity d, DisputeResolveBodyConsequence c, Long actorId, Long alreadyBumped) {
        boolean targetPlayer = c.getTarget() == PartyRole.PLAYER;
        UserEntity party = targetPlayer ? d.getPlayer() : d.getOwner();
        Long partyId = party.getId();
        String reason = c.getReason();

        switch (c.getAction()) {
            case WARN -> {
                notificationService.createNotification(party, "Formal warning",
                        "Following a dispute, a formal warning has been issued: " + reason,
                        NotificationEntity.NotificationType.SYSTEM);
                partyAudit(actorId, party, "WARN", reason);
            }
            case FLAG -> {
                if (!partyId.equals(alreadyBumped)) bumpDisputeRisk(party);
                partyAudit(actorId, party, "FLAG", reason);
            }
            case SUSPEND -> {
                if (targetPlayer) {
                    adminPlayerService.suspend(partyId,
                            new PlayerSuspendBody().reason(reason).until(c.getUntil()), actorId);
                } else {
                    adminOwnerService.suspend(partyId,
                            new OwnerSuspendBody().reason(reason).until(c.getUntil()), actorId);
                }
            }
            case BAN -> {
                if (targetPlayer) {
                    adminPlayerService.ban(partyId, new PlayerReasonBody().reason(reason), actorId);
                } else {
                    adminOwnerService.ban(partyId, new OwnerBanBody().reason(reason), actorId);
                }
            }
            default -> { /* NONE */ }
        }
    }

    /** Bump the party's dispute-at-fault count (feeds the Players/Owners risk model). */
    private void bumpDisputeRisk(UserEntity party) {
        party.setDisputeAtFaultCount(party.getDisputeAtFaultCount() + 1);
        userRepository.save(party);
    }

    private void partyAudit(Long actorId, UserEntity target, String action, String reason) {
        auditRepository.save(AdminAuditEntity.builder()
                .actor(actor(actorId)).target(target).action(action).reason(reason).build());
    }

    /** Phase 1 RBAC: BAN consequence is super-admin only; defaults to allowed until sub-roles exist. */
    /** Applying a BAN consequence from a dispute is SUPER_ADMIN-only (central RBAC). */
    private void requireCanBan(Long actorId) {
        adminPermissionService.requireModerateHard(actorId);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private DisputeRow toRow(DisputeEntity d) {
        return new DisputeRow()
                .disputeId(d.getId())
                .title(titleOf(d))
                .category(DisputeCategory.valueOf(d.getCategory().name()))
                .status(DisputeStatus.valueOf(d.getStatus().name()))
                .priority(DisputePriority.valueOf(d.getPriority().name()))
                .bookingRef(bookingRef(d.getBooking()))
                .venueName(d.getVenueName())
                .playerName(d.getPlayerName())
                .ownerName(d.getOwnerName())
                .assignedToName(d.getAssignedTo() != null ? d.getAssignedTo().getName() : null)
                .waitingOn(WaitingOn.valueOf(d.getWaitingOn().name()))
                .raisedAt(odt(d.getRaisedAt()))
                .isOverdue(isOverdue(d));
    }

    private DisputeDetail toDetail(DisputeEntity d) {
        DisputeDetail dto = new DisputeDetail()
                .disputeId(d.getId())
                .title(titleOf(d))
                .category(DisputeCategory.valueOf(d.getCategory().name()))
                .status(DisputeStatus.valueOf(d.getStatus().name()))
                .priority(DisputePriority.valueOf(d.getPriority().name()))
                .raisedByRole(PartyRole.valueOf(d.getRaisedByRole().name()))
                .raisedAt(odt(d.getRaisedAt()))
                .assignedToName(d.getAssignedTo() != null ? d.getAssignedTo().getName() : null)
                .waitingOn(WaitingOn.valueOf(d.getWaitingOn().name()))
                .isOverdue(isOverdue(d))
                .slaHours(d.getSlaHours())
                .booking(bookingRefDto(d.getBooking()))
                .player(partyMini(d.getPlayer(), DisputeEntity.PartyRole.PLAYER))
                .owner(partyMini(d.getOwner(), DisputeEntity.PartyRole.OWNER))
                .conversation(conversation(d))
                .internalNotes(internalNotes(d))
                .timeline(timeline(d))
                .availableActions(availableActions(d.getStatus()));
        if (d.getOutcome() != null || d.getStatus() == DisputeEntity.DisputeStatus.RESOLVED
                || d.getStatus() == DisputeEntity.DisputeStatus.DISMISSED) {
            dto.resolution(new DisputeResolution()
                    .outcome(d.getOutcome() != null ? ResolutionOutcome.valueOf(d.getOutcome().name()) : null)
                    .atFault(d.getAtFault() != null ? d.getAtFault().name() : "NONE")
                    .rulingNote(d.getRulingNote())
                    .recommendedRefundAmount(d.getRecommendedRefundAmount())
                    .consequenceTarget(d.getConsequenceTarget() != null ? d.getConsequenceTarget().name() : null)
                    .consequenceAction(d.getConsequenceAction() != null
                            ? ConsequenceAction.valueOf(d.getConsequenceAction().name()) : ConsequenceAction.NONE)
                    .resolvedByName(d.getResolvedBy() != null ? d.getResolvedBy().getName() : null)
                    .resolvedAt(odt(d.getResolvedAt())));
        }
        return dto;
    }

    private List<DisputeConversationItem> conversation(DisputeEntity d) {
        List<DisputeMessageEntity> msgs = messageRepository.findByDispute_IdOrderByCreatedAtAsc(d.getId());
        List<DisputeConversationItem> items = new ArrayList<>();
        // Legacy disputes (raised before the conversation table) have no messages — synthesize the
        // original complaint so the thread is never empty.
        if (msgs.isEmpty() && StringUtils.hasText(d.getIssue())) {
            items.add(new DisputeConversationItem()
                    .id(0L).senderRole(d.getRaisedByRole().name()).senderName(d.getPlayerName())
                    .body(d.getIssue()).attachments(List.of()).createdAt(odt(d.getRaisedAt())));
        }
        for (DisputeMessageEntity m : msgs) {
            items.add(new DisputeConversationItem()
                    .id(m.getId()).senderRole(m.getSenderRole()).senderName(m.getSenderName())
                    .body(m.getBody()).attachments(m.getAttachments() != null ? m.getAttachments() : List.of())
                    .createdAt(odt(m.getCreatedAt())));
        }
        return items;
    }

    private List<DisputeInternalNote> internalNotes(DisputeEntity d) {
        return noteRepository.findByDispute_IdOrderByCreatedAtAsc(d.getId()).stream()
                .map(n -> new DisputeInternalNote()
                        .id(n.getId()).authorName(n.getAuthorName()).body(n.getBody()).createdAt(odt(n.getCreatedAt())))
                .toList();
    }

    private List<DisputeTimelineItem> timeline(DisputeEntity d) {
        return eventRepository.findByDispute_IdOrderByCreatedAtAsc(d.getId()).stream()
                .map(e -> new DisputeTimelineItem()
                        .id(e.getId()).action(e.getAction()).actorName(e.getActorName())
                        .summary(e.getSummary()).createdAt(odt(e.getCreatedAt())))
                .toList();
    }

    private PartyMini partyMini(UserEntity u, DisputeEntity.PartyRole role) {
        long prior = role == DisputeEntity.PartyRole.PLAYER
                ? disputeRepository.countByPlayer(u) : disputeRepository.countByOwner(u);
        return new PartyMini()
                .id(u.getId())
                .role(PartyRole.valueOf(role.name()))
                .name(u.getName())
                .phoneVerified(u.isPhoneVerified())
                .riskLevel(riskFromFaults(u.getDisputeAtFaultCount()))
                .priorDisputeCount((int) prior)
                .rating(null);
    }

    private DisputeBookingRef bookingRefDto(BookingEntity b) {
        if (b == null) return null;
        return new DisputeBookingRef()
                .bookingId(b.getId())
                .ref(bookingRef(b))
                .venueName(b.getVenue() != null ? b.getVenue().getName() : null)
                .date(b.getDate())
                .slotLabel(b.getStartTime() != null && b.getEndTime() != null
                        ? b.getStartTime() + " – " + b.getEndTime() : "")
                .amount(b.getAmount())
                .methodLabel(null) // payments are direct owner↔player; no instrument is stored
                .status(b.getStatus() != null ? b.getStatus().name() : null);
    }

    private List<String> availableActions(DisputeEntity.DisputeStatus status) {
        List<String> base = switch (status) {
            case OPEN -> List.of("ASSIGN", "MESSAGE", "REQUEST_INFO", "ADD_NOTE", "RESOLVE", "DISMISS");
            case UNDER_REVIEW -> List.of("MESSAGE", "REQUEST_INFO", "ADD_NOTE", "APPLY_CONSEQUENCE", "RESOLVE", "DISMISS");
            case NEEDS_INFO -> List.of("MESSAGE", "ADD_NOTE", "APPLY_CONSEQUENCE", "RESOLVE", "DISMISS");
            case RESOLVED, DISMISSED -> List.of("ADD_NOTE", "REOPEN");
        };
        // READ_ONLY admins get no actions; SUPPORT/SUPER keep all (BAN consequence is gated at resolve time).
        return adminPermissionService.filterActions(base);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private DisputeEntity require(Long id) {
        return disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", id));
    }

    private void requireOpen(DisputeEntity d) {
        if (d.getStatus() == DisputeEntity.DisputeStatus.RESOLVED
                || d.getStatus() == DisputeEntity.DisputeStatus.DISMISSED) {
            throw new ConflictException("This dispute is closed.");
        }
    }

    private UserEntity actor(Long actorId) {
        return actorId != null ? userRepository.findById(actorId).orElse(null) : null;
    }

    private void event(DisputeEntity d, String action, Long actorId, String summary) {
        UserEntity actor = actor(actorId);
        eventRepository.save(DisputeEventEntity.builder()
                .dispute(d).action(action).actorName(actor != null ? actor.getName() : "Admin")
                .summary(summary).build());
    }

    private boolean isOverdue(DisputeEntity d) {
        if (!ACTIVE_STATUSES.contains(d.getStatus())) return false;
        LocalDateTime base = d.getRaisedAt() != null ? d.getRaisedAt()
                : (d.getCreatedAt() != null ? d.getCreatedAt().atStartOfDay() : null);
        return base != null && base.plusHours(d.getSlaHours()).isBefore(LocalDateTime.now());
    }

    private static String titleOf(DisputeEntity d) {
        return labelOf(d.getCategory()) + " · " + d.getVenueName();
    }

    private static String labelOf(DisputeEntity.Category c) {
        return switch (c) {
            case OWNER_NO_SHOW -> "Owner no-show";
            case OWNER_CANCELLATION -> "Owner cancellation";
            case DOUBLE_BOOKING -> "Double booking";
            case NOT_AS_DESCRIBED -> "Not as described";
            case REFUND_NOT_GIVEN -> "Refund not given";
            case OVERCHARGED -> "Overcharged";
            case SAFETY_BEHAVIOR -> "Safety / behaviour";
            case OTHER -> "Other";
        };
    }

    private static String riskFromFaults(int faults) {
        if (faults >= 3) return "HIGH";
        if (faults >= 1) return "MEDIUM";
        return "NONE";
    }

    private static String bookingRef(BookingEntity b) {
        if (b == null) return null;
        if (StringUtils.hasText(b.getGroupId())) return b.getGroupId();
        return "#" + b.getId();
    }

    private static OffsetDateTime odt(LocalDateTime ldt) {
        return ldt != null ? ldt.atOffset(ZoneOffset.UTC) : null;
    }
}
