package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.OwnerSettingsDto;
import com.turfbook.backend.dto.UpdateOwnerSettingsRequest;
import com.turfbook.backend.entity.OwnerSettingsEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.OwnerSettingsRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.OwnerSettingsService;
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
