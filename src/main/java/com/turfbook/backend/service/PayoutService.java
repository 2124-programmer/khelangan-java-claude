package com.turfbook.backend.service;

import com.turfbook.backend.dto.PayoutDto;
import com.turfbook.backend.dto.PayoutPage;

public interface PayoutService {

    PayoutPage listOwnerPayouts(Long ownerId, int page, int size);

    PayoutPage adminListPayouts(int page, int size, String status);

    PayoutDto processPayout(Long id);
}
