package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.FileUploadResponse;
import com.quickshop.common.exception.BadRequestException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinioStorageService - Tích hợp MinIO upload ảnh sản phẩm, kiểm tra loại file/kích thước và trả public URL")
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MinioStorageService minioStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(minioStorageService, "bucketName", "quickshop-products");
        ReflectionTestUtils.setField(minioStorageService, "minioEndpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(minioStorageService, "minioPublicUrl", "http://localhost:9000");
        ReflectionTestUtils.setField(minioStorageService, "maxFileSizeBytes", 5242880L); // 5MB
    }

    @Nested
    @DisplayName("1. Upload hình ảnh hợp lệ (JPEG, PNG, WEBP, GIF)")
    class ValidImageUploadTests {

        @Test
        @DisplayName("Upload ảnh JPEG hợp lệ thành công và trả về Public URL & Stream URL")
        void uploadProductImage_Jpeg_Success() throws Exception {
            byte[] content = "fake-jpeg-image-bytes".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "product-photo.jpg",
                    "image/jpeg",
                    content
            );

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            FileUploadResponse response = minioStorageService.uploadProductImage(file);

            assertThat(response).isNotNull();
            assertThat(response.getOriginalFilename()).isEqualTo("product-photo.jpg");
            assertThat(response.getContentType()).isEqualTo("image/jpeg");
            assertThat(response.getSize()).isEqualTo(content.length);
            assertThat(response.getBucket()).isEqualTo("quickshop-products");
            assertThat(response.getFileName()).endsWith(".jpg");
            assertThat(response.getUrl()).startsWith("http://localhost:9000/quickshop-products/");
            assertThat(response.getStreamUrl()).startsWith("/api/catalog/media/");

            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Upload ảnh PNG hợp lệ thành công")
        void uploadProductImage_Png_Success() throws Exception {
            byte[] content = "fake-png-image-bytes".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "banner.png",
                    "image/png",
                    content
            );

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            FileUploadResponse response = minioStorageService.uploadProductImage(file);

            assertThat(response).isNotNull();
            assertThat(response.getOriginalFilename()).isEqualTo("banner.png");
            assertThat(response.getContentType()).isEqualTo("image/png");
            assertThat(response.getFileName()).endsWith(".png");
        }

        @Test
        @DisplayName("Upload ảnh WebP hợp lệ thành công")
        void uploadProductImage_Webp_Success() throws Exception {
            byte[] content = "fake-webp-image-bytes".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "thumb.webp",
                    "image/webp",
                    content
            );

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            FileUploadResponse response = minioStorageService.uploadProductImage(file);

            assertThat(response).isNotNull();
            assertThat(response.getContentType()).isEqualTo("image/webp");
            assertThat(response.getFileName()).endsWith(".webp");
        }

        @Test
        @DisplayName("Upload nhiều ảnh cùng lúc thành công")
        void uploadMultipleImages_Success() throws Exception {
            MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", "bytes1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("files", "img2.png", "image/png", "bytes2".getBytes());

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            List<FileUploadResponse> responses = minioStorageService.uploadMultipleImages(new MultipartFile[]{file1, file2});

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getOriginalFilename()).isEqualTo("img1.jpg");
            assertThat(responses.get(1).getOriginalFilename()).isEqualTo("img2.png");
            verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
        }
    }

    @Nested
    @DisplayName("2. Kiểm tra tính hợp lệ (Validation: File rỗng, kích thước, định dạng)")
    class FileValidationTests {

        @Test
        @DisplayName("Ném BadRequestException khi file upload bị rỗng")
        void uploadProductImage_ThrowsWhenEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.jpg",
                    "image/jpeg",
                    new byte[0]
            );

            assertThatThrownBy(() -> minioStorageService.uploadProductImage(emptyFile))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("File tải lên không được để trống");
        }

        @Test
        @DisplayName("Ném BadRequestException khi kích thước file vượt quá 5MB")
        void uploadProductImage_ThrowsWhenSizeExceedsLimit() {
            byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB (vượt quá 5MB)
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large-image.jpg",
                    "image/jpeg",
                    largeContent
            );

            assertThatThrownBy(() -> minioStorageService.uploadProductImage(largeFile))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Kích thước file vượt quá giới hạn tối đa cho phép (5MB)");
        }

        @Test
        @DisplayName("Ném BadRequestException khi MIME type không phải định dạng ảnh (ví dụ: PDF)")
        void uploadProductImage_ThrowsWhenMimeTypeNotSupported() {
            MockMultipartFile pdfFile = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    "pdf-content".getBytes()
            );

            assertThatThrownBy(() -> minioStorageService.uploadProductImage(pdfFile))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Định dạng file không hợp lệ");
        }

        @Test
        @DisplayName("Ném BadRequestException khi đuôi mở rộng file độc hại (ví dụ: .exe giả mạo)")
        void uploadProductImage_ThrowsWhenExtensionNotSupported() {
            MockMultipartFile exeFile = new MockMultipartFile(
                    "file",
                    "malware.exe",
                    "image/jpeg", // Giả mạo MIME type
                    "exe-content".getBytes()
            );

            assertThatThrownBy(() -> minioStorageService.uploadProductImage(exeFile))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Định dạng file không hợp lệ");
        }

        @Test
        @DisplayName("Ném BadRequestException khi upload danh sách nhiều file bị rỗng")
        void uploadMultipleImages_ThrowsWhenArrayEmpty() {
            assertThatThrownBy(() -> minioStorageService.uploadMultipleImages(new MultipartFile[0]))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Danh sách file tải lên không được để trống");
        }
    }

    @Nested
    @DisplayName("3. Thao tác Streaming & Xóa file trên MinIO")
    class StreamingAndDeleteTests {

        @Test
        @DisplayName("Lấy InputStream của file từ MinIO thành công")
        void getFileStream_Success() throws Exception {
            GetObjectResponse mockResponse = mock(GetObjectResponse.class);
            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

            InputStream stream = minioStorageService.getFileStream("test-image.jpg");

            assertThat(stream).isNotNull();
            verify(minioClient).getObject(any(GetObjectArgs.class));
        }

        @Test
        @DisplayName("Ném BadRequestException khi tên file lấy stream bị rỗng")
        void getFileStream_ThrowsWhenFilenameBlank() {
            assertThatThrownBy(() -> minioStorageService.getFileStream("   "))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Tên file không hợp lệ");
        }

        @Test
        @DisplayName("Xóa file ảnh trên MinIO thành công")
        void deleteFile_Success() throws Exception {
            minioStorageService.deleteFile("delete-me.jpg");

            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Kiểm tra tính sẵn sàng isReady của MinIO thành công")
        void isReady_ReturnsTrue() throws Exception {
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            boolean ready = minioStorageService.isReady();

            assertThat(ready).isTrue();
        }
    }
}

