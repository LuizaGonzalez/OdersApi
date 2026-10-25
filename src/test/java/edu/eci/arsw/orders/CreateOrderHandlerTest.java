package edu.eci.arsw.orders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateOrderHandlerTest {
    @Test
    void shouldCreatesOrderWithoutAwsContext() {
        CreateOrderHandler handler = new CreateOrderHandler();

        Order order = handler.handleRequest(new CreateOrderRequest("CUST-42", "MacBook Air", 1), null);

        assertEquals("ORD-001", order.getId());
    }
}
