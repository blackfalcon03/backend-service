package net.ey.backendservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.ey.backendservice.dto.CreateOrderRequest;
import net.ey.backendservice.dto.OrderItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Positive: Successfully create order and apply 10% coupon (SAVE10)")
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-1001")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-001")
                                .quantity(2)
                                .unitPrice(new BigDecimal("25.00"))
                                .build()
                ))
                .couponCode("SAVE10")
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId", is("CUST-1001")))
                .andExpect(jsonPath("$.totalAmount", is(45.00)))
                .andExpect(jsonPath("$.status", is("CREATED")));
    }

    @Test
    @DisplayName("Positive: Successfully create order and apply 20% coupon (SAVE20)")
    void shouldCreateOrderWithSave20Coupon() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-1003")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-003")
                                .quantity(1)
                                .unitPrice(new BigDecimal("100.00"))
                                .build()
                ))
                .couponCode("SAVE20")
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount", is(80.00)))
                .andExpect(jsonPath("$.status", is("CREATED")));
    }

    @Test
    @DisplayName("Negative: Return 400 Bad Request when customer ID is blank")
    void shouldFailWhenCustomerIdMissing() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-001")
                                .quantity(1)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.errors.customerId").exists());
    }

    @Test
    @DisplayName("Negative: Return 400 Bad Request when items list is empty")
    void shouldFailWhenItemsListIsEmpty() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-1004")
                .items(Collections.emptyList())
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    @DisplayName("Negative: Return 400 Bad Request when item quantity is less than 1")
    void shouldFailWhenItemQuantityIsInvalid() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-1005")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-001")
                                .quantity(0)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    @Test
    @DisplayName("Negative: Return 400 Bad Request when unit price is negative")
    void shouldFailWhenItemPriceIsNegative() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-1006")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-001")
                                .quantity(1)
                                .unitPrice(new BigDecimal("-5.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }

    @Test
    @DisplayName("Negative: Return 400 Bad Request when unknown coupon code is supplied")
    void shouldFailWhenInvalidCouponCodeProvided() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-1007")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-001")
                                .quantity(1)
                                .unitPrice(new BigDecimal("50.00"))
                                .build()
                ))
                .couponCode("UNKNOWN_CODE")
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Argument")))
                .andExpect(jsonPath("$.detail", containsString("Unknown coupon code")));
    }

    @Test
    @DisplayName("Positive: Retrieve existing order by ID")
    void shouldGetOrderById() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-1002")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-002")
                                .quantity(1)
                                .unitPrice(new BigDecimal("99.99"))
                                .build()
                ))
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId)))
                .andExpect(jsonPath("$.customerId", is("CUST-1002")));
    }

    @Test
    @DisplayName("Positive: Successfully cancel an order")
    void shouldCancelOrderSuccessfully() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-1008")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-008")
                                .quantity(1)
                                .unitPrice(new BigDecimal("30.00"))
                                .build()
                ))
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId)))
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("Negative: Return 404 for unknown order ID")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "ORD-DOES-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Order Not Found")));
    }
}
