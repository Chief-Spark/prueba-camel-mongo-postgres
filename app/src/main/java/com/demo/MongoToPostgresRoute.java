package com.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;

import java.util.HashMap;
import java.util.Map;

public class MongoToPostgresRoute extends RouteBuilder {

  private final ObjectMapper mapper = new ObjectMapper();

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
          .process(exchange -> {
            // El body es un org.bson.Document; lo convertimos a JSON y extraemos campos
            org.bson.Document doc = exchange.getIn().getBody(org.bson.Document.class);
            String json = doc.toJson();
            JsonNode root = mapper.readTree(json);

            Map<String, Object> p = new HashMap<>();
            p.put("p_nombre", root.get("nombre").asText());
            p.put("p_correo", root.get("correo").asText());

            JsonNode d = root.get("direccion");
            p.put("p_calle", d.get("calle").asText());
            p.put("p_ciudad", d.get("ciudad").asText());
            p.put("p_pais", d.get("pais").asText());

            exchange.getIn().setBody(p);
          })
          .to("sql:SELECT upsert_cliente(:#p_nombre, :#p_correo, :#p_calle, :#p_ciudad, :#p_pais)")
          .log("Upsert OK -> resultado: ${body}")
        .end();
  }
}
