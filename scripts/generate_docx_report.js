const fs = require('fs');
const path = require('path');
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  Table,
  TableRow,
  TableCell,
  WidthType,
  AlignmentType,
  HeadingLevel,
  ImageRun,
  BorderStyle,
  ShadingType
} = require('docx');

const imagesDir = path.resolve(__dirname, '../docs/images');

function readImg(name) {
  const p = path.join(imagesDir, name);
  if (fs.existsSync(p)) {
    return fs.readFileSync(p);
  }
  console.warn('Image not found:', name);
  return null;
}

// Helpers for styling
const borderNone = {
  top: { style: BorderStyle.NONE },
  bottom: { style: BorderStyle.NONE },
  left: { style: BorderStyle.NONE },
  right: { style: BorderStyle.NONE }
};

const borderThin = {
  top: { style: BorderStyle.SINGLE, size: 4, color: "CCCCCC" },
  bottom: { style: BorderStyle.SINGLE, size: 4, color: "CCCCCC" },
  left: { style: BorderStyle.SINGLE, size: 4, color: "CCCCCC" },
  right: { style: BorderStyle.SINGLE, size: 4, color: "CCCCCC" }
};

function createCell(text, bold = false, align = AlignmentType.LEFT, bg = null, widthPercent = null) {
  const cellProps = {
    borders: borderThin,
    children: [
      new Paragraph({
        alignment: align,
        spacing: { before: 80, after: 80 },
        children: [
          new TextRun({
            text: text,
            bold: bold,
            font: "Times New Roman",
            size: 22
          })
        ]
      })
    ]
  };
  if (bg) {
    cellProps.shading = { fill: bg, type: ShadingType.CLEAR };
  }
  if (widthPercent) {
    cellProps.width = { size: widthPercent, type: WidthType.PERCENTAGE };
  }
  return new TableCell(cellProps);
}

function pHeading1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 280, after: 140 },
    children: [
      new TextRun({
        text: text,
        bold: true,
        size: 30,
        font: "Times New Roman",
        color: "003366"
      })
    ]
  });
}

function pHeading2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 200, after: 100 },
    children: [
      new TextRun({
        text: text,
        bold: true,
        size: 26,
        font: "Times New Roman",
        color: "005599"
      })
    ]
  });
}

function pHeading3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 160, after: 80 },
    children: [
      new TextRun({
        text: text,
        bold: true,
        size: 24,
        font: "Times New Roman",
        color: "222222"
      })
    ]
  });
}

function pBody(text, bold = false, italic = false) {
  return new Paragraph({
    spacing: { before: 60, after: 60, line: 276 }, // 1.15 line spacing
    alignment: AlignmentType.JUSTIFIED,
    children: [
      new TextRun({
        text: text,
        bold: bold,
        italics: italic,
        font: "Times New Roman",
        size: 24
      })
    ]
  });
}

function pImage(buf, width = 580, height = 340, caption = "") {
  if (!buf) return new Paragraph({});
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 120, after: 60 },
    children: [
      new ImageRun({
        data: buf,
        transformation: { width, height }
      })
    ]
  });
}

function pCaption(text) {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 40, after: 140 },
    children: [
      new TextRun({
        text: text,
        italics: true,
        bold: true,
        font: "Times New Roman",
        size: 20,
        color: "555555"
      })
    ]
  });
}

function pCallout(title, text) {
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: [
      new TableRow({
        children: [
          new TableCell({
            borders: {
              left: { style: BorderStyle.SINGLE, size: 24, color: "005599" },
              top: { style: BorderStyle.NONE },
              right: { style: BorderStyle.NONE },
              bottom: { style: BorderStyle.NONE }
            },
            shading: { fill: "F0F4F8", type: ShadingType.CLEAR },
            children: [
              new Paragraph({
                spacing: { before: 60, after: 40 },
                children: [
                  new TextRun({
                    text: title,
                    bold: true,
                    font: "Times New Roman",
                    size: 22,
                    color: "003366"
                  })
                ]
              }),
              new Paragraph({
                spacing: { before: 0, after: 60, line: 260 },
                alignment: AlignmentType.JUSTIFIED,
                children: [
                  new TextRun({
                    text: text,
                    font: "Times New Roman",
                    size: 22,
                    color: "333333"
                  })
                ]
              })
            ]
          })
        ]
      })
    ]
  });
}

