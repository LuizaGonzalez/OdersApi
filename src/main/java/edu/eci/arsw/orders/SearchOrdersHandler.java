package edu.eci.arsw.orders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.List;

public class SearchOrdersHandler implements RequestHandler<SearchOrdersRequest, List<Order>> {
    private final OrderService service = OrderService.withSampleData();

    @Override
    public List<Order> handleRequest(SearchOrdersRequest request, Context context) {
        return service.getOrdersByCustomer(request.getCustomerId());
    }
}
