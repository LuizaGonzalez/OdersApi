package edu.eci.arsw.orders;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService();
    }

    @Test
    void shouldCreatesValidOrder() {
        Order order = service.createOrder(new CreateOrderRequest("CUST-42", "Lavadora", 1));

        assertEquals("ORD-001", order.getId());
        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("Lavadora", order.getProduct());
        assertEquals(1, order.getQuantity());
    }

    @Test
    void shouldRetrievesExistingOrder() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-42", "Celular", 2));

        Order found = service.getOrder(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Celular", found.getProduct());
    }

    @Test
    void souldFailsWhenOrderDoesNotExist() {
        OrderNotFoundException ex =
                assertThrows(OrderNotFoundException.class, () -> service.getOrder("ORD-999"));
        assertEquals("Order ORD-999 does not exist", ex.getMessage());
    }

    @Test
    void shouldFindsOrdersByCustomer() {
        service.createOrder(new CreateOrderRequest("CUST-42", "Lavadora", 1));
        service.createOrder(new CreateOrderRequest("CUST-7", "Celular", 2));
        service.createOrder(new CreateOrderRequest("CUST-42", "Mouse", 3));

        List<Order> result = service.getOrdersByCustomer("CUST-42");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getCustomerId().equals("CUST-42")));
    }

    @Test
    void shouldReturnsEmptyListWhenCustomerHasNoOrders() {
        assertTrue(service.getOrdersByCustomer("CUST-100").isEmpty());
    }

    @Test
    void shouldRejectsInvalidInput() {
        assertThrows(InvalidOrderException.class, () -> service.createOrder(null));
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("", "Mouse", 1)));
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-1", " ", 1)));
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-1", "Mouse", 0)));
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-1", "Mouse", -5)));
    }

    @Test
    void ShouldRejectsMissingCustomerIdInSearch() {
        assertThrows(InvalidOrderException.class, () -> service.getOrdersByCustomer(null));
        assertThrows(InvalidOrderException.class, () -> service.getOrdersByCustomer(" "));
    }
}