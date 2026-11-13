package net.ey.backendservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for an individual order item")
public class OrderItemRequest {

    @NotBlank(message = "Product ID is mandatory")
    @Schema(description = "Unique identifier of the product", example = "PROD-1001")
    private String productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the product", example = "2")
    private int quantity;

    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    @Schema(description = "Price per unit", example = "29.99")
    private BigDecimal unitPrice;
}
