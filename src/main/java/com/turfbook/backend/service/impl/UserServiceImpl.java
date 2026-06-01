package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.UpdateProfileRequest;
import com.turfbook.backend.dto.UserDto;
import com.turfbook.backend.dto.UserPage;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.mapper.UserMapper;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDto getMe(Long userId) {
        return userMapper.toDto(getEntityById(userId));
    }

    @Override
    @Transactional
    public UserDto updateMe(Long userId, UpdateProfileRequest request) {
        UserEntity user = getEntityById(userId);

        if (StringUtils.hasText(request.getName())) {
            user.setName(request.getName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPreferredSports() != null) {
            user.setPreferredSports(request.getPreferredSports());
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserPage listUsers(int page, int size, String role, String search) {
        Pageable pageable = PageRequest.of(page, size);
        UserEntity.Role roleEnum = null;
        if (StringUtils.hasText(role)) {
            try {
                roleEnum = UserEntity.Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        String searchParam = StringUtils.hasText(search) ? search : null;
        Page<UserEntity> entityPage = userRepository.findAllByRoleAndSearch(roleEnum, searchParam, pageable);

        UserPage dto = new UserPage();
        dto.setContent(entityPage.getContent().stream().map(userMapper::toDto).toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }

    @Override
    @Transactional
    public UserDto blockUser(Long id) {
        UserEntity user = getEntityById(id);
        user.setBlocked(true);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto unblockUser(Long id) {
        UserEntity user = getEntityById(id);
        user.setBlocked(false);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
