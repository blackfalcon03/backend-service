package net.ey.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import net.ey.backendservice.dto.CreateOrderRequest;
import net.ey.backendservice.dto.OrderItemRequest;
import net.ey.backendservice.dto.OrderResponse;
import net.ey.backendservice.exception.OrderNotFoundException;
import net.ey.backendservice.model.Order;
import net.ey.backendservice.model.OrderItem;
import net.ey.backendservice.model.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OrderService {

    private final Map<String, Order> orderRepository = new ConcurrentHashMap<>();

    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("[OrderService] Initiating order creation for customerId: [{}], itemsCount: [{}], coupon: [{}]",
                request.getCustomerId(), request.getItems() != null ? request.getItems().size() : 0, request.getCouponCode());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            BigDecimal itemTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemTotal);
            log.debug("[OrderService] Item [{}]: quantity={}, unitPrice={}, itemTotal={}",
                    itemReq.getProductId(), itemReq.getQuantity(), itemReq.getUnitPrice(), itemTotal);

            items.add(OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .build());
        }

        BigDecimal totalAmount = applyDiscount(subtotal, request.getCouponCode());
        log.info("[OrderService] Subtotal: [{}], Final Total after discount: [{}]", subtotal, totalAmount);

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
                .id(orderId)
                .customerId(request.getCustomerId())
                .items(items)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .build();

        orderRepository.put(orderId, order);
        log.info("[OrderService] Successfully created order with ID: [{}]", orderId);
        return mapToResponse(order);
    }

    public OrderResponse getOrderById(String orderId) {
        log.debug("[OrderService] Fetching order by ID: [{}]", orderId);
        Order order = orderRepository.get(orderId);
        if (order == null) {
            log.warn("[OrderService] Order lookup failed - ID not found: [{}]", orderId);
            throw new OrderNotFoundException(orderId);
        }
        return mapToResponse(order);
    }

    public List<OrderResponse> listOrders() {
        log.debug("[OrderService] Listing all orders. Total recorded: [{}]", orderRepository.size());
        return orderRepository.values().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public OrderResponse cancelOrder(String orderId) {
        log.info("[OrderService] Request to cancel order: [{}]", orderId);
        Order order = orderRepository.get(orderId);
        if (order == null) {
            log.warn("[OrderService] Cancel failed - order not found: [{}]", orderId);
            throw new OrderNotFoundException(orderId);
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.error("[OrderService] Cancel rejected - order [{}] is already COMPLETED", orderId);
            throw new IllegalStateException("Completed order cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        log.info("[OrderService] Order [{}] successfully marked as CANCELLED", orderId);
        return mapToResponse(order);
    }

    private BigDecimal applyDiscount(BigDecimal subtotal, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return subtotal.setScale(2, RoundingMode.HALF_UP);
        }

        String normalizedCoupon = couponCode.toUpperCase().trim();
        log.debug("[OrderService] Validating and applying coupon code: [{}]", normalizedCoupon);

        // Feature toggle for SDET AI AutoTriage demo / failure injection
        String simBugEnv = System.getenv("SIMULATE_BUG");
        boolean simulateBug = simBugEnv != null ? Boolean.parseBoolean(simBugEnv) : Boolean.getBoolean("SIMULATE_BUG");
        if (simulateBug && "SAVE10".equals(normalizedCoupon)) {
            log.warn("[OrderService] ⚠️ DEMO REGRESSION ACTIVE (SIMULATE_BUG=true): Applying 15% discount (0.85) instead of expected 10% (0.90)!");
            return subtotal.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);
        }

        return switch (normalizedCoupon) {
            case "SAVE10" -> subtotal.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);
            case "SAVE20" -> subtotal.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
            default -> {
                log.warn("[OrderService] Unrecognized coupon code rejected: [{}]", couponCode);
                throw new IllegalArgumentException("Unknown coupon code: " + couponCode);
            }
        };
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .items(order.getItems())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
