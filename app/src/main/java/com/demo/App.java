package com.demo;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultRegistry;
import org.postgresql.ds.PGSimpleDataSource;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
public class App {
  public static void main(String[] args) throws Exception {
    DefaultRegistry reg = new DefaultRegistry();
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setURL("jdbc:postgresql://localhost:5432/demo"); ds.setUser("demo"); ds.setPassword("demo");
    reg.bind("postgresDS", ds);
    MongoClient client = MongoClients.create("mongodb://localhost:27017");
    reg.bind("client", client);
    try (CamelContext ctx = new DefaultCamelContext(reg)) {
      ctx.addRoutes(new MongoToPostgresRoute()); ctx.start(); Thread.currentThread().join();
    }
  }
}
