# El evento de Lambda proxy integration

## Qué cambia con la integración proxy

En la primera arquitectura, API Gateway se encargaba de leer la petición y convertirla, con plantillas, en el JSON exacto que mi Lambda esperaba. Con la integración proxy ese trabajo desaparece: API Gateway ya no transforma nada, simplemente toma la petición HTTP completa, la empaqueta en un solo evento JSON y se lo entrega a la Lambda tal cual llegó.

En Java, ese evento llega como un objeto de tipo APIGatewayProxyRequestEvent, que viene de la librería aws-lambda-java-events. A cambio, la Lambda también tiene una obligación: debe responder con un objeto APIGatewayProxyResponseEvent que incluya el código de estado, los headers y el cuerpo. Si responde otra cosa, API Gateway no sabe qué hacer y devuelve un error 502 de respuesta mal formada.

El formato que se usa en las REST API, que es el que yo uso, corresponde a la versión 1.0 del payload.

## Dónde encontré cada dato

Al inspeccionar el evento, cada pieza de la petición HTTP tiene un lugar propio.

El método HTTP aparece en el campo httpMethod, por ejemplo GET o POST. La ruta aparece en dos formas distintas: path trae la dirección real que llamó el cliente, como /orders/ORD-001, mientras que resource trae la plantilla de la ruta tal como la definí en API Gateway, es decir /orders/{id}. Esa diferencia resulta muy útil, porque me permite enrutar usando resource y no tener que interpretar la URL a mano.

Los parámetros de ruta están en pathParameters. Para la petición GET /orders/ORD-001 ese campo contiene el par id con el valor ORD-001. Los parámetros de consulta están en queryStringParameters, y para una búsqueda como /orders?customerId=CUST-42 ahí aparece customerId con el valor CUST-42.

Los headers están en el campo headers, con una copia en multiValueHeaders para el caso de headers repetidos, y en las REST API conservan las mayúsculas que envió el cliente, como Content-Type. Por último, el cuerpo de la solicitud está en el campo body.

| Dato | Campo del evento | Método en Java |
|---|---|---|
| Método HTTP | httpMethod | getHttpMethod() |
| Ruta real | path | getPath() |
| Ruta definida en API Gateway | resource | getResource() |
| Parámetros de ruta | pathParameters | getPathParameters() |
| Parámetros de consulta | queryStringParameters | getQueryStringParameters() |
| Headers | headers | getHeaders() |
| Cuerpo | body | getBody() |

## Detalles que tuve en cuenta al escribir el código

El primero es que el cuerpo llega como un texto, no como un objeto. Aunque el cliente haya enviado un JSON, la Lambda lo recibe como un String, así que el handler tiene que convertirlo a un objeto Java con Jackson. Eso también significa que un JSON mal formado ya no lo detecta el runtime de Lambda, sino mi propio código, y por eso puedo responder con un 400 con el formato de error que diseñé.

El segundo es que, cuando no hay parámetros, el campo no viene como un mapa vacío sino como null. Por eso el handler revisa si es null antes de leer pathParameters o queryStringParameters, y si no lo hiciera, una búsqueda sin customerId terminaría en un error inesperado en lugar de un 400.

El tercero es que los datos que pide el formato de respuesta son obligatorios. Además del código de estado, el body debe ser un texto, así que el handler convierte los objetos a JSON antes de devolverlos, y agrega el header Content-Type. En el caso de la creación, también agrego el header Location con la dirección del nuevo pedido.

## Eventos de ejemplo guardados en el repositorio

Guardé dos eventos en la carpeta docs/events. El primero, get-order-event.json, representa un GET a /orders/ORD-001 y sirve para ver el parámetro de ruta. El segundo, post-order-event.json, representa un POST a /orders y sirve para ver cómo el cuerpo llega como texto.

Son eventos de ejemplo con el formato real del proxy. Para obtener el evento exacto de mi API se puede copiar el que muestra la pestaña Test de la Lambda, con la plantilla API Gateway AWS Proxy, o imprimirlo en CloudWatch Logs al llamar a la API desplegada.

## Fuentes

- AWS Docs, Create AWS Lambda proxy integrations for HTTP APIs in API Gateway: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html
- Middy, API Gateway REST (resumen del evento v1): https://middy.js.org/docs/events/api-gateway-rest