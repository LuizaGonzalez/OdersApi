package edu.eci.arsw.orders;

import static org.junit.jupiter.api.Assertions.*;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdersApiHandlerTest {

    private OrdersApiHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrdersApiHandler(new OrderService());
    }

    private APIGatewayProxyRequestEvent event(String method, String resource) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withResource(resource);
    }

    private APIGatewayProxyResponseEvent post(String body) {
        return handler.handleRequest(event("POST", "/orders").withBody(body), null);
    }

    @Test
    void createReturns201WithLocation() {
        APIGatewayProxyResponseEvent r =
                post("{\"customerId\":\"CUST-42\",\"product\":\"Lavadora\",\"quantity\":1}");

        assertEquals(201, r.getStatusCode());
        assertEquals("/orders/ORD-001", r.getHeaders().get("Location"));
        assertTrue(r.getBody().contains("\"id\":\"ORD-001\""));
    }

    @Test
    void getExistingReturns200() {
        post("{\"customerId\":\"CUST-42\",\"product\":\"Lavadora\",\"quantity\":1}");

        APIGatewayProxyResponseEvent r = handler.handleRequest(
                event("GET", "/orders/{id}").withPathParameters(Map.of("id", "ORD-001")), null);

        assertEquals(200, r.getStatusCode());
        assertTrue(r.getBody().contains("Lavadora"));
    }

    @Test
    void getMissingReturns404() {
        APIGatewayProxyResponseEvent r = handler.handleRequest(
                event("GET", "/orders/{id}").withPathParameters(Map.of("id", "ORD-999")), null);

        assertEquals(404, r.getStatusCode());
        assertTrue(r.getBody().contains("ORDER_NOT_FOUND"));
    }

    @Test
    void searchByCustomerReturns200() {
        post("{\"customerId\":\"CUST-42\",\"product\":\"Lavadora\",\"quantity\":1}");

        APIGatewayProxyResponseEvent r = handler.handleRequest(
                event("GET", "/orders").withQueryStringParameters(Map.of("customerId", "CUST-42")), null);

        assertEquals(200, r.getStatusCode());
        assertTrue(r.getBody().startsWith("["));
    }

    @Test
    void searchWithoutCustomerIdReturns400() {
        APIGatewayProxyResponseEvent r = handler.handleRequest(event("GET", "/orders"), null);

        assertEquals(400, r.getStatusCode());
        assertTrue(r.getBody().contains("INVALID_REQUEST"));
    }

    @Test
    void invalidQuantityReturns400() {
        assertEquals(400,
                post("{\"customerId\":\"CUST-42\",\"product\":\"Lavadora\",\"quantity\":0}").getStatusCode());
    }

    @Test
    void wrongTypeReturns400() {
        assertEquals(400,
                post("{\"customerId\":\"CUST-42\",\"product\":\"Lavadora\",\"quantity\":\"abc\"}").getStatusCode());
    }

    @Test
    void malformedJsonReturns400() {
        assertEquals(400, post("{esto no es json").getStatusCode());
    }

    @Test
    void missingBodyReturns400() {
        assertEquals(400, post(null).getStatusCode());
    }

    @Test
    void unknownRouteReturns404() {
        assertEquals(404, handler.handleRequest(event("PATCH", "/orders"), null).getStatusCode());
    }
}