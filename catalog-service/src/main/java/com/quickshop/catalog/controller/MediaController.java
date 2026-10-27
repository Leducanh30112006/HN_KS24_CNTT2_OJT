package com.quickshop.catalog.controller;

import com.quickshop.catalog.service.MinioStorageService;
import com.quickshop.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "MinIO Media Storage", description = "Upload & Streaming ảnh sản phẩm trên MinIO S3 Object Storage")
public class MediaController {

    private final MinioStorageService minioStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload hình ảnh sản phẩm lên MinIO Object Storage (FR-04)")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(
            @RequestParam("file") MultipartFile file
    ) {
        String mediaUrl = minioStorageService.uploadProductImage(file);
        return ResponseEntity.ok(ApiResponse.success("Upload ảnh lên MinIO thành công", Map.of(
                "url", mediaUrl,
                "originalFilename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                "size", String.valueOf(file.getSize())
        )));
    }

    @GetMapping("/media/{fileName}")
    @Operation(summary = "Truy xuất stream hình ảnh trực tiếp từ MinIO Server")
    public ResponseEntity<InputStreamResource> getMedia(@PathVariable String fileName) {
        try {
            InputStream stream = minioStorageService.getFileStream(fileName);
            String contentType = "image/jpeg";
            if (fileName.endsWith(".png")) contentType = "image/png";
            else if (fileName.endsWith(".webp")) contentType = "image/webp";
            else if (fileName.endsWith(".gif")) contentType = "image/gif";
            else if (fileName.endsWith(".svg")) contentType = "image/svg+xml";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(new InputStreamResource(stream));

        } catch (Exception e) {
            log.warn("Cannot stream media {} from MinIO: {}", fileName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
