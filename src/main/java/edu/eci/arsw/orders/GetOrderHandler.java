package edu.eci.arsw.orders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class GetOrderHandler implements RequestHandler<GetOrderRequest, Order> {
    private final OrderService service = OrderService.withSampleData();

    @Override
    public Order handleRequest(GetOrderRequest request, Context context) {
        return service.getOrder(request.getId());
    }
}
