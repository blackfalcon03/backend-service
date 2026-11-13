package net.ey.backendservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to create a new order")
public class CreateOrderRequest {

    @NotBlank(message = "Customer ID is mandatory")
    @Schema(description = "Unique identifier of the customer", example = "CUST-5001")
    private String customerId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    @Schema(description = "List of items in the order")
    private List<OrderItemRequest> items;

    @Schema(description = "Optional promotional coupon code (e.g., SAVE10)", example = "SAVE10")
    private String couponCode;
}
