import React, { useMemo } from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import {
  ClassicEditor,
  Essentials,
  Paragraph,
  Heading,
  Bold,
  Italic,
  Underline,
  Strikethrough,
  FontColor,
  FontBackgroundColor,
  List,
  Link,
  Table,
  TableToolbar,
  BlockQuote,
  Undo,
  // CKEditor 5 Image & Upload Plugins
  Image,
  ImageToolbar,
  ImageCaption,
  ImageStyle,
  ImageResize,
  ImageUpload,
  FileRepository
} from 'ckeditor5';
import translations from 'ckeditor5/translations/vi.js';
import 'ckeditor5/ckeditor5.css';
import { minioUploadAdapterPlugin } from '../utils/MinioUploadAdapter';

/**
 * Component trình soạn thảo mô tả chi tiết sản phẩm chuẩn CKEditor 5
 * Tối ưu cho sàn thương mại điện tử QuickShop:
 * - Sử dụng kiến trúc hiện đại từ gói 'ckeditor5' (Loại bỏ Classic Build cũ)
 * - Tích hợp gói ngôn ngữ Tiếng Việt chính thức
 * - Hỗ trợ Upload ảnh nội tuyến trực tiếp lên MinIO Object Storage
 * - Cung cấp đầy đủ toolbar: Định dạng chữ, màu sắc, danh sách, bảng biểu, liên kết, trích dẫn, ảnh nội tuyến
 * - Giấy phép mã nguồn mở (GPL) theo đúng chính sách phiên bản mới của CKEditor
 * - Chiều cao tối thiểu 300px, giao diện bo góc hiện đại hài hòa với hệ thống
 */
const ProductDescriptionEditor = ({
  value = '',
  onChange,
  placeholder = 'Nhập mô tả chi tiết về sản phẩm, chất liệu, tính năng, hướng dẫn sử dụng...',
  disabled = false,
  className = ''
}) => {
  // Cấu hình Editor tối ưu hiệu năng với useMemo
  const editorConfig = useMemo(() => {
    return {
      licenseKey: 'GPL',
      language: 'vi',
      translations: [translations],
      placeholder: placeholder,
      extraPlugins: [minioUploadAdapterPlugin],
      plugins: [
        Essentials,
        Paragraph,
        Heading,
        Bold,
        Italic,
        Underline,
        Strikethrough,
        FontColor,
        FontBackgroundColor,
        List,
        Link,
        Table,
        TableToolbar,
        BlockQuote,
        Undo,
        // Image & Upload plugins
        Image,
        ImageToolbar,
        ImageCaption,
        ImageStyle,
        ImageResize,
        ImageUpload,
        FileRepository
      ],
      toolbar: {
        items: [
          'heading',
          '|',
          'bold',
          'italic',
          'underline',
          'strikethrough',
          '|',
          'fontColor',
          'fontBackgroundColor',
          '|',
          'bulletedList',
          'numberedList',
          '|',
          'uploadImage', // Nút chọn ảnh từ máy tính tải lên MinIO
          'link',
          'insertTable',
          'blockQuote',
          '|',
          'undo',
          'redo'
        ],
        shouldNotGroupWhenFull: false // Tự động thu gọn thanh công cụ trên màn hình nhỏ/responsive
      },
      heading: {
        options: [
          { model: 'paragraph', title: 'Đoạn văn', class: 'ck-heading_paragraph' },
          { model: 'heading1', view: 'h1', title: 'Tiêu đề lớn (H1)', class: 'ck-heading_heading1' },
          { model: 'heading2', view: 'h2', title: 'Tiêu đề vừa (H2)', class: 'ck-heading_heading2' },
          { model: 'heading3', view: 'h3', title: 'Tiêu đề nhỏ (H3)', class: 'ck-heading_heading3' }
        ]
      },
      image: {
        toolbar: [
          'imageStyle:inline',
          'imageStyle:block',
          'imageStyle:side',
          '|',
          'toggleImageCaption',
          'imageTextAlternative',
          '|',
          'resizeImage'
        ],
        resizeOptions: [
          {
            name: 'resizeImage:original',
            value: null,
            label: 'Kích thước gốc'
          },
          {
            name: 'resizeImage:25',
            value: '25',
            label: '25%'
          },
          {
            name: 'resizeImage:50',
            value: '50',
            label: '50%'
          },
          {
            name: 'resizeImage:75',
            value: '75',
            label: '75%'
          }
        ]
      },
      table: {
        contentToolbar: [
          'tableColumn',
          'tableRow',
          'mergeTableCells'
        ]
      },
      link: {
        addTargetToExternalLinks: true,
        defaultProtocol: 'https://'
      }
    };
  }, [placeholder]);

  return (
    <div className={`quickshop-ckeditor-container w-full ${className}`}>
      <div className="quickshop-ckeditor-shell">
        <CKEditor
          editor={ClassicEditor}
          config={editorConfig}
          data={value || ''}
          disabled={disabled}
          onChange={(event, editor) => {
            const data = editor.getData();
            if (onChange) {
              onChange(data);
            }
          }}
        />
      </div>
    </div>
  );
};

export default React.memo(ProductDescriptionEditor);
