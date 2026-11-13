package net.ey.backendservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.ey.backendservice.model.OrderItem;
import net.ey.backendservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response representation of an order")
public class OrderResponse {

    @Schema(description = "Unique identifier of the order", example = "ORD-987654")
    private String id;

    @Schema(description = "Unique identifier of the customer", example = "CUST-5001")
    private String customerId;

    @Schema(description = "Items included in the order")
    private List<OrderItem> items;

    @Schema(description = "Final calculated total amount after any discounts", example = "53.98")
    private BigDecimal totalAmount;

    @Schema(description = "Current lifecycle status of the order", example = "CREATED")
    private OrderStatus status;

    @Schema(description = "Timestamp when the order was placed")
    private Instant createdAt;
}
