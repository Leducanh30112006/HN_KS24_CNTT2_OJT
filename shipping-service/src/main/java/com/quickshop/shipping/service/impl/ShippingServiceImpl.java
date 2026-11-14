package com.quickshop.shipping.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.shipping.controller.ShippingController;
import com.quickshop.shipping.dto.*;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.entity.ShippingWebhookLog;
import com.quickshop.shipping.repository.ShipmentRepository;
import com.quickshop.shipping.repository.ShippingWebhookLogRepository;
import com.quickshop.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final ShippingWebhookLogRepository webhookLogRepository;

    @Override
    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    @Override
    public CalculateFeeResponse calculateFee(CalculateFeeRequest request) {
        String carrier = request.getCarrier() != null ? request.getCarrier().toUpperCase() : "GHN";
        int weight = request.getWeightGrams() != null && request.getWeightGrams() > 0 ? request.getWeightGrams() : 500;
        boolean isExpress = Boolean.TRUE.equals(request.getIsExpress());

        BigDecimal baseFee;
        int estimatedDays;
        switch (carrier) {
            case "GHTK":
                baseFee = BigDecimal.valueOf(22000);
                estimatedDays = isExpress ? 1 : 3;
                break;
            case "MOCK_CARRIER":
                baseFee = BigDecimal.valueOf(20000);
                estimatedDays = isExpress ? 1 : 2;
                break;
            case "GHN":
            default:
                carrier = "GHN";
                baseFee = BigDecimal.valueOf(25000);
                estimatedDays = isExpress ? 1 : 3;
                break;
        }

        // Distance factor: inter-province vs intra-province
        if (request.getFromProvince() != null && request.getToProvince() != null
                && !request.getFromProvince().trim().equalsIgnoreCase(request.getToProvince().trim())) {
            baseFee = baseFee.add(BigDecimal.valueOf(10000));
            estimatedDays += 1;
        }

        // Weight surcharges: +5,000 VND per extra 500g above 1000g
        if (weight > 1000) {
            int extraBlocks = (int) Math.ceil((weight - 1000) / 500.0);
            baseFee = baseFee.add(BigDecimal.valueOf(extraBlocks * 5000L));
        }

        // Express service surcharge
        if (isExpress) {
            baseFee = baseFee.add(BigDecimal.valueOf(15000));
        }

        return CalculateFeeResponse.builder()
                .carrier(carrier)
                .serviceName(isExpress ? "Hỏa tốc 24h" : "Tiêu chuẩn")
                .shippingFee(baseFee)
                .estimatedDays(estimatedDays)
                .note("Cước tính toán tự động dựa trên trọng lượng " + weight + "g và khoảng cách giao hàng")
                .build();
    }

    @Override
    @Transactional
    public Shipment createShipment(CreateShipmentRequest request) {
        if (request.getSubOrderId() == null) {
            throw new BadRequestException("Mã đơn hàng con (subOrderId) không được để trống");
        }

        return shipmentRepository.findBySubOrderId(request.getSubOrderId())
                .orElseGet(() -> {
                    String carrier = request.getCarrierName() != null ? request.getCarrierName().toUpperCase() : "GHN";
                    String prefix = carrier.equals("GHTK") ? "GHTK-" : (carrier.equals("GHN") ? "GHN-" : "MOCK-");
                    String trackingCode = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

                    BigDecimal fee = request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.valueOf(25000);
                    int weight = request.getWeightGrams() != null ? request.getWeightGrams() : 500;

                    Shipment shipment = Shipment.builder()
                            .subOrderId(request.getSubOrderId())
                            .trackingCode(trackingCode)
                            .carrierName(carrier)
                            .shippingFee(fee)
                            .shippingStatus("READY_TO_PICK")
                            .recipientName(request.getRecipientName())
                            .recipientPhone(request.getRecipientPhone())
                            .fromAddress(request.getFromAddress())
                            .toAddress(request.getToAddress())
                            .weightGrams(weight)
                            .estimatedDeliveryDate(LocalDateTime.now().plusDays(3))
                            .build();

                    log.info("Created new shipment: trackingCode={}, subOrderId={}, carrier={}",
                            trackingCode, request.getSubOrderId(), carrier);
                    return shipmentRepository.save(shipment);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getShipmentByTrackingCode(String trackingCode) {
        return shipmentRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn với mã: " + trackingCode));
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment getShipmentBySubOrderId(Long subOrderId) {
        return shipmentRepository.findBySubOrderId(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn cho đơn hàng con: " + subOrderId));
    }

    @Override
    @Transactional
    public Shipment updateShipmentStatus(Long shipmentId, ShipmentStatusUpdateRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn ID: " + shipmentId));

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            shipment.setShippingStatus(request.getStatus().toUpperCase());
        }
        return shipmentRepository.save(shipment);
    }

    @Override
    @Transactional
    public boolean handleWebhook(ShippingController.WebhookPayload payload) {
        if (payload.getEventId() == null || payload.getEventId().isBlank()) {
            throw new BadRequestException("Mã sự kiện webhook (eventId) không được để trống");
        }

        // Idempotency check
        if (webhookLogRepository.existsByEventId(payload.getEventId())) {
            log.warn("Webhook duplicate eventId detected: {}, skipping", payload.getEventId());
            return false;
        }

        // Save log
        webhookLogRepository.save(ShippingWebhookLog.builder()
                .eventId(payload.getEventId())
                .carrierName(payload.getCarrierName() != null ? payload.getCarrierName() : "UNKNOWN")
                .payload(payload.toString())
                .build());

        // Update shipment
        if (payload.getTrackingCode() != null) {
            shipmentRepository.findByTrackingCode(payload.getTrackingCode()).ifPresent(shipment -> {
                if (payload.getStatus() != null) {
                    shipment.setShippingStatus(payload.getStatus().toUpperCase());
                    shipmentRepository.save(shipment);
                    log.info("Shipment {} status updated to {} via webhook", payload.getTrackingCode(), payload.getStatus());
                }
            });
        }
        return true;
    }

    @Override
    @Transactional
    public Shipment simulateNextCarrierStep(String trackingCode) {
        Shipment shipment = getShipmentByTrackingCode(trackingCode);
        String currentStatus = shipment.getShippingStatus();
        String nextStatus;

        switch (currentStatus) {
            case "READY_TO_PICK":
                nextStatus = "PICKED";
                break;
            case "PICKED":
                nextStatus = "DELIVERING";
                break;
            case "DELIVERING":
                nextStatus = "DELIVERED";
                break;
            case "DELIVERED":
                return shipment; // Already delivered
            default:
                nextStatus = "DELIVERING";
                break;
        }

        shipment.setShippingStatus(nextStatus);
        log.info("Simulated shipment status advancement: {} -> {} for trackingCode {}",
                currentStatus, nextStatus, trackingCode);
        return shipmentRepository.save(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentTrackingResponse getShipmentTracking(String trackingCode) {
        Shipment shipment = getShipmentByTrackingCode(trackingCode);
        return buildTrackingResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentTrackingResponse getShipmentTrackingBySubOrderId(Long subOrderId) {
        Shipment shipment = getShipmentBySubOrderId(subOrderId);
        return buildTrackingResponse(shipment);
    }

    @Override
    @Transactional
    public Shipment simulateCarrierStatus(String trackingCode, String targetStatus) {
        if (targetStatus == null || targetStatus.isBlank()) {
            throw new BadRequestException("Trạng thái mô phỏng (targetStatus) không được để trống");
        }
        Shipment shipment = getShipmentByTrackingCode(trackingCode);
        String cleanStatus = targetStatus.trim().toUpperCase();
        shipment.setShippingStatus(cleanStatus);
        log.info("Simulated carrier direct status update: {} -> {} for trackingCode {}",
                shipment.getShippingStatus(), cleanStatus, trackingCode);
        return shipmentRepository.save(shipment);
    }

    private ShipmentTrackingResponse buildTrackingResponse(Shipment shipment) {
        String status = shipment.getShippingStatus();
        String description;
        switch (status) {
            case "READY_TO_PICK":
                description = "Chờ đơn vị vận chuyển lấy hàng";
                break;
            case "PICKED":
                description = "Đơn vị vận chuyển đã lấy hàng và nhập kho";
                break;
            case "DELIVERING":
                description = "Kiện hàng đang được nhân viên giao hàng (Shipper) phát tới bạn";
                break;
            case "DELIVERED":
                description = "Giao hàng thành công";
                break;
            case "DELIVERY_FAILED":
                description = "Giao hàng không thành công (Hẹn giao lại)";
                break;
            case "RETURNED":
                description = "Kiện hàng đã được hoàn trả lại cho người bán";
                break;
            default:
                description = "Đang xử lý vận chuyển";
                break;
        }

        List<TrackingMilestoneDto> timeline = new ArrayList<>();
        LocalDateTime baseTime = shipment.getCreatedAt() != null ? shipment.getCreatedAt() : LocalDateTime.now().minusHours(4);

        // Milestone 1: READY_TO_PICK
        boolean m1Completed = true;
        boolean m1Current = "READY_TO_PICK".equalsIgnoreCase(status);
        timeline.add(TrackingMilestoneDto.builder()
                .status("READY_TO_PICK")
                .title("Đơn hàng đã sẵn sàng giao")
                .description("Người bán đã đóng gói xong, đang chờ bưu tá " + shipment.getCarrierName() + " đến lấy hàng.")
                .location(shipment.getFromAddress() != null ? shipment.getFromAddress() : "Kho người bán")
                .timestamp(baseTime)
                .completed(m1Completed)
                .current(m1Current)
                .build());

        // Milestone 2: PICKED
        boolean m2Completed = !"READY_TO_PICK".equalsIgnoreCase(status);
        boolean m2Current = "PICKED".equalsIgnoreCase(status);
        timeline.add(TrackingMilestoneDto.builder()
                .status("PICKED")
                .title("Đã lấy hàng và nhập kho trung chuyển")
                .description("Bưu tá " + shipment.getCarrierName() + " đã nhận kiện hàng và nhập vào trung tâm khai thác phân loại.")
                .location("Trung tâm khai thác logistics " + shipment.getCarrierName())
                .timestamp(baseTime.plusHours(2))
                .completed(m2Completed)
                .current(m2Current)
                .build());

        // Milestone 3: DELIVERING
        boolean m3Completed = "DELIVERING".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status)
                || "DELIVERY_FAILED".equalsIgnoreCase(status) || "RETURNED".equalsIgnoreCase(status);
        boolean m3Current = "DELIVERING".equalsIgnoreCase(status);
        timeline.add(TrackingMilestoneDto.builder()
                .status("DELIVERING")
                .title("Đang giao hàng")
                .description("Nhân viên giao vận đang trên đường giao kiện hàng tới địa chỉ nhận.")
                .location("Bưu cục phát " + shipment.getCarrierName() + " khu vực người nhận")
                .timestamp(baseTime.plusHours(8))
                .completed(m3Completed)
                .current(m3Current)
                .build());

        // Milestone 4: DELIVERED or Final status
        if ("DELIVERY_FAILED".equalsIgnoreCase(status)) {
            timeline.add(TrackingMilestoneDto.builder()
                    .status("DELIVERY_FAILED")
                    .title("Giao hàng không thành công")
                    .description("Giao hàng không thành công do không liên lạc được với người nhận hoặc thay đổi thời gian nhận.")
                    .location(shipment.getToAddress())
                    .timestamp(shipment.getUpdatedAt() != null ? shipment.getUpdatedAt() : LocalDateTime.now())
                    .completed(true)
                    .current(true)
                    .build());
        } else if ("RETURNED".equalsIgnoreCase(status)) {
            timeline.add(TrackingMilestoneDto.builder()
                    .status("RETURNED")
                    .title("Đã hoàn hàng về cho người bán")
                    .description("Kiện hàng đã được hoàn trả thành công về kho của người bán.")
                    .location(shipment.getFromAddress())
                    .timestamp(shipment.getUpdatedAt() != null ? shipment.getUpdatedAt() : LocalDateTime.now())
                    .completed(true)
                    .current(true)
                    .build());
        } else {
            boolean m4Completed = "DELIVERED".equalsIgnoreCase(status);
            boolean m4Current = "DELIVERED".equalsIgnoreCase(status);
            timeline.add(TrackingMilestoneDto.builder()
                    .status("DELIVERED")
                    .title("Giao hàng thành công")
                    .description("Kiện hàng đã được giao thành công tới người nhận: " + (shipment.getRecipientName() != null ? shipment.getRecipientName() : "Khách hàng"))
                    .location(shipment.getToAddress() != null ? shipment.getToAddress() : "Địa chỉ nhận hàng")
                    .timestamp(m4Completed ? (shipment.getUpdatedAt() != null ? shipment.getUpdatedAt() : baseTime.plusHours(24)) : null)
                    .completed(m4Completed)
                    .current(m4Current)
                    .build());
        }

        return ShipmentTrackingResponse.builder()
                .id(shipment.getId())
                .subOrderId(shipment.getSubOrderId())
                .trackingCode(shipment.getTrackingCode())
                .carrierName(shipment.getCarrierName())
                .shippingFee(shipment.getShippingFee())
                .shippingStatus(shipment.getShippingStatus())
                .statusDescription(description)
                .recipientName(shipment.getRecipientName())
                .recipientPhone(shipment.getRecipientPhone())
                .fromAddress(shipment.getFromAddress())
                .toAddress(shipment.getToAddress())
                .weightGrams(shipment.getWeightGrams())
                .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .timeline(timeline)
                .build();
    }
}
