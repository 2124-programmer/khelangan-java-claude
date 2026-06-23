package com.turfbook.backend.service;

import com.turfbook.backend.dto.AdminDisputePage;
import com.turfbook.backend.dto.DisputeCategory;
import com.turfbook.backend.dto.DisputeDetail;
import com.turfbook.backend.dto.DisputeResolveBody;
import com.turfbook.backend.dto.DisputeStats;
import com.turfbook.backend.dto.DisputeStatus;
import com.turfbook.backend.dto.PartyRole;

import java.util.List;

/**
 * Admin disputes triage + resolution. The admin is an arbiter, not a cashier: resolutions issue
 * rulings + account consequences (reusing the Player/Owner moderation service) and, when money is
 * owed, only NOTIFY the owner to settle offline — the platform never processes a refund.
 */
public interface AdminDisputeService {

    AdminDisputePage list(String q, List<DisputeStatus> status, List<DisputeCategory> category,
                          String priority, String assigned, String sort, int page, int size, Long actorId);

    DisputeStats stats();

    DisputeDetail detail(Long disputeId);

    DisputeDetail assign(Long disputeId, Long adminId, Long actorId);

    DisputeDetail message(Long disputeId, String audience, List<String> channels, String body, Long actorId);

    DisputeDetail requestInfo(Long disputeId, PartyRole party, String message, Long actorId);

    DisputeDetail addNote(Long disputeId, String body, Long actorId);

    DisputeDetail resolve(Long disputeId, DisputeResolveBody body, Long actorId);

    DisputeDetail dismiss(Long disputeId, String reason, Long actorId);

    DisputeDetail reopen(Long disputeId, String reason, Long actorId);
}
