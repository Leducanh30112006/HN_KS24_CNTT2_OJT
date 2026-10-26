package com.quickshop.catalog.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:quickshop-products}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

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

    public String uploadProductImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được rỗng");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (extension.isBlank()) {
            extension = ".jpg";
        }

        String uniqueFileName = UUID.randomUUID() + extension;
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

        try {
            // Ensure bucket exists
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

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

            log.info("Uploaded file '{}' to MinIO bucket '{}' successfully", uniqueFileName, bucketName);
            // Return gateway/internal streaming URL for best compatibility
            return "/api/catalog/media/" + uniqueFileName;

        } catch (Exception e) {
            log.error("Failed to upload image to MinIO: {}", e.getMessage());
            // Fallback placeholder image
            return "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80";
        }
    }

    public InputStream getFileStream(String fileName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }
}
