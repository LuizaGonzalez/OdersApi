# Configuración de API Gateway (Arquitectura A: mapping templates)

Documentación para reproducir la API en la consola de AWS.

## Resumen

| Elemento | Valor |
|---|---|
| Tipo de API | REST API (no HTTP API, porque solo la REST soporta mapping templates) |
| Nombre | orders-api |
| Endpoint | Regional |
| Región | us-east-1 |
| Stage | dev |
| Integración | Lambda **sin** proxy (Lambda proxy integration desactivado) |
| Rol de ejecución de las Lambdas | LabRole (AWS Academy no permite crear roles IAM) |

## Funciones Lambda

Las tres usan el mismo JAR (orders-api-1.0-SNAPSHOT.jar, el generado por maven-shade-plugin, no el que empieza con "original-"), Java 17, 512 MB y timeout de 15 s.

| Función | Handler | Entrada (Java) | Salida (Java) |
|---|---|---|---|
| orders-create | edu.eci.arsw.orders.CreateOrderHandler::handleRequest | CreateOrderRequest | Order |
| orders-get | edu.eci.arsw.orders.GetOrderHandler::handleRequest | GetOrderRequest | Order |
| orders-search | edu.eci.arsw.orders.SearchOrdersHandler::handleRequest | SearchOrdersRequest | List de Order |

Cada función Lambda tiene su propia memoria. Un pedido creado en orders-create no existe para orders-get ni para orders-search. Por eso estas dos usan OrderService.withSampleData().

## Recursos

    /
    └── /orders              GET (búsqueda), POST (crear)
        └── /{id}            GET (consultar)

El recurso hijo debe llamarse {id} (con llaves). Sin ellas, API Gateway lo trata como una ruta fija llamada "id".

## Métodos

### POST /orders (orders-create)

- Request body passthrough: When there are no templates defined (recommended).
- Respuestas: 201 (éxito) y 400.
- Plantilla de solicitud (application/json):
  $input.json('$')
### GET /orders/{id} (orders-get)

- Request body passthrough: When there are no templates defined (recommended).
- Respuestas: 200 (éxito) y 404.
- Plantilla de solicitud (application/json):
  {
  "id": "$input.params('id')"
  }
### GET /orders?customerId= (orders-search)

- Method request: query string customerId, con Required sin marcar. Así el 400 lo produce la validación de la Lambda.
- Request body passthrough: When there are no templates defined (recommended).
- Respuestas: 200 (éxito) y 400.
- Plantilla de solicitud (application/json):
  {
  "customerId": "$input.params('customerId')"
  }
  Si el parámetro falta, la plantilla produce {"customerId": ""}, y la validación de la Lambda responde "customerId is required".

## Respuestas de éxito

En cada método, la fila por defecto de Integration response (con Lambda error regex vacío) es la respuesta de éxito:

| Método | Código de éxito |
|---|---|
| POST /orders | 201 |
| GET /orders/{id} | 200 |
| GET /orders?customerId= | 200 |

En el POST, la fila por defecto no permite cambiar el código de 200 a 201. Se eliminó y se creó de nuevo con código 201 y Lambda error regex vacío.

## Mapeo de errores

Cuando una Lambda lanza una excepción, devuelve HTTP 200 con la cabecera X-Amz-Function-Error: Unhandled y un cuerpo con errorMessage, errorType y stackTrace. API Gateway decide el código HTTP comparando errorMessage con la expresión regular de cada fila de Integration response.

Para cada código de error: primero se declara en Method response, y luego se crea la fila en Integration response.

### Expresiones regulares (campo Lambda error regex)

Para el 404:

    .*does not exist.*

Cubre: Order ORD-999 does not exist.

Para el 400:

    .*(required|must be|JSON parsing).*

Cubre: customerId is required, quantity must be greater than 0 y An error occurred during JSON parsing.

Cada método usa solo las filas que necesita:

| Método | Filas de error |
|---|---|
| POST /orders | 400 |
| GET /orders/{id} | 404 (y opcionalmente un 400 para "required") |
| GET /orders?customerId= | 400 |

### Plantillas de respuesta (application/json)

Para el 404 (ORDER_NOT_FOUND):

    #set($msg = $input.path('$.errorMessage'))
    {"error": "ORDER_NOT_FOUND", "message": "$util.escapeJavaScript($msg)"}

Para el 400 (INVALID_REQUEST):

    #set($msg = $input.path('$.errorMessage'))
    {"error": "INVALID_REQUEST", "message": "$util.escapeJavaScript($msg)"}

La línea #set deja un salto de línea al inicio del cuerpo. Es inofensivo. Para eliminarlo, se puede terminar esa línea con ##.

## Despliegue

1. Deploy API, en el stage dev.
2. Después de cada cambio hay que volver a desplegar. La pestaña Test usa la configuración actual, pero el stage usa la última versión desplegada.
3. URL base: https://ds1ph0ye0c.execute-api.us-east-1.amazonaws.com/dev