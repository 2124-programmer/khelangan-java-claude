package com.turfbook.backend.controller;

import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles user profile avatar uploads for any authenticated user.
 * Stored under {upload.dir}/user-avatars/ and served via the same
 * static resource handler as venue images (StorageConfig /uploads/**).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserAvatarController {

    private static final long MAX_BYTES = 350_000;
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final UserRepository userRepository;

    /**
     * POST /api/v1/users/me/avatar
     * Accepts a single image, stores it, updates the user's avatarUrl in the DB,
     * and returns { "url": "..." }.
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file) throws IOException {

        validateImage(file);

        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;

        Path dir  = Paths.get(uploadDir, "user-avatars");
        Files.createDirectories(dir);
        Path dest = dir.resolve(filename);
        try (var in = file.getInputStream()) {
            Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        String url = baseUrl + "/uploads/user-avatars/" + filename;

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAvatarUrl(url);
        userRepository.save(user);

        log.info("UserAvatarController: stored avatar {} for userId={}", filename, principal.getId());
        return ResponseEntity.ok(Map.of("url", url));
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported image type: " + ct + ". Allowed: jpeg, png, webp");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "Image too large: " + file.getSize() + " bytes (max " + MAX_BYTES + ")");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }
}
