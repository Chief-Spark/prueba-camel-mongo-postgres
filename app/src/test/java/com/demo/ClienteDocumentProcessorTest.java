package com.demo;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClienteDocumentProcessorTest {

    private ClienteDocumentProcessor processor;
    private DefaultCamelContext camelContext;

    @BeforeEach
    void setUp() {
        processor = new ClienteDocumentProcessor();
        camelContext = new DefaultCamelContext();
    }

    @Test
    void testTransformDocumentoCompleto() throws Exception {
        Document doc = new Document("nombre", "Juan Perez")
                .append("correo", "juan@example.com")
                .append("direccion", new Document("calle", "Calle 123")
                        .append("ciudad", "Bogota")
                        .append("pais", "Colombia"));

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(doc);

        processor.process(exchange);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = exchange.getIn().getBody(Map.class);

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("Juan Perez", result.get("p_nombre"));
        assertEquals("juan@example.com", result.get("p_correo"));
        assertEquals("Calle 123", result.get("p_calle"));
        assertEquals("Bogota", result.get("p_ciudad"));
        assertEquals("Colombia", result.get("p_pais"));
    }

    @Test
    void testCamposConCaracteresEspeciales() throws Exception {
        Document doc = new Document("nombre", "Maria Munoz")
                .append("correo", "maria.munoz@correo.co")
                .append("direccion", new Document("calle", "Carrera 45 #12-30")
                        .append("ciudad", "Bogota")
                        .append("pais", "Colombia"));

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(doc);

        processor.process(exchange);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = exchange.getIn().getBody(Map.class);

        assertNotNull(result);
        assertEquals("Maria Munoz", result.get("p_nombre"));
        assertEquals("maria.munoz@correo.co", result.get("p_correo"));
        assertEquals("Carrera 45 #12-30", result.get("p_calle"));
        assertEquals("Bogota", result.get("p_ciudad"));
        assertEquals("Colombia", result.get("p_pais"));
    }

    @Test
    void testDocumentoSinDireccionLanzaExcepcion() {
        Document doc = new Document("nombre", "Carlos Lopez")
                .append("correo", "carlos@example.com");

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(doc);

        assertThrows(NullPointerException.class, () -> processor.process(exchange));
    }
}
