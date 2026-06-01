package com.turfbook.backend.controller;

import com.turfbook.backend.api.DisputesApi;
import com.turfbook.backend.dto.CreateDisputeRequest;
import com.turfbook.backend.dto.DisputeDto;
import com.turfbook.backend.dto.DisputePage;
import com.turfbook.backend.dto.ResolveDisputeRequest;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DisputeController implements DisputesApi {

    private final DisputeService disputeService;
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DisputePage> listDisputes(Integer page, Integer size) {
        UserPrincipal principal = getPrincipal();
        UserEntity currentUser = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(disputeService.listDisputes(currentUser,
                page != null ? page : 0, size != null ? size : 20));
    }

    @Override
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<DisputeDto> createDispute(CreateDisputeRequest request) {
        UserPrincipal principal = getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disputeService.createDispute(principal.getId(), request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeDto> resolveDispute(Long id, ResolveDisputeRequest request) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, request));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
