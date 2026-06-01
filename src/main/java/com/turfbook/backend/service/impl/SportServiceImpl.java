package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.CreateSportRequest;
import com.turfbook.backend.dto.SportDto;
import com.turfbook.backend.dto.UpdateSportRequest;
import com.turfbook.backend.entity.SportEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.mapper.SportMapper;
import com.turfbook.backend.repository.SportRepository;
import com.turfbook.backend.service.SportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SportServiceImpl implements SportService {

    private final SportRepository sportRepository;
    private final SportMapper sportMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SportDto> listSports() {
        return sportRepository.findAll().stream().map(sportMapper::toDto).toList();
    }

    @Override
    @Transactional
    public SportDto createSport(CreateSportRequest request) {
        if (sportRepository.existsByName(request.getName())) {
            throw new ConflictException("Sport already exists: " + request.getName());
        }
        SportEntity sport = SportEntity.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .build();
        return sportMapper.toDto(sportRepository.save(sport));
    }

    @Override
    @Transactional
    public SportDto updateSport(Long id, UpdateSportRequest request) {
        SportEntity sport = sportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sport", "id", id));
        if (StringUtils.hasText(request.getName())) {
            sport.setName(request.getName());
        }
        if (StringUtils.hasText(request.getIcon())) {
            sport.setIcon(request.getIcon());
        }
        return sportMapper.toDto(sportRepository.save(sport));
    }

    @Override
    @Transactional
    public void deleteSport(Long id) {
        SportEntity sport = sportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sport", "id", id));
        sportRepository.delete(sport);
    }
}
