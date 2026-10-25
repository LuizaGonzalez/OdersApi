# Ejemplos de solicitudes y respuestas (Arquitectura A)

API desplegada en API Gateway con mapping templates (integración Lambda sin proxy).

Variable usada en los comandos de PowerShell:

    $url = "https://ds1ph0ye0c.execute-api.us-east-1.amazonaws.com/dev"

Los cuerpos de las solicitudes POST están en docs/examples/:

- ok.json: pedido válido.
- bad.json: pedido inválido (quantity en 0).

## Resumen

| # | Solicitud | Código | Caso |
|---|---|---|---|
| 1 | GET /orders/ORD-001 | 200 | Consulta exitosa |
| 2 | GET /orders/ORD-999 | 404 | Pedido inexistente |
| 3 | GET /orders?customerId=CUST-42 | 200 | Búsqueda con resultados |
| 4 | GET /orders?customerId=CUST-100 | 200 | Búsqueda sin resultados |
| 5 | GET /orders | 400 | Falta customerId |
| 6 | POST /orders (válido) | 201 | Creación exitosa |
| 7 | POST /orders (inválido) | 400 | quantity en 0 |

## 1. Consulta exitosa

Comando:

    curl.exe -i "$url/orders/ORD-001"

Respuesta:

    HTTP/1.1 200 OK
    Content-Type: application/json

    {"id":"ORD-001","customerId":"CUST-42","product":"Lavadora","quantity":1}

## 2. Pedido inexistente

Comando:

    curl.exe -i "$url/orders/ORD-999"

Respuesta:

    HTTP/1.1 404 Not Found
    Content-Type: application/json

    {"error": "ORDER_NOT_FOUND", "message": "Order ORD-999 does not exist"}

## 3. Búsqueda con resultados

Comando:

    curl.exe -i "$url/orders?customerId=CUST-42"

Respuesta:

    HTTP/1.1 200 OK
    Content-Type: application/json

    [{"id":"ORD-001","customerId":"CUST-42","product":"Lavadora","quantity":1}]

## 4. Búsqueda sin resultados

Comando:

    curl.exe -i "$url/orders?customerId=CUST-100"

Respuesta:

    HTTP/1.1 200 OK
    Content-Type: application/json

    []

Una colección vacía no es un error: la respuesta es 200 con un arreglo vacío.

## 5. Falta el parámetro customerId

Comando:

    curl.exe -i "$url/orders"

Respuesta:

    HTTP/1.1 400 Bad Request
    Content-Type: application/json

    {"error": "INVALID_REQUEST", "message": "customerId is required"}

## 6. Creación exitosa

Comando:

    curl.exe -i -X POST "$url/orders" -H "Content-Type: application/json" -d "@docs/examples/ok.json"

Contenido de ok.json:

    {"customerId":"CUST-42","product":"Lavadora","quantity":1}

Respuesta:

    HTTP/1.1 201 Created
    Content-Type: application/json

    {"id":"ORD-003","customerId":"CUST-42","product":"Lavadora","quantity":1}

El id devuelto fue ORD-003 porque la función orders-create seguía "caliente" y ya había atendido dos creaciones anteriores. Tras un arranque en frío el contador vuelve a ORD-001. Es evidencia de que la memoria de Lambda no es almacenamiento persistente.

## 7. Creación con entrada inválida

Comando:

    curl.exe -i -X POST "$url/orders" -H "Content-Type: application/json" -d "@docs/examples/bad.json"

Contenido de bad.json:

    {"customerId":"CUST-42","product":"Lavadora","quantity":0}

Respuesta:

    HTTP/1.1 400 Bad Request
    Content-Type: application/json

    {"error": "INVALID_REQUEST", "message": "quantity must be greater than 0"}

## Respuesta cruda de Lambda (sin pasar por el mapeo)

Cuando la validación falla, esto es lo que devuelve la Lambda antes de que API Gateway lo transforme:

    {
      "errorMessage": "customerId is required",
      "errorType": "edu.eci.arsw.orders.InvalidOrderException",
      "stackTrace": ["edu.eci.arsw.orders.OrderService.getOrdersByCustomer(OrderService.java:39)", "..."]
    }

Lambda responde HTTP 200 con la cabecera X-Amz-Function-Error: Unhandled. API Gateway compara errorMessage con la expresión regular de cada fila de Integration response y, según coincida, asigna el código 400 o 404 y aplica la plantilla que produce el formato de error del contrato.