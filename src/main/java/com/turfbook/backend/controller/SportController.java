package com.turfbook.backend.controller;

import com.turfbook.backend.api.SportsApi;
import com.turfbook.backend.dto.SportDto;
import com.turfbook.backend.service.SportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SportController implements SportsApi {

    private final SportService sportService;

    @Override
    public ResponseEntity<List<SportDto>> listSports() {
        return ResponseEntity.ok(sportService.listSports());
    }
}
