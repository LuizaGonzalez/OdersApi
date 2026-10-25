package edu.eci.arsw.orders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class CreateOrderHandler implements RequestHandler<CreateOrderRequest, Order> {
    private final OrderService service = new OrderService();

    @Override
    public Order handleRequest(CreateOrderRequest request, Context context) {
        return service.createOrder(request);
    }
}

