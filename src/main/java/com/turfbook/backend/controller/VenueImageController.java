package com.turfbook.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Standalone (non-generated) controller for venue image uploads.
 * Images are stored under {upload.dir}/venue-images/ and served via
 * StorageConfig's static resource handler at /uploads/venue-images/{file}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueImageController {

    private static final long MAX_BYTES = 350_000;          // 350 KB upper guard
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://192.168.1.5:8080}")
    private String baseUrl;

    /**
     * POST /api/v1/venues/images/upload
     * Accepts a single JPEG/PNG/WebP file, validates content-type and size,
     * stores it under uploads/venue-images/, returns { "url": "..." }.
     */
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        validateImage(file);

        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;

        Path dir  = Paths.get(uploadDir, "venue-images");
        Files.createDirectories(dir);
        Path dest = dir.resolve(filename);
        try (var in = file.getInputStream()) {
            Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        String url = baseUrl + "/uploads/venue-images/" + filename;
        log.info("VenueImageController: stored image {} → {}", filename, dest);
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
