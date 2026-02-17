package com.demo;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;

public class MongoToPostgresRoute extends RouteBuilder {

  @Override
  public void configure() {

    onException(Exception.class)
        .handled(true)
        .log(LoggingLevel.ERROR, "Error procesando mensaje: ${exception.message}")
        .maximumRedeliveries(0);

    /*
     * En Camel, operation=findAll se usa en un endpoint productor (to:), no en un consumer (from:).
     * Por eso: usamos un timer (consumer), luego llamamos a Mongo con to(...findAll), y después split.
     */
    from("timer:runOnce?repeatCount=1")
        .routeId("mongo-to-postgres")
        // Cuerpo vacío para findAll (consulta = {}):
        .setBody(constant("{}"))
        // Productor: consulta a Mongo y devuelve List<Document>
        .to("mongodb:client?database=demo&collection=clientes&operation=findAll")
        // Iterar cada Document del List
        .split(body())
          .process(new ClienteDocumentProcessor())
          .to("sql:SELECT upsert_cliente(:#p_nombre, :#p_correo, :#p_calle, :#p_ciudad, :#p_pais)")
          .log("Upsert OK -> resultado: ${body}")
        .end();
  }
}
