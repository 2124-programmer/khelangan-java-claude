package com.turfbook.backend.service;

import com.turfbook.backend.dto.CreateDisputeRequest;
import com.turfbook.backend.dto.DisputeDto;
import com.turfbook.backend.dto.DisputePage;
import com.turfbook.backend.dto.ResolveDisputeRequest;
import com.turfbook.backend.entity.UserEntity;

public interface DisputeService {

    DisputePage listDisputes(UserEntity currentUser, int page, int size);

    DisputeDto createDispute(Long playerId, CreateDisputeRequest request);

    DisputeDto resolveDispute(Long id, ResolveDisputeRequest request);
}
