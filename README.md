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

## Fase 1: Implementar la aplicación en Java puro

- ¿Se puede probar la funcionalidad de la aplicación sin AWS?

R/:Sí, toda la lógica de pedidos es Java, así que las pruebas se pueden ejecutar en el mismo computador con mvn test, solo
se necesita JUnit, y no hace falta cuenta de AWS, internet ni el servicio desplegado.

- ¿Tu aplicación requiere un servidor web?

R/: No, la aplicación es un conjunto de clases Java que se pueden llamar directamente desde las pruebas, no escucha en 
ningún puerto ni atiende peticiones, simplemente recibe objetos y devuelve objetos(POJO).

- ¿Requiere Spring Boot?
R/:No, El único framework en el proyecto es JUnit, y solo para las pruebas.

- ¿Qué contiene el JAR generado?

R/:Contiene las clases ya compiladas del proyecto (Order, CreateOrderRequest, OrderService y las dos excepciones) junto
con un archivo de manifiesto con metadatos. Por ahora no trae librerías de terceros, porque el proyecto no depende 
de ninguna en producción. Más adelante, cuando agreguemos las librerías de AWS Lambda, el JAR tendrá que incluirlas.

- ¿Qué partes de tu aplicación conocen actualmente HTTP?

R/:Ninguna. En este punto no existen los códigos de estado, los headers ni las rutas. Lo único que tenemos son
excepciones propias del dominio, como “pedido no encontrado” o “pedido inválido”, que describen qué salió mal en 
términos del negocio.

## Parte II:Desplegar una función Java en AWS Lambda
*Prueba1*
![Test1Lambda.png](evidencia/Test1Lambda.png)
*Prueba2*
![Test2Lambda.png](evidencia/Test2Lambda.png)
*Prueba3*
![Test3Lambda.png](evidencia/Test3Lambda.png)

- ¿cómo convierte Lambda el JSON en objetos Java y viceversa?
El runtime de Java de Lambda trae serialización y deserialización por defecto: el JSON de entrada se convierte en un 
objeto Java y lo que devuelve la función se convierte de nuevo en JSON. Con RequestHandler<I, O>, los tipos genéricos 
le indican al runtime a qué clases convertir, por eso mi CreateOrderHandler recibe un CreateOrderRequest y devuelve un 
Order sin que yo escriba código JSON.

El mapeo se hace por nombre: la serialización por defecto espera que las propiedades del JSON coincidan con los nombres 
en camel case de la clase Java (customerId, product, quantity).

| Experimento | Entrada                                                      | Resultado                                                                                                              |
|---|--------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| Entrada válida | {"customerId":"CUST-42","product":"Lavadora","quantity":1}   | **{"id": "ORD-001","customerId": "CUST-42","product": "Lavadora","quantity": 1 }**                                     |
| Otros valores | {"customerId":"CUST-7","product":"Celular","quantity":2}     | **{"id": "ORD-002","customerId": "CUST-7","product": "Celular","quantity": 2 }**                                       |
| Validación | {"customerId":"CUST-7","product":"Celular","quantity":0}     | Falla con **errorType: edu.eci.arsw.orders.InvalidOrderException** y **errorMessage: quantity must be greater than 0** |
| Campo extra | {"customerId":"CUST-7","product":"Peine","quantity":7,"color":"Azul"} | Ignora el color y devuelve el json correspondiente                                                                     |
| Tipo incorrecto | {"customerId":"CUST-7","product":"Keyboard","quantity":"abc"} | **Falla con errorType: java.lang.RuntimeException, errorMessage: An error occurred during JSON parsing y stackTrace vacío**                                                                                                                   |
| Objeto vacío |{}                                                            | **Falla con errorType: java.lang.RuntimeException, errorMessage: An error occurred during JSON parsing y stackTrace vacío**                                                                                                         |

*Fuente:* https://docs.aws.amazon.com/lambda/latest/dg/java-custom-serialization.html

![lambdaRuntime.png](evidencia/lambdaRuntime.png)

## Parte 3 y 4: Diseñar la interfaz HTTP y mapeo en API Gateway

- ¿De qué es responsable API Gateway actualmente?

R/: En esta primera arquitectura, API Gateway se encarga de recibir las peticiones y enviarlas a la función Lambda que 
corresponda, dependiendo de lo que se quiera hacer. Por ejemplo, si se quiere crear un pedido, la petición se envía a 
orders-create; si se quiere consultar un pedido específico, va a orders-get, y si se quieren buscar varios pedidos,
se envía a orders-search.

También se encarga de extraer la información de la petición, como el ID del pedido que viene en la URL o el customerId
que se envía como parámetro. Si este último no viene, lo deja como un texto vacío. Después, organiza esa información en
un JSON con el formato que necesita cada función Lambda para procesarla.

Cuando Lambda devuelve una respuesta, API Gateway se encarga de definir el código de estado correspondiente, por ejemplo,
200 si todo sale bien, 201 si se crea un pedido, 400 si hay un error en la solicitud o 404 si no se encuentra el pedido. Además, 
adapta los mensajes de error para que todos tengan el mismo formato, con los campos error y message. Para saber
qué código de error utilizar, revisa el mensaje que devuelve Lambda y lo compara con unas expresiones regulares.

En pocas palabras, API Gateway conecta las peticiones con las funciones Lambda y se encarga de organizar tanto la 
información que reciben como las respuestas que devuelven.

**Ventajas**

La mayor ventaja es que el código Java no sabe nada de HTTP. Los handlers reciben un objeto sencillo y devuelven un pedido o una lista de pedidos, sin códigos de estado, headers ni rutas. Esto deja la lógica de negocio muy limpia.

Además, casi no hay código de infraestructura que escribir. No hace falta un adaptador, ni leer eventos de API Gateway, ni armar a mano las respuestas HTTP.

Otra ventaja es que se puede cambiar el contrato sin recompilar. Si quiero cambiar un código de estado o el formato de un error, edito la configuración, vuelvo a desplegar y no toco el JAR.

Por último, cada operación es una Lambda pequeña y aislada, que falla, escala y se despliega por separado.

**Desventajas**

La principal es que una parte importante de la lógica de la API vive en la consola de AWS y no en el repositorio. No se versiona ni se revisa como código, y reproducirla depende de que alguien la haya documentado a mano.

Además, el código HTTP depende del texto del mensaje de error. Si alguien cambia la frase “does not exist” en la excepción de Java, la expresión regular deja de coincidir, el 404 desaparece y el cliente recibe un 200 con el error de Lambda dentro. Es una dependencia frágil y poco visible.

También es difícil de probar de forma local. Las plantillas y las expresiones regulares solo se pueden verificar en AWS, mientras que el código Java se prueba fácilmente con mvn test.

