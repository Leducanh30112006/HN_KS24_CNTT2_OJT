package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.FileUploadResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:quickshop-products}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${minio.public-url:http://localhost:9000}")
    private String minioPublicUrl;

    @Value("${minio.max-file-size:5242880}")
    private long maxFileSizeBytes; // Mặc định 5MB (5 * 1024 * 1024)

    /**
     * Danh sách các MIME type hình ảnh được hỗ trợ hợp lệ
     */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    /**
     * Danh sách các đuôi mở rộng file hợp lệ
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp",
            ".gif"
    );

    @PostConstruct
    public void initBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                log.info("MinIO bucket '{}' does not exist. Creating bucket...", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());

                // Set public read policy for images
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [
                            {
                              "Effect": "Allow",
                              "Principal": "*",
                              "Action": ["s3:GetObject"],
                              "Resource": ["arn:aws:s3:::%s/*"]
                            }
                          ]
                        }
                        """.formatted(bucketName);

                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build()
                );
                log.info("MinIO bucket '{}' created successfully with public read policy.", bucketName);
            } else {
                log.info("MinIO bucket '{}' is ready.", bucketName);
            }
        } catch (Exception e) {
            log.warn("MinIO server not available or connection failed at startup ({}): {}", minioEndpoint, e.getMessage());
        }
    }

    /**
     * Upload một hình ảnh sản phẩm lên MinIO với đầy đủ kiểm tra định dạng và dung lượng
     *
     * @param file MultipartFile gửi lên từ client
     * @return FileUploadResponse chứa Public URL, Stream URL, tên file, dung lượng và định dạng
     */
    public FileUploadResponse uploadProductImage(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().trim() : "image.jpg";
        String extension = extractExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + extension.toLowerCase();
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "image/jpeg";

        try {
            ensureBucketExists();

            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(uniqueFileName)
                                .stream(is, file.getSize(), -1)
                                .contentType(contentType)
                                .build()
                );
            }

            log.info("Uploaded file '{}' (size: {} bytes, mime: {}) to MinIO bucket '{}' successfully",
                    uniqueFileName, file.getSize(), contentType, bucketName);

            String publicUrl = buildPublicUrl(uniqueFileName);
            String streamUrl = "/api/catalog/media/" + uniqueFileName;

            return FileUploadResponse.builder()
                    .url(publicUrl)
                    .streamUrl(streamUrl)
                    .fileName(uniqueFileName)
                    .originalFilename(originalFilename)
                    .contentType(contentType)
                    .size(file.getSize())
                    .bucket(bucketName)
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload image to MinIO: {}", e.getMessage());
            throw new IllegalStateException("Không thể tải ảnh lên MinIO Object Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Upload nhiều hình ảnh sản phẩm cùng lúc
     *
     * @param files Danh sách MultipartFile
     * @return Danh sách FileUploadResponse
     */
    public List<FileUploadResponse> uploadMultipleImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BadRequestException("Danh sách file tải lên không được để trống");
        }

        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(uploadProductImage(file));
        }
        return responses;
    }

    /**
     * Lấy InputStream của file từ MinIO để phục vụ streaming
     *
     * @param fileName Tên file cần lấy
     * @return InputStream dữ liệu file
     */
    public InputStream getFileStream(String fileName) throws Exception {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("Tên file không hợp lệ");
        }

        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }

    /**
     * Xóa file ảnh khỏi MinIO bucket
     *
     * @param fileName Tên file cần xóa
     */
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("Tên file cần xóa không hợp lệ");
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("Đã xóa file '{}' khỏi MinIO bucket '{}'", fileName, bucketName);
        } catch (Exception e) {
            log.error("Lỗi khi xóa file '{}' từ MinIO: {}", fileName, e.getMessage());
            throw new IllegalStateException("Không thể xóa file từ MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra file có tồn tại trên MinIO hay không
     *
     * @param fileName Tên file
     * @return true nếu tồn tại
     */
    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra tính sẵn sàng của MinIO Storage
     */
    public boolean isReady() {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra tính hợp lệ của file tải lên (không rỗng, đúng kích thước, đúng định dạng ảnh)
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File tải lên không được để trống");
        }

        // 1. Kiểm tra dung lượng file
        if (file.getSize() > maxFileSizeBytes) {
            long maxMb = maxFileSizeBytes / (1024 * 1024);
            throw new BadRequestException("Kích thước file vượt quá giới hạn tối đa cho phép (" + maxMb + "MB)");
        }

        // 2. Kiểm tra MIME Type và extension đuôi file
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        boolean mimeValid = contentType != null && ALLOWED_MIME_TYPES.contains(contentType.toLowerCase().trim());
        boolean extValid = extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase().trim());

        if (!mimeValid || !extValid) {
            throw new BadRequestException("Định dạng file không hợp lệ (" + contentType + "). Chỉ chấp nhận các định dạng ảnh: JPEG, JPG, PNG, WEBP, GIF");
        }
    }

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
    }

    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private String buildPublicUrl(String uniqueFileName) {
        String baseUrl = minioPublicUrl != null && !minioPublicUrl.isBlank() ? minioPublicUrl : minioEndpoint;
        baseUrl = baseUrl.replaceAll("/+$", "");
        return String.format("%s/%s/%s", baseUrl, bucketName, uniqueFileName);
    }
}
