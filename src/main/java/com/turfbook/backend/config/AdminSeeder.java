package com.turfbook.backend.config;

import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Idempotent bootstrap of the first SUPER_ADMIN account.
 *
 * <p>Runs on every startup and is safe to re-run (ddl-auto: update, restarts, multiple deploys):
 * if any SUPER_ADMIN already exists it does nothing. Otherwise it creates one super-admin from
 * env-driven credentials and flags it {@code mustChangePassword = true} so the operator is forced
 * to set a real password on first login (enforced server-side in JwtAuthenticationFilter).
 *
 * <p>Credentials come only from configuration ({@code app.bootstrap.admin.email/password}, backed
 * by env vars) — never hardcoded, never logged. If they're absent and no super-admin exists, it
 * logs a clear warning and skips rather than creating a default/weak account or crashing the app.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.email:}")
    private String bootstrapEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Idempotent: a super-admin already exists → nothing to do, no side effects.
        if (userRepository.existsByRoleAndAdminRole(UserEntity.Role.ADMIN, UserEntity.AdminRole.SUPER_ADMIN)) {
            log.info("AdminSeeder: a SUPER_ADMIN already exists — skipping bootstrap.");
            return;
        }

        String email = bootstrapEmail == null ? "" : bootstrapEmail.trim().toLowerCase();
        String password = bootstrapPassword == null ? "" : bootstrapPassword;

        if (email.isEmpty() || password.isEmpty()) {
            log.warn("AdminSeeder: no SUPER_ADMIN exists and BOOTSTRAP_ADMIN_EMAIL/BOOTSTRAP_ADMIN_PASSWORD "
                    + "are not set — skipping bootstrap. Set both env vars and restart to create the first "
                    + "super-admin. (No default account is created.)");
            return;
        }

        // Guard the active_email unique index: if the address is already taken by a non-admin,
        // don't blow up the boot — surface it and skip so the operator can pick another email.
        if (userRepository.existsByActiveEmail(email)) {
            log.warn("AdminSeeder: BOOTSTRAP_ADMIN_EMAIL is already in use by an existing account — "
                    + "skipping bootstrap. Use a fresh email or promote that user to SUPER_ADMIN manually.");
            return;
        }

        UserEntity admin = UserEntity.builder()
                .name("Super Admin")
                .email(email)
                .activeEmail(email)
                // Phone is NOT NULL but not required for an admin; active_phone stays NULL (no unique collision).
                .phone("")
                .passwordHash(passwordEncoder.encode(password))
                .role(UserEntity.Role.ADMIN)
                .adminRole(UserEntity.AdminRole.SUPER_ADMIN)
                .status(UserEntity.AccountStatus.ACTIVE)
                .isBlocked(false)
                .mustChangePassword(true) // forced change on first login
                .acceptedTermsAt(LocalDateTime.now())
                .build();

        admin = userRepository.save(admin);
        // Never log the raw password.
        log.info("AdminSeeder: bootstrapped initial SUPER_ADMIN (id={}, email={}). "
                + "A password change is REQUIRED on first login.", admin.getId(), email);
    }
}
