package com.turfbook.backend.config;

import com.turfbook.backend.entity.SportEntity;
import com.turfbook.backend.repository.SportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the default set of sports on startup (mirrors AdminSeeder). Seed-once: if ANY sport already
 * exists the seeder does nothing, so it's safe across restarts and never re-adds a sport an admin
 * later renamed or deleted. IDs are DB-assigned (IDENTITY) — on a fresh DB the insert order below
 * yields 1..N.
 *
 * <p>The {@code icon} is the emoji the app renders directly (see the sports UI / mock data).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SportSeeder implements ApplicationRunner {

    private final SportRepository sportRepository;

    /** {name, icon-emoji} in seed order. */
    private static final List<String[]> DEFAULT_SPORTS = List.of(
            new String[]{"Cricket", "🏏"},       // 🏏
            new String[]{"Football", "⚽"},            // ⚽
            new String[]{"Badminton", "🏸"},     // 🏸
            new String[]{"8 Ball", "🎱"},        // 🎱
            new String[]{"Pickle ball", "🏓"}    // 🏓
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Seed-once: if any sport already exists, do nothing (respects admin edits/deletions).
        if (sportRepository.count() > 0) {
            log.info("SportSeeder: sports already present — skipping seed.");
            return;
        }
        for (String[] s : DEFAULT_SPORTS) {
            sportRepository.save(SportEntity.builder().name(s[0]).icon(s[1]).build());
        }
        log.info("SportSeeder: seeded {} default sport(s).", DEFAULT_SPORTS.size());
    }
}
