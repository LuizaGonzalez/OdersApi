package edu.eci.arsw.orders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Adaptador HTTP: es la unica clase que conoce API Gateway y HTTP.
 * Traduce el evento proxy a llamadas al OrderService y las excepciones de dominio a codigos de estado.
 */
public class OrdersApiHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrderService service;

    public OrdersApiHandler() {
        this(OrderService.withSampleData());
    }

    public OrdersApiHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            String method = event.getHttpMethod();
            String resource = event.getResource();

            if ("/orders".equals(resource)) {
                if ("POST".equals(method)) return createOrder(event);
                if ("GET".equals(method)) return searchOrders(event);
            } else if ("/orders/{id}".equals(resource)) {
                if ("GET".equals(method)) return getOrder(event);
                if ("DELETE".equals(method)) return deleteOrder(event);
            }
            return error(404, "ROUTE_NOT_FOUND", "No route for " + method + " " + event.getPath());

        } catch (OrderNotFoundException e) {
            return error(404, "ORDER_NOT_FOUND", e.getMessage());
        } catch (InvalidOrderException e) {
            return error(400, "INVALID_REQUEST", e.getMessage());
        } catch (JsonProcessingException e) {
            return error(400, "INVALID_REQUEST", "Request body is not valid JSON for this operation");
        } catch (Exception e) {
            if (context != null) {
                context.getLogger().log("Unexpected error: " + e);
            }
            return error(500, "INTERNAL_ERROR", "Unexpected internal error");
        }
    }

    private APIGatewayProxyResponseEvent createOrder(APIGatewayProxyRequestEvent event)
            throws JsonProcessingException {
        String body = event.getBody();
        if (body == null || body.isBlank()) {
            throw new InvalidOrderException("Request body is required");
        }
        CreateOrderRequest request = MAPPER.readValue(body, CreateOrderRequest.class);
        Order order = service.createOrder(request);
        return json(201, order).withHeaders(Map.of(
                "Content-Type", "application/json",
                "Location", "/orders/" + order.getId()));
    }

    private APIGatewayProxyResponseEvent getOrder(APIGatewayProxyRequestEvent event)
            throws JsonProcessingException {
        return json(200, service.getOrder(pathId(event)));
    }

    private APIGatewayProxyResponseEvent searchOrders(APIGatewayProxyRequestEvent event)
            throws JsonProcessingException {
        Map<String, String> params = event.getQueryStringParameters();
        String customerId = params == null ? null : params.get("customerId");
        return json(200, service.getOrdersByCustomer(customerId));
    }
    private APIGatewayProxyResponseEvent deleteOrder(APIGatewayProxyRequestEvent event) {
        service.deleteOrder(pathId(event));
        return noContent();
    }
    private String pathId(APIGatewayProxyRequestEvent event) {
        Map<String, String> params = event.getPathParameters();
        return params == null ? null : params.get("id");
    }
    private APIGatewayProxyResponseEvent noContent() {
        return new APIGatewayProxyResponseEvent().withStatusCode(204);
    }

    private APIGatewayProxyResponseEvent error(int status, String code, String message) {
        try {
            return json(status, Map.of("error", code, "message", message));
        } catch (JsonProcessingException e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("{}");
        }
    }

    private APIGatewayProxyResponseEvent json(int status, Object body) throws JsonProcessingException {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(MAPPER.writeValueAsString(body));
    }

}