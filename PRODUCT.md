# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Primary:** Online Buyers (Khách hàng) seeking products across diverse categories and independent sellers. They need clear product details, transparent pricing, multi-vendor cart management, dual voucher stacking (Shop + Platform), safe payment options (VNPay & COD with Escrow protection), real-time shipment tracking, and instant shopping assistance.
- **Secondary (Confirmed):**
  - **Sellers (Người bán):** Merchants operating storefronts, managing catalog SKUs and stock levels, confirming orders, printing shipping labels (GHN/GHTK), and reconciling earnings via seller wallets.
  - **Platform Administrators (Quản trị viên):** Operations team reviewing shop KYC submissions, moderating listings, managing site-wide categories and vouchers, and arbitrating customer return/refund disputes.
  - **Logistics Partners / Shippers:** Delivery personnel and simulator agents updating order delivery stages.

## Product Purpose

QuickShop provides a robust, production-grade Multi-Vendor Marketplace (Shopee-style) built on cloud-native microservices. It bridges buyers and multiple merchant stores in a unified storefront, ensuring transactional safety through Escrow intermediation, reliable order orchestration, and contextual AI shopping assistance. Success means frictionless multi-vendor checkout, high consumer trust, and transparent shop operations.

## Positioning

A cloud-native multi-vendor e-commerce platform combining familiar Shopee-grade marketplace UX (cart partitioned by shop, dual stacked vouchers, real-time shipment tracking) with enterprise transaction integrity (Saga Orchestrator with Dead Letter Queue, double-entry financial ledger with Escrow settlement) and embedded Google Gemini AI shopping assistance.

## Operating Context

- **Environment:** Responsive modern web (desktop, tablet, mobile browsers).
- **Core Interactions:** High-frequency browsing, fast search with category and price filters, multi-vendor cart drawer and page, multi-step checkout with address selection and payment gateway redirect (VNPay), live order status timelines, and an on-page AI shopping assistant widget.
- **Merchant & Admin Portals:** Integrated authenticated management dashboards with order fulfillment workflows, inventory tracking, KYC verification, and financial ledgers.

## Capabilities and Constraints

- **Capabilities:**
  - Multi-role authentication (JWT with HttpOnly cookie & Bearer token support, RBAC: `ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`).
  - Dynamic product catalog with hierarchical categories, multi-variant SKUs, and optimistic concurrency control for inventory.
  - Multi-vendor cart on Redis with automatic partitioning by seller shop.
  - Multi-vendor checkout creating one Master Order decomposed into Sub-Orders per shop.
  - Dual voucher engine allowing simultaneous platform and shop promotional codes.
  - Distributed transaction management via Saga Orchestrator pattern over RabbitMQ with Dead Letter Queue error recovery.
  - Payment integration with VNPay Sandbox and COD, protected by an intermediary Escrow account and double-entry ledger.
  - Logistics integration with GHN/GHTK shipping rate calculation, tracking numbers, and idempotent webhooks.
  - AI capabilities powered by Google Gemini API: customer consultation chatbot and automatic SEO product description generator.
- **Constraints:**
  - Microservices architecture across 7 core services with strict database-per-service isolation (MySQL 8.0).
  - Explicit State Machine enforcement for Master Orders and Sub-Orders (`DRAFT` → `PENDING_PAYMENT` → `PROCESSING` → `COMPLETED` / `CANCELLED` / `REFUNDED`).
  - Strict 72-hour escrow hold before merchant wallet release upon confirmed delivery.

## Brand Commitments

- **Name:** QuickShop (Rikkei E-Commerce QuickShop - Marketplace Edition)
- **Visual Identity:** Modern, lively, and trust-inspiring e-commerce aesthetic inspired by leading Southeast Asian marketplace standards (Shopee model) featuring vibrant accents, clean layout, high-contrast badges for discounts and official stores, and crisp typographic hierarchy.
- **Voice & Tone:** Helpful, transparent, energetic, and professional.

## Evidence on Hand

- **SRS Specification:** `SRS - Rikkei E-Commerce QuickShop (Marketplace Edition).md` (comprehensive functional requirements, state machines, and API architecture).
- **Frontend Codebase:** `frontend/` (React 18, Vite, Tailwind CSS, Lucide Icons, Zustand stores for auth, cart, and notifications).
- **Microservices Backend:** 7 Spring Boot 3.3.x microservices (`api-gateway`, `identity-service`, `catalog-service`, `order-service`, `payment-service`, `shipping-service`, `notification-ai-service`).
- **Infrastructure & Deployment:** `docker-compose.local.yml`, `docker-compose.prod.yml`, and comprehensive guides under `docs/`.

## Product Principles

1. **Uncompromising Financial & Transactional Clarity:** Never conceal shipping fees, platform commissions, or voucher breakdowns. Multi-vendor itemization and Escrow protections must be obvious to both buyer and seller.
2. **Effortless Multi-Vendor Experience:** Multi-shop ordering and multi-package tracking must feel coherent and unified, preventing cognitive overload during checkout and fulfillment.
3. **Responsive State Feedback:** All commerce-critical interactions (cart operations, voucher checks, order status progressions, wallet balance updates) must provide instantaneous, unambiguous feedback.
4. **Assistive, Non-Intrusive Intelligence:** The Gemini AI advisor serves as a helpful companion that accelerates product discovery and answers queries without distracting from the core buying workflow.

## Accessibility & Inclusion

- Compliance with WCAG 2.1 AA standards.
- High contrast for pricing, discount badges, and inventory alert tags.
- Full keyboard accessibility and focus management for interactive drawers, modals, and checkout wizards.
- Clear error states, field validation messages, and screen-reader accessible form labels.
