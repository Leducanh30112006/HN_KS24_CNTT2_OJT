package com.quickshop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO phản hồi thông tin sau khi upload file ảnh lên MinIO Object Storage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse implements Serializable {
    /**
     * URL truy cập công khai trực tiếp (Public URL) từ MinIO Server
     */
    private String url;

    /**
     * URL streaming nội bộ qua API Gateway / Catalog Service
     */
    private String streamUrl;

    /**
     * Tên file duy nhất được lưu trữ trên MinIO (UUID + extension)
     */
    private String fileName;

    /**
     * Tên file gốc ban đầu của người dùng tải lên
     */
    private String originalFilename;

    /**
     * Định dạng MIME type của ảnh (ví dụ: image/jpeg, image/png, image/webp)
     */
    private String contentType;

    /**
     * Kích thước file tính theo bytes
     */
    private Long size;

    /**
     * Tên bucket lưu trữ trên MinIO
     */
    private String bucket;
}

