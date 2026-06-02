package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.CreateDisputeRequest;
import com.turfbook.backend.dto.DisputeDto;
import com.turfbook.backend.dto.DisputePage;
import com.turfbook.backend.dto.ResolveDisputeRequest;
import com.turfbook.backend.entity.*;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.DisputeMapper;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.DisputeRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.DisputeService;
import com.turfbook.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DisputeMapper disputeMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public DisputePage listDisputes(UserEntity currentUser, int page, int size) {
        log.info("DisputeService.listDisputes() called - userId={}, role={}", currentUser.getId(), currentUser.getRole());
        Pageable pageable = PageRequest.of(page, size);
        Page<DisputeEntity> entityPage;

        switch (currentUser.getRole()) {
            case PLAYER -> entityPage = disputeRepository.findByPlayerOrderByCreatedAtDesc(currentUser, pageable);
            case OWNER  -> entityPage = disputeRepository.findByOwnerOrderByCreatedAtDesc(currentUser, pageable);
            default     -> entityPage = disputeRepository.findAllOrderByCreatedAtDesc(pageable);
        }

        return toDisputePage(entityPage);
    }

    @Override
    @Transactional
    public DisputeDto createDispute(Long playerId, CreateDisputeRequest request) {
        log.info("DisputeService.createDispute() called - playerId={}, bookingId={}", playerId, request.getBookingId());
        UserEntity player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", playerId));

        BookingEntity booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        if (!booking.getPlayer().getId().equals(playerId)) {
            throw new UnauthorizedException("You can only raise disputes for your own bookings");
        }

        UserEntity owner = booking.getVenue().getOwner();

        DisputeEntity dispute = DisputeEntity.builder()
                .booking(booking)
                .player(player)
                .playerName(player.getName())
                .owner(owner)
                .ownerName(owner.getName())
                .venueName(booking.getVenue().getName())
                .issue(request.getIssue())
                .status(DisputeEntity.DisputeStatus.OPEN)
                .build();

        dispute = disputeRepository.save(dispute);

        notificationService.createNotification(
                owner,
                "New Dispute Raised",
                String.format("Player %s has raised a dispute for booking at %s. Issue: %s",
                        player.getName(), booking.getVenue().getName(), request.getIssue()),
                NotificationEntity.NotificationType.SYSTEM
        );

        log.info("DisputeService.createDispute() completed - disputeId={}", dispute.getId());
        return disputeMapper.toDto(dispute);
    }

    @Override
    @Transactional
    public DisputeDto resolveDispute(Long id, ResolveDisputeRequest request) {
        log.info("DisputeService.resolveDispute() called - id={}", id);
        DisputeEntity dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", id));

        dispute.setStatus(DisputeEntity.DisputeStatus.RESOLVED);
        dispute.setResolvedNote(request.getResolvedNote());
        dispute = disputeRepository.save(dispute);

        notificationService.createNotification(
                dispute.getPlayer(),
                "Dispute Resolved",
                String.format("Your dispute for booking at %s has been resolved. Note: %s",
                        dispute.getVenueName(), request.getResolvedNote()),
                NotificationEntity.NotificationType.SYSTEM
        );
        notificationService.createNotification(
                dispute.getOwner(),
                "Dispute Resolved",
                String.format("A dispute from %s for venue %s has been resolved. Note: %s",
                        dispute.getPlayerName(), dispute.getVenueName(), request.getResolvedNote()),
                NotificationEntity.NotificationType.SYSTEM
        );

        log.info("DisputeService.resolveDispute() completed - id={}, status=RESOLVED", id);
        return disputeMapper.toDto(dispute);
    }

    private DisputePage toDisputePage(Page<DisputeEntity> entityPage) {
        DisputePage dto = new DisputePage();
        dto.setContent(entityPage.getContent().stream().map(disputeMapper::toDto).toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }
}
