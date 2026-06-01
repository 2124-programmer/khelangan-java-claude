package com.turfbook.backend.service;

import com.turfbook.backend.dto.CreateSportRequest;
import com.turfbook.backend.dto.SportDto;
import com.turfbook.backend.dto.UpdateSportRequest;

import java.util.List;

public interface SportService {

    List<SportDto> listSports();

    SportDto createSport(CreateSportRequest request);

    SportDto updateSport(Long id, UpdateSportRequest request);

    void deleteSport(Long id);
}
