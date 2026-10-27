package com.quickshop.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierWebhookRequest {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("carrier_name")
    private String carrierName;

    @JsonProperty("tracking_code")
    private String trackingCode;

    @JsonProperty("shipping_status")
    private String shippingStatus;

    @JsonProperty("description")
    private String description;

    @JsonProperty("raw_data")
    private Map<String, Object> rawData;
}
