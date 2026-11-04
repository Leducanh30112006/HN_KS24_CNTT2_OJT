package com.quickshop.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.catalog.dto.FileUploadResponse;
import com.quickshop.catalog.exception.GlobalExceptionHandler;
import com.quickshop.catalog.service.MinioStorageService;
import com.quickshop.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MediaController - REST APIs Upload & Streaming ảnh sản phẩm trên MinIO Storage")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MinioStorageService minioStorageService;

    @Test
    @DisplayName("POST /api/catalog/upload - Upload 1 ảnh thành công trả về 201 Created kèm Public URL")
    void uploadProductImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "phone.png",
                "image/png",
                "fake-png-content".getBytes()
        );

        FileUploadResponse response = FileUploadResponse.builder()
                .url("http://localhost:9000/quickshop-products/abc-123.png")
                .streamUrl("/api/catalog/media/abc-123.png")
                .fileName("abc-123.png")
                .originalFilename("phone.png")
                .contentType("image/png")
                .size(15L)
                .bucket("quickshop-products")
                .build();

        when(minioStorageService.uploadProductImage(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/catalog/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("http://localhost:9000/quickshop-products/abc-123.png"))
                .andExpect(jsonPath("$.data.fileName").value("abc-123.png"))
                .andExpect(jsonPath("$.data.originalFilename").value("phone.png"));
    }

    @Test
    @DisplayName("POST /api/catalog/upload - 400 Bad Request khi upload file không hợp lệ hoặc quá kích thước")
    void uploadProductImage_ThrowsBadRequestWhenValidationFails() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        when(minioStorageService.uploadProductImage(any()))
                .thenThrow(new BadRequestException("Định dạng file không hợp lệ (application/pdf). Chỉ chấp nhận các định dạng ảnh: JPEG, JPG, PNG, WEBP, GIF"));

        mockMvc.perform(multipart("/api/catalog/upload").file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Định dạng file không hợp lệ (application/pdf). Chỉ chấp nhận các định dạng ảnh: JPEG, JPG, PNG, WEBP, GIF"));
    }

    @Test
    @DisplayName("POST /api/catalog/upload/multiple - Upload nhiều ảnh cùng lúc thành công")
    void uploadMultipleImages_Success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", "content2".getBytes());

        FileUploadResponse resp1 = FileUploadResponse.builder().url("url1").originalFilename("img1.jpg").build();
        FileUploadResponse resp2 = FileUploadResponse.builder().url("url2").originalFilename("img2.jpg").build();

        when(minioStorageService.uploadMultipleImages(any())).thenReturn(List.of(resp1, resp2));

        mockMvc.perform(multipart("/api/catalog/upload/multiple").file(file1).file(file2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].originalFilename").value("img1.jpg"))
                .andExpect(jsonPath("$.data[1].originalFilename").value("img2.jpg"));
    }

    @Test
    @DisplayName("GET /api/catalog/media/{fileName} - Streaming ảnh thành công từ MinIO với đúng Content-Type")
    void getMedia_Success() throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream("image-binary-stream".getBytes());
        when(minioStorageService.getFileStream("sample.png")).thenReturn(is);

        mockMvc.perform(get("/api/catalog/media/sample.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", "public, max-age=86400"));
    }

    @Test
    @DisplayName("GET /api/catalog/media/{fileName} - 404 Not Found khi file không tồn tại trên MinIO")
    void getMedia_NotFound() throws Exception {
        when(minioStorageService.getFileStream("notfound.jpg")).thenThrow(new RuntimeException("Object not found"));

        mockMvc.perform(get("/api/catalog/media/notfound.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/catalog/media/{fileName} - Xóa ảnh thành công trên MinIO")
    void deleteMedia_Success() throws Exception {
        mockMvc.perform(delete("/api/catalog/media/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa file ảnh trên MinIO thành công"));
    }

    @Test
    @DisplayName("GET /api/catalog/media/health - Kiểm tra trạng thái MinIO Server")
    void checkMinioHealth_Success() throws Exception {
        when(minioStorageService.isReady()).thenReturn(true);

        mockMvc.perform(get("/api/catalog/media/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.ready").value(true));
    }
}

