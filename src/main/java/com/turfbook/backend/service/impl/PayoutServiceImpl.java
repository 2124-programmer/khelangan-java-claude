package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.PayoutDto;
import com.turfbook.backend.dto.PayoutPage;
import com.turfbook.backend.entity.PayoutEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.mapper.PayoutMapper;
import com.turfbook.backend.repository.PayoutRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRepository payoutRepository;
    private final UserRepository userRepository;
    private final PayoutMapper payoutMapper;

    @Override
    @Transactional(readOnly = true)
    public PayoutPage listOwnerPayouts(Long ownerId, int page, int size) {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));
        Pageable pageable = PageRequest.of(page, size);
        Page<PayoutEntity> entityPage = payoutRepository.findByOwnerOrderByCreatedAtDesc(owner, pageable);
        return toPayoutPage(entityPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutPage adminListPayouts(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        PayoutEntity.PayoutStatus payoutStatus = null;
        if (StringUtils.hasText(status)) {
            try {
                payoutStatus = PayoutEntity.PayoutStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        Page<PayoutEntity> entityPage = payoutRepository.findAllByStatus(payoutStatus, pageable);
        return toPayoutPage(entityPage);
    }

    @Override
    @Transactional
    public PayoutDto processPayout(Long id) {
        PayoutEntity payout = payoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payout", "id", id));

        if (payout.getStatus() != PayoutEntity.PayoutStatus.PENDING) {
            throw new ConflictException("Only PENDING payouts can be processed. Current status: " + payout.getStatus());
        }

        payout.setStatus(PayoutEntity.PayoutStatus.SETTLED);
        return payoutMapper.toDto(payoutRepository.save(payout));
    }

    private PayoutPage toPayoutPage(Page<PayoutEntity> entityPage) {
        PayoutPage dto = new PayoutPage();
        dto.setContent(entityPage.getContent().stream().map(payoutMapper::toDto).toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }
}
