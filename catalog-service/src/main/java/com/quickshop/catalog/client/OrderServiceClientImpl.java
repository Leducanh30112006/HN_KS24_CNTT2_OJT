package com.quickshop.catalog.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceClientImpl implements OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${order.service.url:http://localhost:8083}")
    private String orderServiceUrl;

    @Value("${catalog.review.enforce-order-completed:true}")
    private boolean enforceOrderCompleted;

    @Override
    public boolean isSubOrderCompleted(Long subOrderId, Long userId, Long productId) {
        if (!enforceOrderCompleted) {
            return true;
        }
        if (subOrderId == null || subOrderId <= 0) {
            return false;
        }
        try {
            String url = String.format("%s/api/orders/sub-orders/%d/review-eligibility?userId=%s&productId=%s",
                    orderServiceUrl, subOrderId,
                    userId != null ? userId : "",
                    productId != null ? productId : "");

            OrderEligibilityResponse response = restTemplate.getForObject(url, OrderEligibilityResponse.class);
            return response != null && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.warn("Không thể xác thực trạng thái SubOrder #{} từ Order Service: {}. Cho phép fallback.", subOrderId, e.getMessage());
            // Fallback: nếu order-service chưa bật thì fallback an toàn
            return true;
        }
    }
}
