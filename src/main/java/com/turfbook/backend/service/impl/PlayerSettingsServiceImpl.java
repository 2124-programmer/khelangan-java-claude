package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.PlayerSettingsDto;
import com.turfbook.backend.dto.UpdatePlayerSettingsRequest;
import com.turfbook.backend.entity.PlayerSettingsEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.PlayerSettingsRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.PlayerSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerSettingsServiceImpl implements PlayerSettingsService {

    private final PlayerSettingsRepository playerSettingsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PlayerSettingsDto getSettings(Long playerId) {
        return toDto(findOrCreate(playerId));
    }

    @Override
    @Transactional
    public PlayerSettingsDto updateSettings(Long playerId, UpdatePlayerSettingsRequest request) {
        PlayerSettingsEntity settings = findOrCreate(playerId);
        if (request.getPushNotificationsEnabled() != null) {
            settings.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getEmailNotificationsEnabled() != null) {
            settings.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        return toDto(playerSettingsRepository.save(settings));
    }

    private PlayerSettingsEntity findOrCreate(Long playerId) {
        return playerSettingsRepository.findByPlayerId(playerId)
                .orElseGet(() -> {
                    UserEntity player = userRepository.findById(playerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", playerId));
                    return playerSettingsRepository.save(
                            PlayerSettingsEntity.builder().player(player).build());
                });
    }

    private PlayerSettingsDto toDto(PlayerSettingsEntity entity) {
        return PlayerSettingsDto.builder()
                .pushNotificationsEnabled(entity.isPushNotificationsEnabled())
                .emailNotificationsEnabled(entity.isEmailNotificationsEnabled())
                .build();
    }
}
