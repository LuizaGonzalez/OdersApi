package edu.eci.arsw.orders;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderService {
    private final Map<String,Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(0);

    public static OrderService withSampleData(){
        OrderService service = new OrderService();
        service.createOrder(new CreateOrderRequest("CUST-42", "Lavadora", 1));
        service.createOrder(new CreateOrderRequest("CUST-7", "Celular", 2));
        return service;
    }
    public Order createOrder(CreateOrderRequest request){
        validate(request);
        String id = String.format("ORD-%03d", sequence.incrementAndGet());
        Order order = new Order(id, request.getCustomerId().trim(),request.getProduct().trim(), request.getQuantity());
        orders.put(id, order);
        return order;
    }
    public Order getOrder(String id) {
        if (id == null || id.isBlank()) {
            throw new InvalidOrderException("Order id is required");
        }
        Order order = orders.get(id);
        if (order == null) {
            throw new OrderNotFoundException(id);
        }
        return order;
    }
    public List<Order> getOrdersByCustomer(String customerId){
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException("customerId is required");
        }
        return orders.values().stream()
                .filter(o -> o.getCustomerId().equals(customerId))
                .sorted(Comparator.comparing(Order::getId))
                .toList();
    }
    private void validate(CreateOrderRequest r) {
        if (r == null) {
            throw new InvalidOrderException("Request body is required");
        }
        if (r.getCustomerId() == null || r.getCustomerId().isBlank()) {
            throw new InvalidOrderException("customerId is required");
        }
        if (r.getProduct() == null || r.getProduct().isBlank()) {
            throw new InvalidOrderException("product is required");
        }
        if (r.getQuantity() <= 0) {
            throw new InvalidOrderException("quantity must be greater than 0");
        }
    }
}
