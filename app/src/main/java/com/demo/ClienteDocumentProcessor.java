package com.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.HashMap;
import java.util.Map;

public class ClienteDocumentProcessor implements Processor {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws Exception {
        org.bson.Document doc = exchange.getIn().getBody(org.bson.Document.class);
        String json = doc.toJson();
        JsonNode root = mapper.readTree(json);

        Map<String, Object> params = new HashMap<>();
        params.put("p_nombre", root.get("nombre").asText());
        params.put("p_correo", root.get("correo").asText());

        JsonNode d = root.get("direccion");
        params.put("p_calle", d.get("calle").asText());
        params.put("p_ciudad", d.get("ciudad").asText());
        params.put("p_pais", d.get("pais").asText());

        exchange.getIn().setBody(params);
    }
}
