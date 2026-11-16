import axiosClient from '../api/axiosClient.js';

/**
 * Danh sách định dạng ảnh hợp lệ đồng bộ với backend MinioStorageService
 */
const ALLOWED_MIME_TYPES = [
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/webp',
  'image/gif'
];

/**
 * Giới hạn dung lượng ảnh: 5MB (đồng bộ với minio.max-file-size của backend)
 */
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

/**
 * Custom Upload Adapter cho CKEditor 5 kết nối với MinIO Object Storage
 * thông qua API POST /api/catalog/upload có sẵn của Catalog Service.
 */
export class MinioUploadAdapter {
  constructor(loader) {
    this.loader = loader;
    this.abortController = new AbortController();
  }

  /**
   * Bắt đầu tiến trình upload file lên MinIO
   * @returns {Promise<{default: string}>}
   */
  async upload() {
    const file = await this.loader.file;

    // 1. Kiểm tra tính hợp lệ của file ở client trước khi gửi (Client-side validation)
    if (!file) {
      throw new Error('Không tìm thấy tệp tin hình ảnh');
    }

    const fileType = (file.type || '').toLowerCase();
    const isMimeValid = ALLOWED_MIME_TYPES.includes(fileType);
    const fileName = (file.name || '').toLowerCase();
    const isExtValid = /\.(jpe?g|png|webp|gif)$/i.test(fileName);

    if (!isMimeValid && !isExtValid) {
      throw new Error(
        `Định dạng tệp "${file.type || fileName}" không hợp lệ. Chỉ chấp nhận các định dạng ảnh: JPG, PNG, WEBP, GIF.`
      );
    }

    if (file.size > MAX_FILE_SIZE_BYTES) {
      const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
      throw new Error(
        `Dung lượng ảnh (${sizeMb}MB) vượt quá giới hạn cho phép (tối đa 5MB). Vui lòng chọn ảnh dung lượng nhỏ hơn.`
      );
    }

    // 2. Đóng gói Multipart form-data
    const formData = new FormData();
    formData.append('file', file);

    try {
      // 3. Gửi request upload tới MinIO thông qua axiosClient (tự động đính kèm Token và ShopId)
      const res = await axiosClient.post('/api/catalog/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        signal: this.abortController.signal,
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            this.loader.uploadTotal = progressEvent.total;
            this.loader.uploaded = progressEvent.loaded;
          }
        }
      });

      // 4. Bóc tách Public URL hoặc Stream URL từ phản hồi của MinIO
      // Spring Boot ApiResponse format: { success: true, message: '...', data: { url, streamUrl, ... } }
      const data = res?.data || res;
      const imageUrl = data?.url || data?.streamUrl;

      if (!imageUrl) {
        throw new Error('Máy chủ không trả về đường dẫn hình ảnh hợp lệ');
      }

      // Trả về định dạng CKEditor yêu cầu: { default: imageUrl }
      return {
        default: imageUrl
      };
    } catch (error) {
      // Bắt lỗi hủy bỏ request
      if (
        error?.name === 'CanceledError' ||
        error?.name === 'AbortError' ||
        this.abortController.signal.aborted
      ) {
        throw new Error('Tiến trình tải ảnh lên đã bị hủy bỏ.');
      }

      // Bắt thông điệp lỗi chi tiết từ backend ApiResponse
      const serverMessage =
        error?.response?.data?.message ||
        error?.message ||
        'Tải ảnh lên máy chủ MinIO thất bại. Vui lòng kiểm tra kết nối mạng.';

      throw new Error(serverMessage);
    }
  }

  /**
   * Hủy tiến trình upload khi người dùng bấm Hủy hoặc xóa ảnh đang tải dở
   */
  abort() {
    this.abortController.abort();
  }
}

/**
 * Plugin CKEditor để đăng ký MinioUploadAdapter vào FileRepository
 */
export function minioUploadAdapterPlugin(editor) {
  editor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
    return new MinioUploadAdapter(loader);
  };
}
