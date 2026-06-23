package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.OwnerSettingsDto;
import com.turfbook.backend.dto.UpdateOwnerSettingsRequest;
import com.turfbook.backend.entity.FeatureCode;
import com.turfbook.backend.entity.OwnerSettingsEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.OwnerSettingsRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.OwnerSettingsService;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerSettingsServiceImpl implements OwnerSettingsService {

    private final OwnerSettingsRepository ownerSettingsRepository;
    private final UserRepository userRepository;
    private final SubscriptionGate subscriptionGate;

    @Override
    @Transactional
    public OwnerSettingsDto getSettings(Long ownerId) {
        return toDto(findOrCreate(ownerId));
    }

    @Override
    @Transactional
    public OwnerSettingsDto updateSettings(Long ownerId, UpdateOwnerSettingsRequest request) {
        OwnerSettingsEntity settings = findOrCreate(ownerId);
        if (request.getAutoAcceptBookings() != null) {
            // Gate enabling auto-accept on the owner holding a plan that grants AUTO_ACCEPT
            // on at least one venue. Disabling is always allowed.
            if (Boolean.TRUE.equals(request.getAutoAcceptBookings())
                    && !subscriptionGate.ownerHasFeatureOnAnyVenue(ownerId, FeatureCode.AUTO_ACCEPT)) {
                throw new ForbiddenException(
                        "Auto-accept requires a Growth plan or higher. Upgrade a venue Subscription to enable it.");
            }
            settings.setAutoAcceptBookings(request.getAutoAcceptBookings());
        }
        if (request.getPushNotificationsEnabled() != null) {
            settings.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        return toDto(ownerSettingsRepository.save(settings));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAutoAccept(Long ownerId) {
        return ownerSettingsRepository.findByOwnerId(ownerId)
                .map(OwnerSettingsEntity::isAutoAcceptBookings)
                .orElse(false);
    }

    private OwnerSettingsEntity findOrCreate(Long ownerId) {
        return ownerSettingsRepository.findByOwnerId(ownerId)
                .orElseGet(() -> {
                    UserEntity owner = userRepository.findById(ownerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));
                    return ownerSettingsRepository.save(
                            OwnerSettingsEntity.builder().owner(owner).build());
                });
    }

    private OwnerSettingsDto toDto(OwnerSettingsEntity entity) {
        OwnerSettingsDto dto = new OwnerSettingsDto();
        dto.setAutoAcceptBookings(entity.isAutoAcceptBookings());
        dto.setPushNotificationsEnabled(entity.isPushNotificationsEnabled());
        return dto;
    }
}
