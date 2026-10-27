package com.quickshop.shipping.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.event.ShipmentStatusUpdatedEvent;
import com.quickshop.shipping.adapter.CarrierAdapter;
import com.quickshop.shipping.adapter.CarrierAdapterFactory;
import com.quickshop.shipping.adapter.CarrierShipmentResult;
import com.quickshop.shipping.config.RabbitMQShippingConfig;
import com.quickshop.shipping.dto.*;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.entity.ShippingWebhookLog;
import com.quickshop.shipping.entity.enums.ShippingStatus;
import com.quickshop.shipping.exception.*;
import com.quickshop.shipping.repository.ShipmentRepository;
import com.quickshop.shipping.repository.ShippingWebhookLogRepository;
import com.quickshop.shipping.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShippingWebhookLogRepository webhookLogRepository;
    private final CarrierAdapterFactory carrierAdapterFactory;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Optional<StringRedisTemplate> stringRedisTemplate;

    private static final String WEBHOOK_LOCK_PREFIX = "lock:shipping:webhook:event:";

    @Override
    public CalculateFeeResponse calculateShippingFee(CalculateFeeRequest request) {
        CarrierAdapter adapter = carrierAdapterFactory.getAdapter(request.getCarrierName());
        return adapter.calculateFee(request);
    }

    @Override
    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        log.info("Creating shipment for subOrderId={}, shopId={}", request.getSubOrderId(), request.getShopId());

        // 1. Kiểm tra chống tạo trùng shipment cho cùng Sub-Order
        if (shipmentRepository.existsBySubOrderId(request.getSubOrderId())) {
            log.warn("Shipment already exists for subOrderId={}", request.getSubOrderId());
            throw new DuplicateShipmentException("Vận đơn cho đơn hàng Sub-Order #" + request.getSubOrderId() + " đã tồn tại");
        }

        // 2. Lấy carrier adapter tương ứng (Mock Simulator hoặc GHN Production)
        CarrierAdapter adapter = carrierAdapterFactory.getAdapter(request.getCarrierName());
        CarrierShipmentResult carrierResult = adapter.createShipment(request);

        // 3. Khởi tạo thực thể Shipment
        Shipment shipment = Shipment.builder()
                .subOrderId(request.getSubOrderId())
                .shopId(request.getShopId())
                .trackingCode(carrierResult.getTrackingCode())
                .carrierName(carrierResult.getCarrierName())
                .shippingFee(carrierResult.getShippingFee())
                .shippingStatus(ShippingStatus.READY_TO_PICK)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .shippingAddress(request.getShippingAddress())
                .originAddress(request.getOriginAddress())
                .weightGrams(request.getWeightGrams() != null ? request.getWeightGrams() : 500)
                .codAmount(request.getCodAmount())
                .isMock(carrierResult.getIsMock())
                .statusNote("Vận đơn đã được khởi tạo, sẵn sàng lấy hàng")
                .build();

        try {
            shipment = shipmentRepository.save(shipment);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while saving shipment for subOrderId={}: {}", request.getSubOrderId(), e.getMessage());
            throw new DuplicateShipmentException("Vận đơn cho đơn hàng Sub-Order #" + request.getSubOrderId() + " đã tồn tại (ràng buộc cơ sở dữ liệu)");
        }

        // 4. Phát sự kiện cập nhật trạng thái vận đơn qua RabbitMQ cho Order Service
        publishStatusEvent(shipment, "Tạo vận đơn thành công");

        log.info("Shipment created successfully: id={}, trackingCode={}, subOrderId={}",
                shipment.getId(), shipment.getTrackingCode(), shipment.getSubOrderId());

        return mapToResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentBySubOrderId(Long subOrderId) {
        Shipment shipment = shipmentRepository.findBySubOrderId(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn cho Sub-Order #" + subOrderId));
        return mapToResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTrackingCode(String trackingCode) {
        Shipment shipment = shipmentRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn với mã: " + trackingCode));
        return mapToResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsByShopId(Long shopId) {
        return shipmentRepository.findByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShipmentResponse updateShippingStatus(String trackingCode, ShippingStatus newStatus, String note) {
        log.info("Updating shipment status: trackingCode={}, newStatus={}, note={}", trackingCode, newStatus, note);

        Shipment shipment = shipmentRepository.findByTrackingCodeForUpdate(trackingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn với mã: " + trackingCode));

        ShippingStatus currentStatus = shipment.getShippingStatus();

        if (currentStatus == newStatus) {
            log.info("Shipment {} is already at status {}. Skipping redundant state change.", trackingCode, newStatus);
            return mapToResponse(shipment);
        }

        // Kiểm tra tính hợp lệ của việc chuyển đổi trạng thái
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidShippingStatusException("Không thể chuyển trạng thái vận đơn từ " + currentStatus + " sang " + newStatus);
        }

        shipment.setShippingStatus(newStatus);
        if (note != null && !note.isBlank()) {
            shipment.setStatusNote(note);
        }
        shipment = shipmentRepository.save(shipment);

        // Phát sự kiện qua RabbitMQ
        publishStatusEvent(shipment, note);

        log.info("Shipment status updated: trackingCode={}, from={}, to={}", trackingCode, currentStatus, newStatus);
        return mapToResponse(shipment);
    }

    @Override
    @Transactional
    public void processWebhook(String carrier, Map<String, String> headers, String rawPayload, CarrierWebhookRequest request) {
        // 1. Nhận và validate các trường bắt buộc của webhook
        validateWebhookRequest(request);

        String eventId = request.getEventId().trim();
        String trackingCode = request.getTrackingCode().trim();
        String statusStr = request.getShippingStatus().trim().toUpperCase();

        log.info("[Webhook] Received webhook from carrier={}: eventId={}, trackingCode={}, status={}",
                carrier, eventId, trackingCode, statusStr);

        // Parse trạng thái vận đơn mục tiêu
        ShippingStatus targetStatus;
        try {
            targetStatus = ShippingStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            log.error("[Webhook] Invalid shipping_status in webhook payload: {}", statusStr);
            throw new InvalidShippingStatusException("Trạng thái vận đơn không hợp lệ trong webhook: " + statusStr);
        }

        // 2. Xác minh nguồn gửi theo khả năng của carrier adapter
        CarrierAdapter adapter = carrierAdapterFactory.getAdapter(carrier);
        if (!adapter.verifyWebhook(headers, rawPayload)) {
            log.warn("[Webhook] Unauthorized webhook signature/token from carrier={}", carrier);
            throw new InvalidWebhookSignatureException("Xác minh chữ ký / Token webhook thất bại từ đơn vị vận chuyển " + carrier);
        }

        // 3. Cơ chế chống race condition - Lớp 1: Khóa phân tán Redis Lock
        String lockKey = WEBHOOK_LOCK_PREFIX + eventId;
        boolean lockAcquired = false;
        try {
            if (stringRedisTemplate.isPresent()) {
                Boolean acquired = stringRedisTemplate.get().opsForValue()
                        .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(10));
                lockAcquired = Boolean.TRUE.equals(acquired);

                if (!lockAcquired) {
                    log.info("[Webhook] Concurrent request detected for eventId={}. Checking existing result...", eventId);
                    // Đợi ngắn và kiểm tra xem request đồng thời trước đó đã hoàn tất chưa
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    if (webhookLogRepository.existsByEventId(eventId)) {
                        log.info("[Webhook] EventId={} was completed by concurrent request. Returning idempotently.", eventId);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Webhook] Redis lock error, falling back to database unique constraint: {}", e.getMessage());
        }

        try {
            // 4. Cơ chế chống xử lý trùng - Lớp 2: Kiểm tra bản ghi trong bảng shipping_webhooks_log
            Optional<ShippingWebhookLog> existingLog = webhookLogRepository.findByEventId(eventId);
            if (existingLog.isPresent()) {
                ShippingWebhookLog logItem = existingLog.get();
                if ("SUCCESS".equalsIgnoreCase(logItem.getStatus())) {
                    log.info("[Webhook] EventId={} already processed successfully. Idempotent skip.", eventId);
                    return;
                }
            }

            // 5. Khóa bi quan và cập nhật trạng thái Shipment (Pessimistic Lock)
            Shipment shipment = shipmentRepository.findByTrackingCodeForUpdate(trackingCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn với mã: " + trackingCode));

            ShippingStatus currentStatus = shipment.getShippingStatus();

            // Nếu trạng thái đã trùng khớp với trạng thái hiện tại => Idempotent, không cần cập nhật lại
            if (currentStatus == targetStatus) {
                log.info("[Webhook] Shipment {} is already at status {}. Recording log and skipping update.", trackingCode, targetStatus);
                saveWebhookLogSafe(eventId, carrier, rawPayload, "SUCCESS", "Trạng thái đã là " + targetStatus);
                return;
            }

            // 6. Kiểm tra trạng thái chuyển tiếp hợp lệ (State Machine Invariants)
            if (!currentStatus.canTransitionTo(targetStatus)) {
                log.error("[Webhook] Invalid status transition from {} to {} for trackingCode={}",
                        currentStatus, targetStatus, trackingCode);
                saveWebhookLogSafe(eventId, carrier, rawPayload, "FAILED",
                        "Không thể chuyển trạng thái từ " + currentStatus + " sang " + targetStatus);
                throw new InvalidShippingStatusException("Không thể chuyển trạng thái vận đơn từ " + currentStatus + " sang " + targetStatus);
            }

            // Cập nhật trạng thái Shipment
            shipment.setShippingStatus(targetStatus);
            if (request.getDescription() != null && !request.getDescription().isBlank()) {
                shipment.setStatusNote(request.getDescription().trim());
            }
            shipmentRepository.save(shipment);

            // 7. Ghi nhận log webhook với trạng thái SUCCESS
            // Database-level Unique Constraint (uk_shipping_webhooks_event_id) đảm bảo chống race condition tầng DB
            saveWebhookLogSafe(eventId, carrier, rawPayload, "SUCCESS", null);

            // 8. Phát sự kiện đồng bộ trạng thái đơn hàng sang Order Service qua RabbitMQ
            publishStatusEvent(shipment, request.getDescription());

            log.info("[Webhook] Webhook eventId={} processed successfully. Shipment {} updated to {}",
                    eventId, trackingCode, targetStatus);

        } finally {
            // Giải phóng khóa Redis an toàn
            if (lockAcquired && stringRedisTemplate.isPresent()) {
                try {
                    stringRedisTemplate.get().delete(lockKey);
                } catch (Exception e) {
                    log.warn("[Webhook] Failed to release Redis lock {}: {}", lockKey, e.getMessage());
                }
            }
        }
    }

    private void validateWebhookRequest(CarrierWebhookRequest request) {
        if (request == null) {
            throw new InvalidWebhookPayloadException("Dữ liệu webhook không được để trống");
        }
        if (request.getEventId() == null || request.getEventId().trim().isEmpty()) {
            throw new InvalidWebhookPayloadException("Trường 'event_id' là bắt buộc để đảm bảo tính Idempotency");
        }
        if (request.getTrackingCode() == null || request.getTrackingCode().trim().isEmpty()) {
            throw new InvalidWebhookPayloadException("Trường 'tracking_code' không được để trống");
        }
        if (request.getShippingStatus() == null || request.getShippingStatus().trim().isEmpty()) {
            throw new InvalidWebhookPayloadException("Trường 'shipping_status' không được để trống");
        }
    }

    private void saveWebhookLogSafe(String eventId, String carrier, String rawPayload, String status, String errorMessage) {
        try {
            ShippingWebhookLog logEntry = ShippingWebhookLog.builder()
                    .eventId(eventId)
                    .carrierName(carrier)
                    .payload(rawPayload != null ? rawPayload : "{}")
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
            webhookLogRepository.save(logEntry);
        } catch (DataIntegrityViolationException e) {
            // Bắt trường hợp hai request đồng thời cùng chèn một event_id
            log.info("[Webhook] Duplicate event_id detected by database unique constraint for eventId={}. Handled idempotently.", eventId);
        }
    }

    private void publishStatusEvent(Shipment shipment, String note) {
        try {
            ShipmentStatusUpdatedEvent event = ShipmentStatusUpdatedEvent.builder()
                    .subOrderId(shipment.getSubOrderId())
                    .trackingCode(shipment.getTrackingCode())
                    .carrierName(shipment.getCarrierName())
                    .shippingStatus(shipment.getShippingStatus().name())
                    .shippingFee(shipment.getShippingFee())
                    .note(note)
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQShippingConfig.EXCHANGE_SHIPPING,
                    RabbitMQShippingConfig.ROUTING_KEY_SHIPPING_STATUS_UPDATED,
                    event
            );
            log.info("Published ShipmentStatusUpdatedEvent to RabbitMQ: subOrderId={}, status={}",
                    shipment.getSubOrderId(), shipment.getShippingStatus());
        } catch (Exception e) {
            log.error("Failed to publish ShipmentStatusUpdatedEvent to RabbitMQ for subOrderId={}: {}",
                    shipment.getSubOrderId(), e.getMessage());
        }
    }

    private ShipmentResponse mapToResponse(Shipment s) {
        return ShipmentResponse.builder()
                .id(s.getId())
                .subOrderId(s.getSubOrderId())
                .shopId(s.getShopId())
                .trackingCode(s.getTrackingCode())
                .carrierName(s.getCarrierName())
                .shippingFee(s.getShippingFee())
                .shippingStatus(s.getShippingStatus())
                .recipientName(s.getRecipientName())
                .recipientPhone(s.getRecipientPhone())
                .shippingAddress(s.getShippingAddress())
                .originAddress(s.getOriginAddress())
                .weightGrams(s.getWeightGrams())
                .codAmount(s.getCodAmount())
                .isMock(s.getIsMock())
                .statusNote(s.getStatusNote())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
