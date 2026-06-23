package com.turfbook.backend.controller;

import com.turfbook.backend.api.AdminDisputesApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.AdminDisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController implements AdminDisputesApi {

    private final AdminDisputeService service;

    @Override
    public ResponseEntity<AdminDisputePage> listAdminDisputes(String q, List<DisputeStatus> status,
            List<DisputeCategory> category, String priority, String assigned, String sort,
            Integer page, Integer size) {
        return ResponseEntity.ok(service.list(q, status, category, priority, assigned, sort,
                page != null ? page : 0, size != null ? size : 20, currentUserId()));
    }

    @Override
    public ResponseEntity<DisputeStats> getDisputeStats() {
        return ResponseEntity.ok(service.stats());
    }

    @Override
    public ResponseEntity<DisputeDetail> getDisputeDetail(Long disputeId) {
        return ResponseEntity.ok(service.detail(disputeId));
    }

    @Override
    public ResponseEntity<DisputeDetail> assignDispute(Long disputeId, AssignDisputeRequest body) {
        Long adminId = body != null ? body.getAdminId() : null;
        return ResponseEntity.ok(service.assign(disputeId, adminId, currentUserId()));
    }

    @Override
    public ResponseEntity<DisputeDetail> messageDisputeParty(Long disputeId, MessageDisputePartyRequest body) {
        String audience = body.getAudience() != null ? body.getAudience().getValue() : null;
        return ResponseEntity.ok(service.message(disputeId, audience, body.getChannels(), body.getBody(), currentUserId()));
    }

    @Override
    public ResponseEntity<DisputeDetail> requestDisputeInfo(Long disputeId, RequestDisputeInfoRequest body) {
        return ResponseEntity.ok(service.requestInfo(disputeId, body.getParty(), body.getMessage(), currentUserId()));
    }

    @Override
    public ResponseEntity<DisputeDetail> addDisputeNote(Long disputeId, AddDisputeNoteRequest body) {
        return ResponseEntity.ok(service.addNote(disputeId, body.getBody(), currentUserId()));
    }

    @Override
    public ResponseEntity<DisputeDetail> adminResolveDispute(Long disputeId, DisputeResolveBody body) {
        return ResponseEntity.ok(service.resolve(disputeId, body, currentUserId()));
    }

    @Override
    public ResponseEntity<DisputeDetail> dismissDispute(Long disputeId, DisputeReasonBody body) {
        return ResponseEntity.ok(service.dismiss(disputeId, body.getReason(), currentUserId()));
    }

    @Override
    public ResponseEntity<DisputeDetail> reopenDispute(Long disputeId, DisputeReasonBody body) {
        return ResponseEntity.ok(service.reopen(disputeId, body.getReason(), currentUserId()));
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof UserPrincipal up ? up.getId() : null;
    }
}
