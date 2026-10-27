package com.quickshop.payment.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.payment.constant.LedgerAccount;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.enums.EscrowStatus;
import com.quickshop.payment.entity.enums.LedgerTransactionType;
import com.quickshop.payment.exception.DisputedEscrowException;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.service.DoubleEntryLedgerService;
import com.quickshop.payment.service.EscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private final EscrowAccountRepository escrowAccountRepository;
    private final WalletRepository walletRepository;
    private final DoubleEntryLedgerService doubleEntryLedgerService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${quickshop.platform.commission-rate:0.03}")
    private BigDecimal commissionRate = new BigDecimal("0.03"); // 3% hoa hồng sàn

    @Override
    @Transactional
    public EscrowResponse holdEscrow(HoldEscrowRequest request) {
        log.info("[Escrow Hold][SubOrderID: {}][MasterOrderID: {}] Bắt đầu tạm giữ số tiền: {} VND cho Shop #{}",
                request.getSubOrderId(), request.getMasterOrderId(), request.getAmount(), request.getShopId());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền tạm giữ Escrow phải lớn hơn 0");
        }

        // Kiểm tra tính Idempotent: Nếu đã tồn tại Escrow cho SubOrder này
        Optional<EscrowAccount> existingOpt = escrowAccountRepository.findBySubOrderId(request.getSubOrderId());
        if (existingOpt.isPresent()) {
            EscrowAccount existing = existingOpt.get();
            if (existing.getStatus() == EscrowStatus.HOLDING && existing.getAmount().compareTo(request.getAmount()) == 0) {
                log.info("[Escrow Hold][SubOrderID: {}] Escrow đã được giữ từ trước. Trả về thông tin hiện tại.",
                        request.getSubOrderId());
                return mapToResponse(existing);
            }
            throw new IllegalStateException("Sub-Order " + request.getSubOrderId() + " đã có tài khoản Escrow với trạng thái: " + existing.getStatus());
        }

        // Tính phí hoa hồng sàn ước tính
        BigDecimal estimatedCommission = request.getAmount()
                .multiply(commissionRate)
                .setScale(0, RoundingMode.HALF_UP);

        EscrowAccount escrow = EscrowAccount.builder()
                .subOrderId(request.getSubOrderId())
                .masterOrderId(request.getMasterOrderId())
                .shopId(request.getShopId())
                .amount(request.getAmount())
                .commissionFee(estimatedCommission)
                .status(EscrowStatus.HOLDING)
                .isDisputed(false)
                .build();

        EscrowAccount savedEscrow = escrowAccountRepository.save(escrow);

        // Ghi bút toán kép vào Sổ cái: Nợ CUSTOMER_PAYMENT | Có SYSTEM_ESCROW
        String txId = "ESCROW_HOLD_SUB_" + request.getSubOrderId();
        doubleEntryLedgerService.recordEntry(
                txId,
                LedgerTransactionType.ESCROW_HOLD,
                LedgerAccount.CUSTOMER_PAYMENT,
                LedgerAccount.SYSTEM_ESCROW,
                request.getAmount(),
                "Tạm giữ tiền thanh toán cho Sub-Order #" + request.getSubOrderId()
        );

        log.info("[Escrow Hold][SubOrderID: {}] Tạm giữ Escrow và ghi sổ cái thành công. Số tiền: {} VND",
                request.getSubOrderId(), request.getAmount());

        return mapToResponse(savedEscrow);
    }

    @Override
    @Transactional
    public EscrowResponse releaseEscrow(ReleaseEscrowRequest request) {
        Long subOrderId = request.getSubOrderId();
        log.info("[Escrow Release][SubOrderID: {}] Bắt đầu giải phóng Escrow với Trigger: {}", subOrderId, request.getTrigger());

        String lockKey = "lock:escrow:" + subOrderId;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(acquired)) {
            throw new IllegalStateException("Yêu cầu thao tác Escrow Sub-Order " + subOrderId + " đang được xử lý đồng thời.");
        }

        try {
            // Khóa bi quan (Pessimistic Write Lock) trên CSDL
            EscrowAccount escrow = escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Escrow cho Sub-Order: " + subOrderId));

            // Kiểm tra Idempotent: Nếu đã được giải phóng từ trước
            if (escrow.getStatus() == EscrowStatus.RELEASED) {
                log.info("[Escrow Release][SubOrderID: {}] Escrow đã được giải phóng từ trước. Bỏ qua thao tác lặp.", subOrderId);
                return mapToResponse(escrow);
            }

            if (escrow.getStatus() != EscrowStatus.HOLDING) {
                throw new IllegalStateException("Chỉ có thể giải phóng Escrow ở trạng thái HOLDING. Trạng thái hiện tại: " + escrow.getStatus());
            }

            // Yêu cầu 10: Đóng băng giải phóng Escrow khi Sub-Order có tranh chấp / khiếu nại
            if (escrow.isDisputed()) {
                log.warn("[Escrow Release][SubOrderID: {}] Không thể giải phóng Escrow vì đang có tranh chấp: {}",
                        subOrderId, escrow.getDisputeReason());
                throw new DisputedEscrowException("Sub-Order " + subOrderId + " đang có khiếu nại/tranh chấp chưa xử lý! Đã đóng băng giải phóng tiền.");
            }

            // Tính hoa hồng sàn chuẩn xác: commissionFee = amount * commissionRate
            BigDecimal commissionFee = escrow.getAmount()
                    .multiply(commissionRate)
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal netSellerAmount = escrow.getAmount().subtract(commissionFee);

            // 1. Cập nhật Escrow sang RELEASED
            escrow.setStatus(EscrowStatus.RELEASED);
            escrow.setCommissionFee(commissionFee);
            escrow.setReleasedAt(LocalDateTime.now());
            escrowAccountRepository.save(escrow);

            // 2. Cập nhật Ví Seller (Lock bi quan ví của Shop)
            Long shopId = escrow.getShopId();
            Wallet wallet = walletRepository.findByShopIdForUpdate(shopId)
                    .orElseGet(() -> {
                        log.info("[Escrow Release] Khởi tạo ví mới cho Shop #{}", shopId);
                        Wallet newWallet = Wallet.builder()
                                .shopId(shopId)
                                .balance(BigDecimal.ZERO)
                                .frozenBalance(BigDecimal.ZERO)
                                .build();
                        return walletRepository.save(newWallet);
                    });

            wallet.setBalance(wallet.getBalance().add(netSellerAmount));
            walletRepository.save(wallet);

            // 3. Ghi 2 bút toán kép vào Sổ cái
            // Bút toán 1: Tiền bán hàng về Ví Seller (Nợ SYSTEM_ESCROW | Có SELLER_WALLET)
            String releaseTxId = "ESCROW_RELEASE_SUB_" + subOrderId;
            doubleEntryLedgerService.recordEntry(
                    releaseTxId,
                    LedgerTransactionType.ESCROW_RELEASE,
                    LedgerAccount.SYSTEM_ESCROW,
                    LedgerAccount.sellerWallet(shopId),
                    netSellerAmount,
                    "Giải phóng tiền hàng Sub-Order #" + subOrderId + " cho Shop #" + shopId
            );

            // Bút toán 2: Thu hoa hồng Sàn (Nợ SYSTEM_ESCROW | Có PLATFORM_REVENUE)
            String commissionTxId = "COMMISSION_FEE_SUB_" + subOrderId;
            doubleEntryLedgerService.recordEntry(
                    commissionTxId,
                    LedgerTransactionType.COMMISSION_FEE,
                    LedgerAccount.SYSTEM_ESCROW,
                    LedgerAccount.PLATFORM_REVENUE,
                    commissionFee,
                    "Thu phí hoa hồng sàn (3%) cho Sub-Order #" + subOrderId
            );

            log.info("[Escrow Release][SubOrderID: {}] Giải phóng Escrow THÀNH CÔNG! Thực nhận Seller: {} VND, Hoa hồng: {} VND",
                    subOrderId, netSellerAmount, commissionFee);

            return mapToResponse(escrow);

        } finally {
            try {
                String current = stringRedisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(current)) {
                    stringRedisTemplate.delete(lockKey);
                }
            } catch (Exception e) {
                log.error("[Escrow Release] Lỗi giải phóng lock Redis: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public EscrowResponse refundEscrow(RefundEscrowRequest request) {
        Long subOrderId = request.getSubOrderId();
        BigDecimal refundAmount = request.getRefundAmount();
        log.info("[Escrow Refund][SubOrderID: {}] Bắt đầu hoàn tiền Escrow: {} VND. Lý do: {}",
                subOrderId, refundAmount, request.getReason());

        String lockKey = "lock:escrow:" + subOrderId;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(acquired)) {
            throw new IllegalStateException("Yêu cầu hoàn tiền Escrow Sub-Order " + subOrderId + " đang được xử lý.");
        }

        try {
            EscrowAccount escrow = escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Escrow cho Sub-Order: " + subOrderId));

            if (escrow.getStatus() == EscrowStatus.REFUNDED) {
                log.info("[Escrow Refund][SubOrderID: {}] Escrow đã được hoàn tiền từ trước.", subOrderId);
                return mapToResponse(escrow);
            }

            if (escrow.getStatus() != EscrowStatus.HOLDING) {
                throw new IllegalStateException("Chỉ có thể hoàn tiền Escrow ở trạng thái HOLDING. Trạng thái hiện tại: " + escrow.getStatus());
            }

            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(escrow.getAmount()) > 0) {
                throw new BadRequestException("Số tiền hoàn không hợp lệ: " + refundAmount + " (Tổng giữ: " + escrow.getAmount() + ")");
            }

            // Cập nhật trạng thái Escrow sang REFUNDED
            escrow.setStatus(EscrowStatus.REFUNDED);
            escrow.setRefundedAt(LocalDateTime.now());
            escrow.setDisputed(false); // Đã giải quyết xong tranh chấp
            escrowAccountRepository.save(escrow);

            // Ghi bút toán kép Sổ cái: Nợ SYSTEM_ESCROW | Có CUSTOMER_REFUND
            String refundTxId = "REFUND_SUB_" + subOrderId;
            doubleEntryLedgerService.recordEntry(
                    refundTxId,
                    LedgerTransactionType.REFUND,
                    LedgerAccount.SYSTEM_ESCROW,
                    LedgerAccount.CUSTOMER_REFUND,
                    refundAmount,
                    "Hoàn tiền cho khách hàng Sub-Order #" + subOrderId + ": " + request.getReason()
            );

            log.info("[Escrow Refund][SubOrderID: {}] Hoàn tiền Escrow THÀNH CÔNG! Số tiền: {} VND", subOrderId, refundAmount);

            return mapToResponse(escrow);

        } finally {
            try {
                String current = stringRedisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(current)) {
                    stringRedisTemplate.delete(lockKey);
                }
            } catch (Exception e) {
                log.error("[Escrow Refund] Lỗi giải phóng lock Redis: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public EscrowResponse freezeEscrowForDispute(Long subOrderId, String reason) {
        log.info("[Escrow Dispute][SubOrderID: {}] Tiếp nhận khiếu nại tranh chấp. Đóng băng Escrow. Lý do: {}",
                subOrderId, reason);

        EscrowAccount escrow = escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Escrow cho Sub-Order: " + subOrderId));

        if (escrow.getStatus() != EscrowStatus.HOLDING) {
            throw new IllegalStateException("Không thể đóng băng Escrow khi trạng thái không phải HOLDING. Trạng thái: " + escrow.getStatus());
        }

        escrow.setDisputed(true);
        escrow.setDisputeReason(reason);
        escrow.setDisputedAt(LocalDateTime.now());

        EscrowAccount saved = escrowAccountRepository.save(escrow);
        log.info("[Escrow Dispute][SubOrderID: {}] Đã đóng băng giải phóng Escrow thành công.", subOrderId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public EscrowResponse unfreezeEscrow(Long subOrderId) {
        log.info("[Escrow Unfreeze][SubOrderID: {}] Bác bỏ/Giải quyết tranh chấp không hoàn tiền. Mở đóng băng Escrow.", subOrderId);

        EscrowAccount escrow = escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Escrow cho Sub-Order: " + subOrderId));

        escrow.setDisputed(false);
        escrow.setDisputeReason(null);

        EscrowAccount saved = escrowAccountRepository.save(escrow);
        log.info("[Escrow Unfreeze][SubOrderID: {}] Đã mở đóng băng Escrow thành công.", subOrderId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public EscrowResponse markSubOrderDelivered(Long subOrderId, LocalDateTime deliveredAt) {
        EscrowAccount escrow = escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Escrow cho Sub-Order: " + subOrderId));

        escrow.setDeliveredAt(deliveredAt != null ? deliveredAt : LocalDateTime.now());
        EscrowAccount saved = escrowAccountRepository.save(escrow);
        log.info("[Escrow Delivered][SubOrderID: {}] Đã ghi nhận thời gian DELIVERED: {}", subOrderId, escrow.getDeliveredAt());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public int autoReleaseEligibleEscrows() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(3);
        log.info("[Escrow Auto-Release] Quét các Escrow DELIVERED trước {} (quá 3 ngày) không có khiếu nại...", thresholdTime);

        List<EscrowAccount> eligibleList = escrowAccountRepository.findEligibleForAutoRelease(thresholdTime);
        int count = 0;

        for (EscrowAccount escrow : eligibleList) {
            try {
                releaseEscrow(ReleaseEscrowRequest.builder()
                        .subOrderId(escrow.getSubOrderId())
                        .trigger("AUTO_SCHEDULED_DELIVERED_3_DAYS")
                        .build());
                count++;
            } catch (Exception e) {
                log.error("[Escrow Auto-Release][SubOrderID: {}] Lỗi giải phóng tự động: {}",
                        escrow.getSubOrderId(), e.getMessage());
            }
        }

        log.info("[Escrow Auto-Release] Đã giải phóng tự động {} Sub-Orders thành công.", count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public EscrowResponse getEscrowBySubOrderId(Long subOrderId) {
        EscrowAccount escrow = escrowAccountRepository.findBySubOrderId(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Escrow cho Sub-Order: " + subOrderId));
        return mapToResponse(escrow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscrowResponse> getEscrowsByMasterOrderId(Long masterOrderId) {
        return escrowAccountRepository.findByMasterOrderId(masterOrderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private EscrowResponse mapToResponse(EscrowAccount entity) {
        BigDecimal commission = entity.getCommissionFee() != null ? entity.getCommissionFee() : BigDecimal.ZERO;
        BigDecimal netSeller = entity.getAmount().subtract(commission);

        return EscrowResponse.builder()
                .id(entity.getId())
                .subOrderId(entity.getSubOrderId())
                .masterOrderId(entity.getMasterOrderId())
                .shopId(entity.getShopId())
                .amount(entity.getAmount())
                .commissionFee(commission)
                .netSellerAmount(netSeller)
                .status(entity.getStatus())
                .isDisputed(entity.isDisputed())
                .disputeReason(entity.getDisputeReason())
                .disputedAt(entity.getDisputedAt())
                .deliveredAt(entity.getDeliveredAt())
                .releasedAt(entity.getReleasedAt())
                .refundedAt(entity.getRefundedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
