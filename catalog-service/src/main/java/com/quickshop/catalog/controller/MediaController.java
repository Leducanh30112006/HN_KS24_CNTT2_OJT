package com.quickshop.catalog.controller;

import com.quickshop.catalog.dto.FileUploadResponse;
import com.quickshop.catalog.service.MinioStorageService;
import com.quickshop.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "MinIO Media Storage API", description = "Upload & Streaming ảnh sản phẩm trên MinIO S3 Object Storage; kiểm tra loại file và kích thước")
public class MediaController {

    private final MinioStorageService minioStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload một hình ảnh sản phẩm lên MinIO Object Storage (Kiểm tra định dạng và kích thước file)")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadProductImage(
            @RequestParam("file") MultipartFile file
    ) {
        FileUploadResponse response = minioStorageService.uploadProductImage(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Upload ảnh lên MinIO thành công", response));
    }

    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload nhiều hình ảnh sản phẩm cùng lúc lên MinIO Object Storage")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files
    ) {
        List<FileUploadResponse> responses = minioStorageService.uploadMultipleImages(files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Upload danh sách ảnh lên MinIO thành công (" + responses.size() + " files)", responses));
    }

    @GetMapping("/media/{fileName}")
    @Operation(summary = "Truy xuất stream hình ảnh trực tiếp từ MinIO Server")
    public ResponseEntity<InputStreamResource> getMedia(@PathVariable String fileName) {
        try {
            InputStream stream = minioStorageService.getFileStream(fileName);
            String contentType = determineContentType(fileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(new InputStreamResource(stream));

        } catch (Exception e) {
            log.warn("Cannot stream media {} from MinIO: {}", fileName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/media/{fileName}")
    @Operation(summary = "Xóa file ảnh khỏi MinIO Object Storage")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable String fileName) {
        minioStorageService.deleteFile(fileName);
        return ResponseEntity.ok(ApiResponse.success("Xóa file ảnh trên MinIO thành công", null));
    }

    @GetMapping("/media/health")
    @Operation(summary = "Kiểm tra trạng thái kết nối MinIO Object Storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkMinioHealth() {
        boolean ready = minioStorageService.isReady();
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra trạng thái MinIO", Map.of(
                "status", ready ? "UP" : "DOWN",
                "ready", ready
        )));
    }

    private String determineContentType(String fileName) {
        if (fileName == null) return "image/jpeg";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }
}
