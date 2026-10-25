# OdersApi
Order Management API: Java 17 + Maven on AWS Lambda, exposed via API Gateway (mapping templates vs. Lambda proxy). Workshop on architectural responsibilities.

## Fase 0: Contrato de la API


| Operacion  | Request | Exito | Errores                                                                                         |
|------------|---------|-------|-------------------------------------------------------------------------------------------------|
|Crear pedido| POST /orders y el body {customerId, product, quantity}|201 Created | 400 si falta un campo, esta vacio o el JSon es invalido o condiciones de negocio no lo érmitesn |
|Consultar pedido| GET /orders/{id}|200 OK, más el pedido que quiere | 404 si no existe                                                                                |
|Buscar por cliente|GET /orders?customerId=*id*|200 OK, mas el array con los pedidos | 400 si falta el customer Id y 404 si no existe.                                                 |
|Eliminar pedido| DELETE /orders/{id}|204 No content| 404 si no existe  |

**Formato de error**
> {
> 
> "error": "ORDER_NOT_FOUND",
> 
> "message": "Order ORD-999 does not exist"
> 
> }

| Codigo | HTTP |
|--------|------|
|INVALID_REQUEST| 400 |
|ORDER_NOT_FOUND|404|
|INTERNAL_ERROR|500|

**Preguntas**

- ¿Cuáles son los recursos de tu API?

  R/: El recurso principal de la API son los pedidos (orders). Lo podemos ver de dos formas: la colección completa de 
  pedidos en /orders y un pedido específico en /orders/{id}. El cliente no lo traté como un recurso aparte, porque en
  este lab solo sirve como criterio para filtrar pedidos.

- ¿Por qué elegiste /orders/{id} para consultar un pedido en particular?

  R/:Porque el id identifica a un único pedido, y en REST lo que identifica a un recurso va en la ruta. Así la URL 
  funciona como el “nombre” del pedido.

- ¿Por qué customerId se representa como parámetro de consulta en la operación de búsqueda?
  
  R/:Porque customerId no identifica un pedido, sino que sirve para filtrar la lista de pedidos. Seguimos consultando 
  la colección /orders, solo que pidiendo los que pertenecen a un cliente. 

- ¿Qué debería devolver POST /orders cuando un pedido se crea exitosamente?

  R/:Debería devolver 201 Created, que indica que el recurso se creó, y no un simple 200. En el cuerpo va el pedido
  completo, incluyendo el id que asignó el servidor, para que el cliente sepa cómo referirse a él.

- ¿Qué debería devolver la API cuando un pedido no existe?

  R/:Un 404 Not Found con un cuerpo JSON que explique el error, por ejemplo con el código ORDER_NOT_FOUND y un mensaje 
  como “Order ORD-999 does not exist”. Así el cliente distingue claramente entre “ese pedido no existe”.

- ¿Qué información debería contener una respuesta de error?

  R/:Dos cosas. Primero, un código corto y estable (como ORDER_NOT_FOUND) que un programa pueda leer sin depender del 
  texto. Segundo, un mensaje claro para personas que explique qué pasó. Lo que nunca debe incluir son detalles internos 
  como stack traces, nombres de clases o información de la infraestructura, porque exponerlos es un riesgo de seguridad
  y no le sirve al cliente. Todos los errores usan el mismo formato, para que quien consuma la API pueda manejarlos de
  manera uniforme.