// Build the complete document
async function buildDoc() {
  console.log('Building document elements...');

  const children = [];

  // ==========================================
  // TRANG BÌA (COVER PAGE)
  // ==========================================
  children.push(
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 200, after: 60 },
      children: [
        new TextRun({ text: "BỘ GIÁO DỤC VÀ ĐÀO TẠO", bold: true, size: 26, font: "Times New Roman" })
      ]
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 0, after: 60 },
      children: [
        new TextRun({ text: "HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG", bold: true, size: 28, font: "Times New Roman", color: "990000" })
      ]
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 0, after: 200 },
      children: [
        new TextRun({ text: "KHOA CÔNG NGHỆ THÔNG TIN", bold: true, size: 26, font: "Times New Roman" })
      ]
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 0, after: 400 },
      children: [
        new TextRun({ text: "─────────────── ★ ───────────────", bold: true, size: 22, font: "Times New Roman", color: "888888" })
      ]
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 200, after: 120 },
      children: [
        new TextRun({ text: "BÁO CÁO DỰ ÁN THỰC TẬP DOANH NGHIỆP (OJT)", bold: true, size: 36, font: "Times New Roman", color: "003366" })
      ]
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 0, after: 400 },
      children: [
        new TextRun({ text: "(Áp dụng quy trình phát triển phần mềm Agile / Scrum – 04 Sprint)", italics: true, size: 26, font: "Times New Roman", color: "555555" })
      ]
    })
  );

  // Bordered Box for Project Title & Tech
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            new TableCell({
              borders: {
                top: { style: BorderStyle.SINGLE, size: 12, color: "003366" },
                bottom: { style: BorderStyle.SINGLE, size: 12, color: "003366" },
                left: { style: BorderStyle.SINGLE, size: 12, color: "003366" },
                right: { style: BorderStyle.SINGLE, size: 12, color: "003366" }
              },
              shading: { fill: "F8FAFC", type: ShadingType.CLEAR },
              children: [
                new Paragraph({
                  alignment: AlignmentType.CENTER,
                  spacing: { before: 140, after: 80 },
                  children: [
                    new TextRun({ text: "ĐỀ TÀI DỰ ÁN:", bold: true, size: 28, font: "Times New Roman", color: "003366" })
                  ]
                }),
                new Paragraph({
                  alignment: AlignmentType.CENTER,
                  spacing: { before: 0, after: 80 },
                  children: [
                    new TextRun({ text: "XÂY DỰNG SÀN THƯƠNG MẠI ĐIỆN TỬ ĐA NGƯỜI BÁN QUICKSHOP (MARKETPLACE EDITION)", bold: true, size: 28, font: "Times New Roman", color: "C0392B" })
                  ]
                }),
                new Paragraph({
                  alignment: AlignmentType.CENTER,
                  spacing: { before: 0, after: 140 },
                  children: [
                    new TextRun({ text: "Công nghệ áp dụng: Java 17 (Spring Boot 3.3 Microservices), React 18, MySQL 8.0, Redis 7, RabbitMQ, Docker Compose, PayOS VietQR, VNPay, Leaflet OpenStreetMap, Google Gemini AI", italics: true, size: 22, font: "Times New Roman", color: "334155" })
                  ]
                })
              ]
            })
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 300, after: 100 } })
  );

  // Table of Teachers & Students
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Giảng viên hướng dẫn:", true, AlignmentType.LEFT, "E2E8F0", 35),
            createCell("TS. Nguyễn Thị Hồng / ThS. Trần Văn Minh", false, AlignmentType.LEFT, null, 65)
          ]
        }),
        new TableRow({
          children: [
            createCell("Mentor doanh nghiệp:", true, AlignmentType.LEFT, "E2E8F0", 35),
            createCell("Tech Lead & Chuyên gia Phần mềm Doanh nghiệp OJT", false, AlignmentType.LEFT, null, 65)
          ]
        }),
        new TableRow({
          children: [
            createCell("Nhóm sinh viên thực hiện:", true, AlignmentType.LEFT, "E2E8F0", 35),
            createCell("Nhóm QuickShop (Team 03 - PTIT CNTT2)", true, AlignmentType.LEFT, null, 65)
          ]
        }),
        new TableRow({
          children: [
            createCell("1. Lê Đức Anh", false, AlignmentType.LEFT, null, 35),
            createCell("MSSV: B21DCCN003  |  Vai trò: Team Leader, DevOps & Security Lead, System Architect", false, AlignmentType.LEFT, null, 65)
          ]
        }),
        new TableRow({
          children: [
            createCell("2. Nguyễn Văn Kiên", false, AlignmentType.LEFT, null, 35),
            createCell("MSSV: B21DCCN045  |  Vai trò: Backend Lead, Saga Orchestration, Payment & Shipping", false, AlignmentType.LEFT, null, 65)
          ]
        }),
        new TableRow({
          children: [
            createCell("3. Triệu Quốc Bình", false, AlignmentType.LEFT, null, 35),
            createCell("MSSV: B21DCCN018  |  Vai trò: Frontend Lead, UI/UX Specialist & Multi-Portal Integration", false, AlignmentType.LEFT, null, 65)
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 400, after: 0 } }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      children: [
        new TextRun({ text: "HÀ NỘI – THÁNG 11 NĂM 2026", bold: true, size: 26, font: "Times New Roman" })
      ]
    }),
    new Paragraph({
      pageBreakBefore: true
    })
  );

  // ==========================================
  // HƯỚNG DẪN & LIÊN KẾT MINH CHỨNG
  // ==========================================
  children.push(
    pCallout(
      "HƯỚNG DẪN SỬ DỤNG BÁO CÁO DÀNH CHO HỘI ĐỒNG VÀ SINH VIÊN",
      "1. Báo cáo được thiết kế theo đúng quy chuẩn đồ án/thực tập tốt nghiệp OJT tại Học viện Công nghệ Bưu chính Viễn thông (PTIT), áp dụng quy trình phát triển phần mềm chuẩn Agile/Scrum gồm 04 Sprint (mỗi Sprint 01 tuần, 5 ngày làm việc từ Thứ Hai đến Thứ Sáu, nghỉ Thứ Bảy và Chủ Nhật). Dự án khởi động ngày 26/10/2026 và nghiệm thu bàn giao vào ngày 20/11/2026.\n2. Tất cả các hình ảnh minh chứng trong báo cáo được trích xuất trực tiếp từ sản phẩm thực tế đang vận hành trên Live Domain https://quickshop.io.vn và hệ thống cơ sở dữ liệu thật của nhóm."
    ),
    new Paragraph({ spacing: { before: 140, after: 100 } }),
    pHeading1("THÔNG TIN LIÊN KẾT MINH CHỨNG DỰ ÁN"),
    pBody("Hội đồng đánh giá và Giảng viên hướng dẫn có thể kiểm tra trực tiếp mã nguồn, tiến độ quản lý công việc và sản phẩm triển khai thực tế của nhóm thông qua các liên kết công khai dưới đây:")
  );

  // Proof links table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Hạng mục minh chứng", true, AlignmentType.CENTER, "D9E2EC", 35),
            createCell("Công cụ sử dụng", true, AlignmentType.CENTER, "D9E2EC", 25),
            createCell("Đường dẫn truy cập (Public URL)", true, AlignmentType.CENTER, "D9E2EC", 40)
          ]
        }),
        new TableRow({
          children: [
            createCell("Toàn bộ Mã nguồn Dự án (Backend & Frontend)", false, AlignmentType.LEFT),
            createCell("GitHub Repository", false, AlignmentType.CENTER),
            createCell("https://github.com/Leducanh30112006/HN_KS24_CNTT2_OJT", true, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("Quản lý Công việc & Sprint Board (60 Tasks)", false, AlignmentType.LEFT),
            createCell("Trello / GitHub Projects", false, AlignmentType.CENTER),
            createCell("QuickShop_Trello_Import_Updated.csv", true, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("Sản phẩm Triển khai Thực tế (Live Domain)", false, AlignmentType.LEFT),
            createCell("Cloudflare + Nginx SSL", false, AlignmentType.CENTER),
            createCell("https://quickshop.io.vn", true, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("Tài liệu API & Cổng Gateway Microservices", false, AlignmentType.LEFT),
            createCell("Spring Cloud Gateway", false, AlignmentType.CENTER),
            createCell("https://api.quickshop.io.vn (Port 8080)", true, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("Giám sát Hiệu năng & Tracing", false, AlignmentType.LEFT),
            createCell("Prometheus & Grafana", false, AlignmentType.CENTER),
            createCell("http://localhost:3000 (Grafana) / 9411 (Zipkin)", false, AlignmentType.LEFT)
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 180, after: 100 } })
  );

  // ==========================================
  // CHƯƠNG 1: TỔNG QUAN DỰ ÁN VÀ MÔI TRƯỜNG LÀM VIỆC
  // ==========================================
  children.push(
    pHeading1("CHƯƠNG 1: TỔNG QUAN DỰ ÁN VÀ MÔI TRƯỜNG LÀM VIỆC"),
    pHeading2("1.1. Giới thiệu bài toán và Mục tiêu dự án"),
    pBody("Trong bối cảnh nền kinh tế số và thương mại điện tử phát triển bùng nổ, mô hình Sàn thương mại điện tử Đa người bán (Multi-Vendor Marketplace) như Shopee, Lazada hay Tiki đã trở thành hạ tầng thiết yếu kết nối hàng triệu người mua với hàng trăm nghìn thương nhân độc lập. Tuy nhiên, việc vận hành một sàn đa gian hàng đặt ra những thách thức công nghệ vô cùng phức tạp: làm thế nào để xử lý giỏ hàng gom nhiều shop trong một lần thanh toán, điều phối giao dịch phân tán không gây sai lệch kho hàng, bảo vệ tiền người mua qua cơ chế ký quỹ (Escrow) và sổ cái kế toán kép, đồng thời tích hợp trí tuệ nhân tạo để nâng cao trải nghiệm mua sắm."),
    pBody("Dự án QuickShop (Marketplace Edition) được thực hiện trong thời gian 04 tuần thực tập OJT nhằm xây dựng một sàn thương mại điện tử chuẩn cấp độ Production dựa trên kiến trúc Cloud-Native Microservices hiện đại."),
    pBody("Mục tiêu cốt lõi của dự án:"),
    pBody("• Về mặt nghiệp vụ: Xây dựng trọn vẹn 4 phân hệ người dùng độc lập: Cổng Người Mua (Buyer Portal) với giao diện Bento Grid và bản đồ GPS Leaflet; Kênh Người Bán (Seller Center) với CKEditor 5 và thống kê SKU; Cổng Quản Trị (Admin Portal) duyệt KYC và điều hành Flash Sale AI; và Kênh Nhân viên Giao Hàng (Shipper Portal) tiếp nhận đơn và cập nhật vận trình."),
    pBody("• Về mặt kỹ thuật: Áp dụng kiến trúc Microservices phân tán với 1 API Gateway và 6 Backend Services độc lập (Database-per-Service); điều phối giao dịch phân tán qua Saga Orchestrator Pattern trên RabbitMQ; thanh toán trực tuyến qua cổng VNPay và quét mã PayOS VietQR EMVCo; tối ưu hóa truy xuất bằng Redis Caching và lưu trữ MinIO S3."),
    pBody("• Về mặt quy trình: Vận hành nhóm nghiêm ngặt theo mô hình Agile/Scrum qua đúng 4 Sprint (từ 26/10/2026 đến 20/11/2026), 20 ngày làm việc (chỉ làm việc từ Thứ Hai đến Thứ Sáu, nghỉ Thứ Bảy và Chủ Nhật), quản lý mã nguồn tập trung qua Git Flow với 389 commits."),

    pHeading2("1.2. Cơ cấu nhóm và Phân công vai trò"),
    pBody("Nhóm thực hiện dự án gồm 03 thành viên với chuyên môn hóa cao theo chuẩn quy trình phần mềm công nghiệp:")
  );

  // Team roles table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("STT", true, AlignmentType.CENTER, "D9E2EC", 8),
            createCell("Họ và Tên", true, AlignmentType.CENTER, "D9E2EC", 22),
            createCell("Vai trò (Role)", true, AlignmentType.CENTER, "D9E2EC", 25),
            createCell("Trách nhiệm chính trong dự án QuickShop", true, AlignmentType.CENTER, "D9E2EC", 45)
          ]
        }),
        new TableRow({
          children: [
            createCell("1", false, AlignmentType.CENTER),
            createCell("Lê Đức Anh", true, AlignmentType.LEFT),
            createCell("Team Leader, DevOps & Security Lead, System Architect", false, AlignmentType.LEFT),
            createCell("Lập kế hoạch 4 Sprint, thiết kế kiến trúc 1 Gateway + 6 Backend Services, triển khai Docker Compose, cấu hình Spring Cloud Gateway, JWT Dual Auth (Cookie/Bearer), Google OAuth 2.0, OTP Email Redis, trợ lý Google Gemini AI, CI/CD GitHub Actions và vá 18 tiêu chí an ninh QA.", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("2", false, AlignmentType.CENTER),
            createCell("Nguyễn Văn Kiên", true, AlignmentType.LEFT),
            createCell("Backend Lead, Saga Orchestrator, Payment & Shipping Architect", false, AlignmentType.LEFT),
            createCell("Thiết kế CSDL Database-per-Service, thư viện common-library, giỏ hàng phân tán Redis, Checkout tách Sub-Orders, Saga Orchestrator qua RabbitMQ, Dead Letter Queue (order.dlq), cổng thanh toán VNPay Sandbox & PayOS VietQR, Sổ cái kép Double-entry Ledger, Ký quỹ Escrow 72h và Shipping Webhook Idempotency.", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("3", false, AlignmentType.CENTER),
            createCell("Triệu Quốc Bình", true, AlignmentType.LEFT),
            createCell("Frontend Lead, UI/UX Specialist & Multi-Portal Integration", false, AlignmentType.LEFT),
            createCell("Catalog Service đa biến thể SKU, khóa lạc quan @Version, upload ảnh MinIO S3, xây dựng 4 phân hệ Frontend SPA (Buyer, Seller, Admin, Shipper), bản đồ số Leaflet OpenStreetMap GPS, hệ thống chat Full-page Messenger, CKEditor 5, quản lý Flash Sale AI, trải nghiệm 3D Scrollytelling (WebGL/GSAP/Lenis) và kiểm thử E2E đa thiết bị.", false, AlignmentType.LEFT)
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 140, after: 100 } }),

    pHeading2("1.3. Công nghệ sử dụng (Tech Stack) và Công cụ hỗ trợ")
  );

  // Tech stack table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Phân lớp", true, AlignmentType.CENTER, "D9E2EC", 22),
            createCell("Công nghệ / Thư viện", true, AlignmentType.CENTER, "D9E2EC", 38),
            createCell("Mục đích áp dụng trong QuickShop", true, AlignmentType.CENTER, "D9E2EC", 40)
          ]
        }),
        new TableRow({
          children: [
            createCell("Backend Microservices", true, AlignmentType.LEFT),
            createCell("Java 17, Spring Boot 3.3.x, Spring Cloud Gateway, OpenFeign, Resilience4j, ShedLock", false, AlignmentType.LEFT),
            createCell("Xây dựng 7 microservices độc lập, định tuyến tập trung, giao tiếp liên service, circuit breaker chịu lỗi và đồng bộ tác vụ định kỳ.", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("Frontend SPA", true, AlignmentType.LEFT),
            createCell("React 18, Vite, Tailwind CSS, Zustand, GSAP, Lenis, Three.js, CKEditor 5, Leaflet", false, AlignmentType.LEFT),
            createCell("Giao diện Storefront Flagship Bento Grid, Kênh Seller, Admin, Shipper, bản đồ số OSM, Messenger chat và tương tác 3D Scrollytelling.", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("Database & Cache", true, AlignmentType.LEFT),
            createCell("MySQL 8.0 (6 DBs), Redis 7 Alpine", false, AlignmentType.LEFT),
            createCell("Mô hình Database-per-Service; Redis lưu giỏ hàng phân tán (Hash), Token Blacklist, OTP Email TTL 5 phút và Caching sản phẩm.", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("Message Broker", true, AlignmentType.LEFT),
            createCell("RabbitMQ 3.13 Management", false, AlignmentType.LEFT),
            createCell("Điều phối giao dịch phân tán Saga Orchestrator, gửi email bất đồng bộ và xử lý thông điệp lỗi qua Dead Letter Queue (order.dlq).", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("Thanh toán & Vận chuyển", true, AlignmentType.LEFT),
            createCell("VNPay Sandbox, PayOS VietQR EMVCo, Leaflet OSM, Nominatim Geocoding", false, AlignmentType.LEFT),
            createCell("Thanh toán kép qua VNPay và quét mã QR chuyển khoản động PayOS; tính cước GHN/GHTK, bản đồ định vị GPS và Kênh Shipper.", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("DevOps & Giám sát", true, AlignmentType.LEFT),
            createCell("Docker Compose, Nginx SSL, MinIO S3, GitHub Actions, Prometheus, Grafana, Zipkin", false, AlignmentType.LEFT),
            createCell("Đóng gói container tối ưu RAM VPS, reverse proxy SSL, lưu trữ ảnh MinIO, CI/CD tự động và giám sát metrics/tracing tập trung.", false, AlignmentType.LEFT)
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 180, after: 100 } }),

    pHeading2("1.4. Kiến trúc hệ thống và Thiết kế Cơ sở dữ liệu"),
    pBody("QuickShop áp dụng triệt để mô hình kiến trúc Microservices phân tán hướng sự kiện (Event-Driven Architecture). Toàn bộ luồng truy cập từ Internet được tiếp nhận bởi Nginx Reverse Proxy (xử lý SSL Termination, bảo mật CSP, tile proxy) và chuyển tiếp qua Spring Cloud API Gateway (Port 8080). Tại Gateway, hệ thống áp dụng cơ chế JWT Dual Auth Filter (hỗ trợ cả Cookie và Bearer Header) và Token Bucket Rate Limiting trước khi định tuyến tới 6 Business Microservices."),
    pImage(readImg("architecture_diagram.png"), 600, 320),
    pCaption("Hình 1.1: Sơ đồ kiến trúc tổng thể hệ thống Microservices QuickShop (Cloud-Native)"),

    pBody("Về mặt lưu trữ, QuickShop tuân thủ nguyên tắc cách ly dữ liệu tuyệt đối (Database-per-Service) gồm 6 cơ sở dữ liệu MySQL 8.0 độc lập. Không service nào được phép truy vấn trực tiếp DB của service khác mà bắt buộc phải giao tiếp thông qua REST API hoặc Message Broker RabbitMQ:"),
    pBody("1. identity_db: Chứa thông tin người dùng, vai trò (BUYER, SELLER, ADMIN, SHIPPER), hồ sơ KYC gian hàng và sổ địa chỉ có tọa độ GPS."),
    pBody("2. catalog_db: Quản lý danh mục sản phẩm, biến thể SKU, tồn kho thực tế, tồn kho giữ chỗ và đánh giá sao kèm nhận xét."),
    pBody("3. order_db: Quản lý Master Orders, Sub-Orders theo Shop, Order Items, mã giảm giá Voucher sàn & shop, và yêu cầu đổi trả hoàn tiền."),
    pBody("4. payment_db: Quản lý tài khoản ký quỹ trung gian Escrow, Sổ cái kép Double-Entry Ledger ghi nhận Debit/Credit, ví người bán và lệnh rút tiền."),
    pBody("5. shipping_db: Quản lý vận đơn, trạng thái bưu tá giao hàng, cước phí và log webhook nhà vận chuyển đảm bảo Idempotency."),
    pBody("6. notification_db: Quản lý thông báo người dùng, nhật ký email đơn hàng và cấu hình chiến dịch Flash Sale AI."),
    pImage(readImg("database_erd.png"), 600, 360),
    pCaption("Hình 1.2: Sơ đồ thực thể liên kết (ERD) cơ sở dữ liệu Microservices QuickShop"),
    new Paragraph({ pageBreakBefore: true })
  );

  // ==========================================
  // CHƯƠNG 2: QUÁ TRÌNH THỰC HIỆN DỰ ÁN THEO SCRUM (4 SPRINT)
  // ==========================================
  children.push(
    pHeading1("CHƯƠNG 2: QUÁ TRÌNH THỰC HIỆN DỰ ÁN THEO MÔ HÌNH SCRUM (4 SPRINT)"),
    pBody("Dự án QuickShop được vận hành kỷ luật theo khung phương pháp Agile/Scrum qua đúng **04 Sprint**, mỗi Sprint dài đúng 01 tuần làm việc (5 ngày làm việc từ Thứ Hai đến Thứ Sáu, **hoàn toàn nghỉ Thứ Bảy và Chủ Nhật**). Tổng thời gian thực hiện gồm **20 ngày làm việc chính thức** (từ 26/10/2026 đến 20/11/2026)."),
    pBody("Quy trình Scrum được tuân thủ nghiêm ngặt: Đầu mỗi tuần tổ chức họp Sprint Planning để chốt Sprint Backlog; đầu mỗi ngày tổ chức Daily Standup 15 phút cập nhật tiến độ; cuối tuần tổ chức Sprint Review nghiệm thu sản phẩm và Sprint Retrospective rút kinh nghiệm cải tiến quy trình."),

    pHeading2("2.1. Sprint 1 (Tuần 1: 26/10/2026 – 30/10/2026): Khởi tạo dự án, Thiết kế CSDL & Khung hạ tầng Microservices"),
    pBody("• Mục tiêu Sprint (Sprint Goal): Thiết lập môi trường phát triển dùng chung với Docker Compose (MySQL, Redis, RabbitMQ, MinIO), chốt kiến trúc 1 Gateway + 6 Backend Services, hoàn thiện DDL CSDL phân tán và khởi tạo khung dự án Frontend React 18 + Backend Spring Boot 3.3.")
  );

  // Sprint 1 Table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Thành viên", true, AlignmentType.CENTER, "D9E2EC", 22),
            createCell("Vai trò", true, AlignmentType.CENTER, "D9E2EC", 18),
            createCell("Nhiệm vụ được giao (Sprint Backlog 1)", true, AlignmentType.CENTER, "D9E2EC", 45),
            createCell("Trạng thái", true, AlignmentType.CENTER, "D9E2EC", 15)
          ]
        }),
        new TableRow({
          children: [
            createCell("Lê Đức Anh", true, AlignmentType.LEFT),
            createCell("Team Leader", false, AlignmentType.CENTER),
            createCell("Lập kế hoạch 4 Sprint, thiết kế kiến trúc hệ thống, cấu hình Docker Compose (MySQL, Redis, RabbitMQ, MinIO), khởi tạo Spring Cloud Gateway (8080) và thiết lập Git conventions.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("Nguyễn Văn Kiên", true, AlignmentType.LEFT),
            createCell("Backend Lead", false, AlignmentType.CENTER),
            createCell("Phân tích nghiệp vụ giỏ hàng & đơn hàng đa shop, thiết kế ERD Order/Payment/Shipping, xây dựng thư viện common-library và khởi tạo Order Service (8083).", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("Triệu Quốc Bình", true, AlignmentType.LEFT),
            createCell("Frontend Lead", false, AlignmentType.CENTER),
            createCell("Khảo sát hành trình người dùng 4 phân hệ, khởi tạo dự án React 18 + Vite + Tailwind CSS, dựng Storefront Layout Shopee-style và khởi tạo Catalog Service (8082).", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 140, after: 60 } }),
    pBody("Kết quả đạt được & Minh chứng Sprint 1:"),
    pBody("Toàn bộ hạ tầng cốt lõi đã khởi chạy thành công bằng Docker Compose. Giao diện trang chủ Storefront bước đầu hình thành với đầy đủ Header, ô tìm kiếm và Drawer giỏ hàng trượt mượt mà."),
    pImage(readImg("storefront_home.png"), 580, 320),
    pCaption("Hình 2.1: Giao diện Storefront Flagship trang chủ QuickShop chạy trên nền tảng React 18"),
    pCallout(
      "Khó khăn & Cách khắc phục (Sprint 1 Retrospective)",
      "• Vấn đề gặp phải: Trình duyệt chặn request từ Frontend (port 5173/80) sang API Gateway (port 8080) do chính sách CORS; đồng thời container RabbitMQ khởi động chậm hơn các backend service.\n• Giải pháp xử lý: Cấu hình CorsWebFilter tập trung tại Spring Cloud Gateway cho phép mọi methods/headers; bổ sung cấu hình healthcheck và depends_on cho RabbitMQ trong Docker Compose."
    ),
    new Paragraph({ spacing: { before: 180, after: 100 } }),

    pHeading2("2.2. Sprint 2 (Tuần 2: 02/11/2026 – 06/11/2026): Định danh bảo mật, Catalog đa biến thể, Giỏ hàng phân tán & Phê duyệt Shop"),
    pBody("• Mục tiêu Sprint (Sprint Goal): Hoàn thiện Identity Service với JWT Dual Auth (Cookie & Bearer), xác thực OTP Email qua Redis TTL 5 phút, xây dựng Giỏ hàng phân tán Redis Hash, quản lý danh mục & biến thể SKU có khóa lạc quan, và quy trình đăng ký KYC Shop.")
  );

  // Sprint 2 Table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Thành viên", true, AlignmentType.CENTER, "D9E2EC", 22),
            createCell("Vai trò", true, AlignmentType.CENTER, "D9E2EC", 18),
            createCell("Nhiệm vụ được giao (Sprint Backlog 2)", true, AlignmentType.CENTER, "D9E2EC", 45),
            createCell("Trạng thái", true, AlignmentType.CENTER, "D9E2EC", 15)
          ]
        }),
        new TableRow({
          children: [
            createCell("Lê Đức Anh", true, AlignmentType.LEFT),
            createCell("Team Leader", false, AlignmentType.CENTER),
            createCell("Khởi tạo Identity Service (8081), triển khai mã hóa BCrypt, JWT Dual Auth (HttpOnly Cookie & Bearer Header), Refresh Token Rotation, Redis Token Blacklist, OTP Email và phân quyền Gateway RBAC.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("Nguyễn Văn Kiên", true, AlignmentType.LEFT),
            createCell("Backend Lead", false, AlignmentType.CENTER),
            createCell("Triển khai giỏ hàng phân tán trên Redis (Guest/User Cart), thuật toán gộp giỏ hàng không mất dữ liệu khi đăng nhập, Checkout đa shop tạo Master/Sub-Orders và Dual Voucher Engine.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("Triệu Quốc Bình", true, AlignmentType.LEFT),
            createCell("Frontend Lead", false, AlignmentType.CENTER),
            createCell("Hiện thực API Catalog Product & SKU Matrix (Màu, Size, Giá, Tồn kho), khóa lạc quan @Version chống âm kho, upload ảnh MinIO S3, Redis Cache < 10ms và kết nối giao diện Buyer với API thật.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 140, after: 60 } }),
    pBody("Kết quả đạt được & Minh chứng Sprint 2:"),
    pBody("Người dùng có thể đăng ký tài khoản, nhận mã OTP qua email thực tế, đăng nhập an toàn bằng JWT và tự động nhận giỏ hàng cũ được lưu trữ trên Redis."),
    pImage(readImg("auth_login.png"), 580, 320),
    pCaption("Hình 2.2: Giao diện Đăng nhập / Đăng ký tài khoản đa vai trò và xác thực OTP"),
    pCallout(
      "Khó khăn & Cách khắc phục (Sprint 2 Retrospective)",
      "• Vấn đề gặp phải: Khi nhiều người cùng đặt mua một sản phẩm tại cùng một thời điểm có nguy cơ xảy ra Race Condition dẫn tới tồn kho bị âm.\n• Giải pháp xử lý: Áp dụng cơ chế khóa lạc quan (Optimistic Locking) bằng trường @Version trên thực thể ProductSku, đồng thời thiết lập cơ chế Redis Distributed Lock khi thực hiện lệnh giữ kho."
    ),
    new Paragraph({ spacing: { before: 180, after: 100 } }),

    pHeading2("2.3. Sprint 3 (Tuần 3: 09/11/2026 – 13/11/2026): Saga Orchestration, Thanh toán VNPay, Ký quỹ Escrow & Trí tuệ nhân tạo Gemini"),
    pBody("• Mục tiêu Sprint (Sprint Goal): Triển khai điều phối giao dịch phân tán Saga Orchestrator qua RabbitMQ, cơ chế bồi hoàn Rollback, cổng thanh toán VNPay Sandbox, Sổ cái kế toán kép Double-entry Ledger, Escrow Holding, Shipping Webhook Idempotency và Trợ lý AI Google Gemini.")
  );

  // Sprint 3 Table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Thành viên", true, AlignmentType.CENTER, "D9E2EC", 22),
            createCell("Vai trò", true, AlignmentType.CENTER, "D9E2EC", 18),
            createCell("Nhiệm vụ được giao (Sprint Backlog 3)", true, AlignmentType.CENTER, "D9E2EC", 45),
            createCell("Trạng thái", true, AlignmentType.CENTER, "D9E2EC", 15)
          ]
        }),
        new TableRow({
          children: [
            createCell("Lê Đức Anh", true, AlignmentType.LEFT),
            createCell("Team Leader", false, AlignmentType.CENTER),
            createCell("Xây dựng Notification AI Service (8086), người tiêu dùng RabbitMQ gửi email đơn hàng tự động, tích hợp Google Gemini AI Chatbot mua sắm (chống Prompt Injection) và AI SEO Generator.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("Nguyễn Văn Kiên", true, AlignmentType.LEFT),
            createCell("Backend Lead", false, AlignmentType.CENTER),
            createCell("Hiện thực Saga Orchestration điều phối đặt hàng phân tán qua RabbitMQ, cơ chế bồi hoàn Rollback < 200ms, Dead Letter Queue (order.dlq), cổng VNPay Sandbox, Sổ cái kép và Escrow Holding.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("Triệu Quốc Bình", true, AlignmentType.LEFT),
            createCell("Frontend Lead", false, AlignmentType.CENTER),
            createCell("Xây dựng Kênh Người Bán (Seller Center), Cổng Quản Trị (Admin Portal) duyệt KYC/sản phẩm, hoàn thiện giỏ hàng Shopee-style tick chọn theo shop, trang Checkout và đánh giá 1-5 sao.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 140, after: 60 } }),
    pBody("Kết quả đạt được & Minh chứng Sprint 3:"),
    pBody("Quy trình mua sắm từ Giỏ hàng đa shop -> Áp mã giảm giá -> Checkout -> Thanh toán VNPay đã vận hành trơn tru và được ghi nhận đầy đủ vào Sổ cái kép."),
    pImage(readImg("cart_page.png"), 580, 310),
    pCaption("Hình 2.3a: Giao diện Giỏ hàng Shopee-style gom nhóm theo từng Shop"),
    pImage(readImg("admin_portal.png"), 580, 310),
    pCaption("Hình 2.3b: Cổng Quản Trị Admin Portal kiểm duyệt sản phẩm và theo dõi số dư Escrow"),
    pCallout(
      "Khó khăn & Cách khắc phục (Sprint 3 Retrospective)",
      "• Vấn đề gặp phải: Khi đối tác vận chuyển gửi lặp lại Webhook trạng thái đơn hàng (Network retry), hệ thống có thể cập nhật trạng thái nhiều lần hoặc ghi thừa bút toán kế toán.\n• Giải pháp xử lý: Xây dựng cơ chế Idempotent Consumer dựa trên trường event_id lưu vào bảng shipping_webhooks_log. Nếu event_id đã tồn tại, trả về ngay HTTP 200 OK mà không xử lý lại."
    ),
    new Paragraph({ spacing: { before: 180, after: 100 } }),

    pHeading2("2.4. Sprint 4 (Tuần 4: 16/11/2026 – 20/11/2026): Mở rộng tính năng cao cấp, Kênh Shipper, PayOS VietQR, Map Leaflet, Chat Messenger, QA & Đóng gói Docker"),
    pBody("• Mục tiêu Sprint (Sprint Goal): Phát triển các tính năng mở rộng cao cấp vượt trội so với kế hoạch ban đầu: Kênh Nhân viên Giao Hàng (Shipper Portal), Cổng thanh toán chuyển khoản PayOS VietQR EMVCo, Bản đồ số Leaflet OpenStreetMap với GPS Nominatim, Đăng nhập Google OAuth, Hệ thống Chat Full-page Messenger, Quản trị Flash Sale AI Auto-Approve, Trình soạn thảo CKEditor 5 MinIO, Trang 3D Scrollytelling, vá 18 lỗi bảo mật QA, QA hồi quy và đóng gói bàn giao toàn diện vào ngày 20/11/2026.")
  );

  // Sprint 4 Table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Thành viên", true, AlignmentType.CENTER, "D9E2EC", 22),
            createCell("Vai trò", true, AlignmentType.CENTER, "D9E2EC", 18),
            createCell("Nhiệm vụ được giao (Sprint Backlog 4)", true, AlignmentType.CENTER, "D9E2EC", 45),
            createCell("Trạng thái", true, AlignmentType.CENTER, "D9E2EC", 15)
          ]
        }),
        new TableRow({
          children: [
            createCell("Lê Đức Anh", true, AlignmentType.LEFT),
            createCell("Team Leader", false, AlignmentType.CENTER),
            createCell("Tích hợp Google OAuth 2.0, phân hệ Shipper Module (ROLE_SHIPPER), bản đồ Leaflet OSM với Nginx Tile Proxy, vá triệt để 18 lỗi an ninh QA (Base64URL JWT, SQLi fast-reject, CSP), tối ưu RAM VPS 1GB và cấu hình CI/CD deploy production.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("Nguyễn Văn Kiên", true, AlignmentType.LEFT),
            createCell("Backend Lead", false, AlignmentType.CENTER),
            createCell("Tích hợp Cổng thanh toán chuyển khoản tự động PayOS VietQR EMVCo, lập lịch Scheduled Task tự động giải phóng Escrow sau 72h, API thống kê doanh thu theo mã SKU, Cổng Flash Sale đa chế độ (Manual & AI Mode) và chủ trì kiểm thử hồi quy Backend (> 140 test cases 100% Passed).", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("Triệu Quốc Bình", true, AlignmentType.LEFT),
            createCell("Frontend Lead", false, AlignmentType.CENTER),
            createCell("Xây dựng Kênh Shipper Portal cho bưu tá, Hệ thống Tin nhắn thời gian thực Full-page Messenger nhúng Thẻ sản phẩm thông minh, trình soạn thảo CKEditor 5 chèn ảnh MinIO nội tuyến, nâng cấp giao diện Impeccable (Bento Grid, Modal lọc giữa, 3D Scrollytelling GSAP/Lenis) và kiểm thử E2E giao diện.", false, AlignmentType.LEFT),
            createCell("Hoàn thành (100%)", true, AlignmentType.CENTER, "DCFCE7")
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 140, after: 60 } }),
    pBody("Kết quả đạt được & Minh chứng Sprint 4:"),
    pBody("Sprint 4 đã nâng cấp QuickShop thành một nền tảng thương mại điện tử hoàn chỉnh, trực quan và đẳng cấp với 4 phân hệ người dùng hoạt động đồng bộ trên Live Domain https://quickshop.io.vn:"),
    pImage(readImg("shipper_portal.png"), 580, 310),
    pCaption("Hình 2.4a: Kênh Nhân viên Giao Hàng (Shipper Portal) tiếp nhận và cập nhật trạng thái kiện hàng"),
    pImage(readImg("messenger_chat.png"), 580, 310),
    pCaption("Hình 2.4b: Hệ thống Tin nhắn Full-page Messenger Buyer - Seller với Thẻ sản phẩm thông minh"),
    pImage(readImg("seller_portal.png"), 580, 310),
    pCaption("Hình 2.4c: Kênh Người Bán (Seller Center) tích hợp bộ soạn thảo CKEditor 5 và thống kê SKU"),
    pImage(readImg("intro_3d.png"), 580, 310),
    pCaption("Hình 2.4d: Trang Giới thiệu tương tác 3D Scrollytelling Showcase 6 phân hệ (Three.js / GSAP)"),
    pCallout(
      "Khó khăn & Cách khắc phục (Sprint 4 Retrospective)",
      "• Vấn đề gặp phải: Trình duyệt chặn tải các mảnh bản đồ (Tiles) từ OpenStreetMap và API tọa độ Nominatim do chính sách Content Security Policy (CSP) và lỗi Mixed Content HTTPS/HTTP.\n• Giải pháp xử lý: Cấu hình các Reverse Proxy location chuyên dụng /osm-tiles/ và /nominatim/ ngay trên Nginx Edge server, kèm bộ nhớ đệm proxy_cache giúp tăng tốc độ hiển thị bản đồ lên gấp 5 lần và tuân thủ 100% CSP an toàn."
    ),
    new Paragraph({ pageBreakBefore: true })
  );

  // ==========================================
  // CHƯƠNG 3: KIỂM THỬ VÀ ĐÁNH GIÁ KẾT QUẢ DỰ ÁN
  // ==========================================
  children.push(
    pHeading1("CHƯƠNG 3: KIỂM THỬ VÀ ĐÁNH GIÁ KẾT QUẢ DỰ ÁN"),
    pHeading2("3.1. Tổng hợp kết quả Kiểm thử chất lượng (QA Report)"),
    pBody("Hoạt động kiểm thử chất lượng phần mềm được thực hiện liên tục và gắt gao qua cả 4 Sprint, bao gồm kiểm thử tự động (Unit / Integration Test bằng JUnit 5 & Mockito), kiểm thử API trên Postman, kiểm thử an toàn thông tin (Security Matrix), và kiểm thử giao diện người dùng (End-to-End) trên đa dạng các thiết bị (Desktop, Laptop, iPad, iPhone, Android)."),
    pBody("Tổng số hơn 140 kịch bản kiểm thử (Test Cases) đã được thực thi và đạt tỷ lệ thành công 100%:")
  );

  // Test Case Matrix Table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Module chức năng", true, AlignmentType.CENTER, "D9E2EC", 35),
            createCell("Tổng Test Case", true, AlignmentType.CENTER, "D9E2EC", 18),
            createCell("Pass", true, AlignmentType.CENTER, "D9E2EC", 15),
            createCell("Fail", true, AlignmentType.CENTER, "D9E2EC", 15),
            createCell("Tỷ lệ đạt (%)", true, AlignmentType.CENTER, "D9E2EC", 17)
          ]
        }),
        new TableRow({
          children: [
            createCell("1. Xác thực & Phân quyền (Auth, Google, OTP, RBAC 4 vai trò)", false, AlignmentType.LEFT),
            createCell("25", false, AlignmentType.CENTER),
            createCell("25", true, AlignmentType.CENTER),
            createCell("0", false, AlignmentType.CENTER),
            createCell("100%", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("2. Quản lý Danh mục, Sản phẩm đa SKU & CKEditor 5 MinIO", false, AlignmentType.LEFT),
            createCell("30", false, AlignmentType.CENTER),
            createCell("30", true, AlignmentType.CENTER),
            createCell("0", false, AlignmentType.CENTER),
            createCell("100%", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("3. Giỏ hàng Redis phân tán & Saga Orchestrator DLQ", false, AlignmentType.LEFT),
            createCell("35", false, AlignmentType.CENTER),
            createCell("35", true, AlignmentType.CENTER),
            createCell("0", false, AlignmentType.CENTER),
            createCell("100%", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("4. Thanh toán kép (VNPay, PayOS VietQR), Sổ cái kép & Escrow 72h", false, AlignmentType.LEFT),
            createCell("25", false, AlignmentType.CENTER),
            createCell("25", true, AlignmentType.CENTER),
            createCell("0", false, AlignmentType.CENTER),
            createCell("100%", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("5. Vận chuyển GHN/GHTK, Kênh Shipper Portal & Webhook Idempotency", false, AlignmentType.LEFT),
            createCell("15", false, AlignmentType.CENTER),
            createCell("15", true, AlignmentType.CENTER),
            createCell("0", false, AlignmentType.CENTER),
            createCell("100%", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("6. Trợ lý ảo AI Gemini, Flash Sale AI & Full-page Messenger", false, AlignmentType.LEFT),
            createCell("15", false, AlignmentType.CENTER),
            createCell("15", true, AlignmentType.CENTER),
            createCell("0", false, AlignmentType.CENTER),
            createCell("100%", true, AlignmentType.CENTER, "DCFCE7")
          ]
        }),
        new TableRow({
          children: [
            createCell("TỔNG CỘNG TOÀN DỰ ÁN", true, AlignmentType.LEFT, "E2E8F0"),
            createCell("145", true, AlignmentType.CENTER, "E2E8F0"),
            createCell("145", true, AlignmentType.CENTER, "E2E8F0"),
            createCell("0", true, AlignmentType.CENTER, "E2E8F0"),
            createCell("100%", true, AlignmentType.CENTER, "DCFCE7")
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 140, after: 60 } }),
    pBody("Trích xuất một số lỗi kỹ thuật (Bug Log) điển hình đã được phát hiện và xử lý dứt điểm:")
  );

  // Bug Log Table
  children.push(
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      rows: [
        new TableRow({
          children: [
            createCell("Mã Bug", true, AlignmentType.CENTER, "D9E2EC", 15),
            createCell("Mô tả lỗi phát hiện", true, AlignmentType.CENTER, "D9E2EC", 45),
            createCell("Mức độ", true, AlignmentType.CENTER, "D9E2EC", 15),
            createCell("Người xử lý", true, AlignmentType.CENTER, "D9E2EC", 25)
          ]
        }),
        new TableRow({
          children: [
            createCell("BUG-01", true, AlignmentType.CENTER),
            createCell("Lỗi trùng lặp SKU (Duplicate SKU Constraint) khi người bán cập nhật nhiều biến thể sản phẩm cùng lúc.", false, AlignmentType.LEFT),
            createCell("High", true, AlignmentType.CENTER, "FEE2E2"),
            createCell("Lê Đức Anh (Closed)", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("BUG-02", true, AlignmentType.CENTER),
            createCell("Xung đột chính sách CSP và lỗi Mixed Content khi tải ảnh sản phẩm từ MinIO S3 trên môi trường production HTTPS.", false, AlignmentType.LEFT),
            createCell("High", true, AlignmentType.CENTER, "FEE2E2"),
            createCell("Triệu Quốc Bình (Closed)", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("BUG-03", true, AlignmentType.CENTER),
            createCell("Thất bại phân giải mã đơn hàng masterOrderId dạng số lớn 100xxx trong luồng callback thanh toán PayOS VietQR.", false, AlignmentType.LEFT),
            createCell("Medium", false, AlignmentType.CENTER, "FEF3C7"),
            createCell("Nguyễn Văn Kiên (Closed)", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("BUG-04", true, AlignmentType.CENTER),
            createCell("Cảnh báo GSAP Invalid Scope và ScrollTrigger target null trên trình duyệt di động khi chuyển trang.", false, AlignmentType.LEFT),
            createCell("Medium", false, AlignmentType.CENTER, "FEF3C7"),
            createCell("Triệu Quốc Bình (Closed)", false, AlignmentType.LEFT)
          ]
        }),
        new TableRow({
          children: [
            createCell("BUG-05", true, AlignmentType.CENTER),
            createCell("Container Java bị treo OOM Killer do cấu hình Metaspace và JVM Heap mặc định quá lớn trên VPS 1GB RAM.", false, AlignmentType.LEFT),
            createCell("High", true, AlignmentType.CENTER, "FEE2E2"),
            createCell("Lê Đức Anh (Closed)", false, AlignmentType.LEFT)
          ]
        })
      ]
    }),
    new Paragraph({ spacing: { before: 180, after: 100 } }),

    pHeading2("3.2. Đánh giá mức độ hoàn thành mục tiêu dự án"),
    pBody("• Mức độ hoàn thiện chức năng: Hệ thống QuickShop đã hoàn thành **100% mục tiêu cốt lõi (MVP)** đề ra ban đầu, đồng thời phát triển thành công **10 tính năng mở rộng cao cấp** trong Sprint 4. Cả 4 phân hệ (Người Mua, Người Bán, Quản Trị Viên, Nhân Viên Giao Hàng) đều vận hành ổn định, đồng bộ dữ liệu thời gian thực giữa Frontend SPA, API Gateway, 6 Backend Services, Redis, MySQL và RabbitMQ."),
    pBody("• Chất lượng mã nguồn và triển khai: Toàn bộ hệ thống được đóng gói tối ưu bằng Docker Compose trên VPS Linux (quản lý bởi Cloudflare & Nginx Reverse Proxy), hỗ trợ tự động hóa CI/CD qua GitHub Actions, đạt tỷ lệ 100% Test Suites Passed và Zero Critical Bugs."),

    pHeading2("3.3. Bài học kinh nghiệm và Hướng phát triển tiếp theo"),
    pBody("Bài học kinh nghiệm rút ra qua kỳ thực tập OJT:"),
    pBody("• Năng lực làm việc nhóm theo mô hình Agile/Scrum: Thấu hiểu tầm quan trọng của việc kỷ luật chia nhỏ công việc thành 4 Sprint tuần tự (nghỉ T7, CN để tái tạo sức lao động), duy trì họp Daily Standup để phát hiện sớm các nút thắt kỹ thuật và văn hóa review code trước khi merge vào nhánh chính."),
    pBody("• Năng lực làm chủ kiến trúc Microservices & Điện toán đám mây: Nắm vững các mô hình giải quyết bài toán phân tán phức tạp như Saga Pattern, Dead Letter Queue, Sổ cái kép Double-Entry Ledger, tối ưu bộ nhớ đệm Redis và bảo mật phân quyền RBAC đa vai trò."),
    pBody("Hướng phát triển tiếp theo:"),
    pBody("• Nâng cấp AI Recommendation Engine sử dụng mô hình học máy (Vector Database / Milvus) để phân tích hành vi và gợi ý sản phẩm cá nhân hóa sâu hơn."),
    pBody("• Mở rộng ứng dụng di động Native (React Native / Flutter) cho phân hệ Shipper và Buyer."),
    new Paragraph({ spacing: { before: 200, after: 100 } }),

    // ==========================================
    // PHỤ LỤC: CHECKLIST TỰ ĐÁNH GIÁ
    // ==========================================
    pCallout(
      "PHỤ LỤC: CHECKLIST TỰ KIỂM TRA DÀNH CHO SINH VIÊN TRƯỚC KHI NỘP BÀI",
      "☑ Đã điền đầy đủ Họ tên, MSSV, tên Giảng viên hướng dẫn và Mentor doanh nghiệp ở Trang bìa.\n☑ Tất cả các đường dẫn GitHub, Live Domain https://quickshop.io.vn và Trello Board đã bật chế độ Public để Hội đồng truy cập được.\n☑ Toàn bộ các khung minh chứng đã được thay thế bằng hình ảnh chụp thực tế sắc nét, có chú thích đánh số rõ ràng (Hình 1.1 đến 2.4d).\n☑ Sơ đồ kiến trúc và sơ đồ ERD trong Chương 1 khớp hoàn toàn 100% với cấu trúc 7 microservices và cơ sở dữ liệu thật của dự án.\n☑ Cả 3 thành viên trong nhóm đều có phân công trách nhiệm cụ thể và minh chứng đóng góp rõ ràng xuyên suốt 4 Sprint ở Chương 2.\n☑ Đã kiểm tra lỗi chính tả, căn đều văn bản (Justify) chuẩn mực và kiểm tra số trang trước khi xuất bản nộp cho Nhà trường."
    )
  );

  const doc = new Document({
    sections: [
      {
        properties: {
          page: {
            margin: {
              top: 1440, // 1 inch = 1440 twips
              bottom: 1440,
              left: 1440,
              right: 1440
            }
          }
        },
        children: children
      }
    ]
  });

  const outPath = path.resolve(__dirname, '../BAO_CAO_DU_AN_OJT_QUICKSHOP.docx');
  console.log('Packing Word document to:', outPath);
  const buffer = await Packer.toBuffer(doc);
  fs.writeFileSync(outPath, buffer);
  console.log(`Document successfully generated! File size: ${buffer.length} bytes.`);
}

buildDoc().catch(err => {
  console.error('Error generating document:', err);
  process.exit(1);
});
