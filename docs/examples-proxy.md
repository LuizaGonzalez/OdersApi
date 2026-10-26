# Ejemplos de solicitudes y respuestas (Arquitectura B: Lambda proxy integration)

API desplegada en API Gateway con integración proxy. Una sola Lambda (orders-api-handler) atiende todas las rutas, y el adaptador HTTP escrito en Java decide los códigos de estado y el formato de los errores.

Variable usada en los comandos de PowerShell:

    $url2 = "https://tm2inxg7bd.execute-api.us-east-1.amazonaws.com/dev"

Los cuerpos de las solicitudes POST están en docs/examples/:

- ok.json: pedido válido.
- bad.json: pedido inválido (quantity en 0).
- wrong-type.json: pedido con un tipo incorrecto (quantity como texto).

## Resumen

| # | Solicitud | Código | Caso |
|---|---|---|---|
| 1 | GET /orders/ORD-001 | 200 | Consulta exitosa |
| 2 | GET /orders/ORD-999 | 404 | Pedido inexistente |
| 3 | GET /orders?customerId=CUST-42 | 200 | Búsqueda con resultados |
| 4 | GET /orders?customerId=CUST-100 | 200 | Búsqueda sin resultados |
| 5 | GET /orders | 400 | Falta customerId |
| 6 | POST /orders (válido) | 201 | Creación exitosa, con header Location |
| 7 | POST /orders (quantity en 0) | 400 | Validación de dominio |
| 8 | GET /orders/ORD-003 | 200 | El pedido creado en el paso 6 existe |
| 9 | POST /orders (quantity como texto) | 400 | Error de formato, resuelto en Java |

## 1. Consulta exitosa

Comando:

    curl.exe -i "$url2/orders/ORD-001"

Respuesta:

    HTTP/1.1 200 OK
    Content-Type: application/json

    {"id":"ORD-001","customerId":"CUST-42","product":"Lavadora","quantity":1}

## 2. Pedido inexistente

Comando:

    curl.exe -i "$url2/orders/ORD-999"

Respuesta:

    HTTP/1.1 404 Not Found
    Content-Type: application/json

    {"message":"Order ORD-999 does not exist","error":"ORDER_NOT_FOUND"}

## 3. Búsqueda con resultados

Comando:

    curl.exe -i "$url2/orders?customerId=CUST-42"

Respuesta:

    HTTP/1.1 200 OK
    Content-Type: application/json

    [{"id":"ORD-001","customerId":"CUST-42","product":"Lavadora","quantity":1}]

## 4. Búsqueda sin resultados

Comando:

    curl.exe -i "$url2/orders?customerId=CUST-100"

Respuesta:

    HTTP/1.1 200 OK
    Content-Type: application/json

    []

## 5. Falta el parámetro customerId

Comando:

    curl.exe -i "$url2/orders"

Respuesta:

    HTTP/1.1 400 Bad Request
    Content-Type: application/json

    {"message":"customerId is required","error":"INVALID_REQUEST"}

## 6. Creación exitosa

Comando:

    curl.exe -i -X POST "$url2/orders" -H "Content-Type: application/json" -d "@docs/examples/ok.json"

Respuesta:

    HTTP/1.1 201 Created
    Content-Type: application/json
    Location: /orders/ORD-003

    {"id":"ORD-003","customerId":"CUST-42","product":"Lavadora","quantity":1}

La respuesta incluye el header Location con la dirección del nuevo pedido. En la Arquitectura A no se devolvía.

## 7. Creación con quantity en 0

Comando:

    curl.exe -i -X POST "$url2/orders" -H "Content-Type: application/json" -d "@docs/examples/bad.json"

Respuesta:

    HTTP/1.1 400 Bad Request
    Content-Type: application/json

    {"message":"quantity must be greater than 0","error":"INVALID_REQUEST"}

## 8. El pedido creado existe

Comando:

    curl.exe -i "$url2/orders/ORD-003"

Respuesta:

    HTTP/1.1 200 OK
    Content-Type: application/json

    {"id":"ORD-003","customerId":"CUST-42","product":"Lavadora","quantity":1}

Como una sola Lambda atiende todas las operaciones, el pedido creado con el POST puede consultarse con un GET, mientras la función siga en el mismo entorno de ejecución. En la Arquitectura A esto no era posible, porque cada operación era una función con su propia memoria. Sigue sin ser almacenamiento persistente: si Lambda recicla el entorno, el pedido desaparece.

## 9. Creación con un tipo incorrecto

Comando:

    curl.exe -i -X POST "$url2/orders" -H "Content-Type: application/json" -d "@docs/examples/wrong-type.json"

Contenido de wrong-type.json:

    {"customerId":"CUST-42","product":"Lavadora","quantity":"abc"}

Respuesta:

    HTTP/1.1 400 Bad Request
    Content-Type: application/json

    {"message":"Request body is not valid JSON for this operation","error":"INVALID_REQUEST"}

En la Arquitectura A, este mismo caso fallaba dentro del runtime de Lambda con un RuntimeException genérico ("An error occurred during JSON parsing"), antes de llegar al código, y solo se pudo mapear agregando una expresión regular en API Gateway. Aquí lo resuelve el adaptador Java, que captura el error de Jackson y responde con el formato de error del contrato.

## Observación sobre el orden de los campos

En los errores, el campo message aparece antes que error. Es válido, porque en JSON el orden de los campos no importa, y ocurre porque el adaptador construye el objeto con Map.of, que no garantiza un orden.