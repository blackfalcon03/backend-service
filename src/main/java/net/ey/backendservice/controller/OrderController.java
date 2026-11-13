package net.ey.backendservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ey.backendservice.dto.CreateOrderRequest;
import net.ey.backendservice.dto.OrderResponse;
import net.ey.backendservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for creating, fetching, and cancelling orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an order", description = "Validates the input items, applies coupon discounts if provided, and creates an order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order successfully created",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or coupon code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("[REST POST /api/v1/orders] Incoming create order request for customer: [{}]", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        log.info("[REST POST /api/v1/orders] Successfully processed. Created Order ID: [{}]", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get order by ID", description = "Retrieves the details of an existing order by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", example = "ORD-ABC12345")
            @PathVariable("id") String id) {
        log.debug("[REST GET /api/v1/orders/{}] Request received", id);
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all orders", description = "Retrieves all recorded orders.")
    @ApiResponse(responseCode = "200", description = "List of orders",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    public ResponseEntity<List<OrderResponse>> listOrders() {
        log.debug("[REST GET /api/v1/orders] Listing all orders");
        return ResponseEntity.ok(orderService.listOrders());
    }

    @PostMapping(value = "/{id}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cancel an order", description = "Cancels an existing order if it is not completed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order successfully cancelled",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID", example = "ORD-ABC12345")
            @PathVariable("id") String id) {
        log.info("[REST POST /api/v1/orders/{}/cancel] Processing cancellation request", id);
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }
}
