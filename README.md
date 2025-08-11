# Prueba Técnica: Migración MongoDB → PostgreSQL con Apache Camel (Java 17)

Proyecto reproducible para leer documentos desde MongoDB, transformarlos y cargarlos en PostgreSQL usando Java 17 + Apache Camel. Incluye Docker Compose, seeds, funciones PL/pgSQL, pruebas y guía de validación.

# Resumen rápido (TL;DR)

# 1) Arrancar infraestructura
docker compose up -d

# 2) Compilar y ejecutar la app (desde ./app)
cd app
mvn -q -DskipTests clean package
java -jar target/camel-mongo-postgres-1.0.0-shaded.jar

# 3) Validar en Postgres
docker exec -it postgres psql -U demo -d demo -c "select * from clientes_por_pais('Colombia');"
Arquitectura

┌──────────────┐   findAll + JSON   ┌───────────────────────────┐      SQL (función)
│   MongoDB    │ ─────────────────▶ │   Apache Camel (Java 17)  │ ──────────────────▶ PostgreSQL
│ demo.clientes│      (timer)       │ timer → to(mongo) → split │    upsert_cliente()
└──────────────┘                     │ map params → to(sql)      │ ◀── clientes_por_pais()
                                     └───────────────────────────┘
MongoDB: colección clientes con direccion embebida (seed de 10 docs).

Camel: ruta con timer → mongodb (findAll como productor) → split → sql (PL/pgSQL).

PostgreSQL: tablas clientes y direcciones, funciones upsert_cliente(...) y clientes_por_pais(...).

Requisitos previos (macOS)

Docker Desktop activo.

Java 17 (JDK) y Maven.

IntelliJ IDEA.

Instalación con Homebrew:


brew install --cask docker
brew install openjdk@17 maven
echo 'export JAVA_HOME="$(/usr/libexec/java_home -v 17)"' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
mvn -version
📁 Estructura del proyecto

prueba-camel-mongo-postgres/
├─ docker-compose.yml
├─ mongo/
│  └─ init/01_seed.js                  # 10 clientes de prueba
├─ postgres/
│  └─ init/01_schema.sql               # DDL + funciones PL/pgSQL
└─ app/
   ├─ pom.xml                          # Maven (Camel + drivers)
   └─ src/
      ├─ main/java/com/demo/App.java   # Arranque Camel + registry beans (Mongo y Postgres)
      ├─ main/java/com/demo/MongoToPostgresRoute.java  # Ruta Camel (timer → mongo → split → sql)
      ├─ main/resources/application.properties
      └─ test/java/com/demo/RouteSmokeTest.java (opcional)
# Infraestructura (Docker)
Levantar todo:


docker compose up -d
docker ps
Comprobaciones rápidas:


# Mongo: hay 10 documentos?
docker exec -it mongo mongosh demo --eval 'db.clientes.countDocuments()'

# Postgres: existen tablas?
docker exec -it postgres psql -U demo -d demo -c "\dt"
Reinicializar (borra datos y re-aplica seeds/esquema):


docker compose down -v
docker compose up -d
# Aplicación (Camel, Java 17)
Dependencias clave (pom.xml)
camel-main, camel-mongodb, camel-sql, camel-timer

postgresql, jackson-databind

slf4j-simple (logs en consola)

No se declara mongodb-driver-sync directamente (lo trae camel-mongodb con versión compatible).

Ruta Camel (concepto)
Timer dispara una sola vez.

to("mongodb:...&operation=findAll") obtiene lista de documentos.

split(body()) procesa cada documento.

Mapea campos a parámetros y ejecuta SELECT upsert_cliente(...) en Postgres.

Ejecutar
Opción A (JAR “gordo”):


cd app
mvn -q -DskipTests clean package
java -jar target/camel-mongo-postgres-1.0.0-shaded.jar
Opción B (sin JAR “gordo”):


cd app
mvn -q -DskipTests exec:java -Dexec.mainClass=com.demo.App -Dexec.cleanupDaemonThreads=false
Verás logs como:


Documento Mongo: {...}
Upsert OK -> resultado: ...
Detener la app: Ctrl + C.

# Diseño de datos
MongoDB (seed)

{
  "nombre": "Ana Pérez",
  "correo": "ana@example.com",
  "direccion": { "calle": "Calle 10 #5-12", "ciudad": "Bogotá", "pais": "Colombia" }
}
Embebido 1:1 para direccion (lecturas atómicas, simple).

correo como clave natural (se usa para desduplicar en PG vía upsert).

Consultas útiles:


// listar por país
db.clientes.find(
  { "direccion.pais": "Colombia" },
  { _id:0, nombre:1, correo:1, "direccion.ciudad":1 }
).sort({ nombre:1 })
// actualizar correo
db.clientes.updateOne(
  { nombre: "Ana Pérez" },
  { $set: { correo: "ana.perez@nuevo.com" } }
)
PostgreSQL (DDL y funciones)
CREATE TABLE IF NOT EXISTS clientes (
  id     SERIAL PRIMARY KEY,
  nombre TEXT NOT NULL,
  correo TEXT NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS direcciones (
  id         SERIAL PRIMARY KEY,
  cliente_id INT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
  calle      TEXT NOT NULL,
  ciudad     TEXT NOT NULL,
  pais       TEXT NOT NULL
);
-- UPSERT por correo + sincronizar dirección 1:1
CREATE OR REPLACE FUNCTION upsert_cliente(
  p_nombre TEXT, p_correo TEXT, p_calle TEXT, p_ciudad TEXT, p_pais TEXT
) RETURNS INT AS $$
DECLARE v_cliente_id INT;
BEGIN
  INSERT INTO clientes(nombre, correo)
  VALUES (p_nombre, p_correo)
  ON CONFLICT (correo) DO UPDATE SET nombre = EXCLUDED.nombre
  RETURNING id INTO v_cliente_id;
  IF EXISTS (SELECT 1 FROM direcciones WHERE cliente_id = v_cliente_id) THEN
    UPDATE direcciones
       SET calle = p_calle, ciudad = p_ciudad, pais = p_pais
     WHERE cliente_id = v_cliente_id;
  ELSE
    INSERT INTO direcciones(cliente_id, calle, ciudad, pais)
    VALUES (v_cliente_id, p_calle, p_ciudad, p_pais);
  END IF;
  RETURN v_cliente_id;
END;
$$ LANGUAGE plpgsql;
-- Consulta por país
CREATE OR REPLACE FUNCTION clientes_por_pais(p_pais TEXT)
RETURNS TABLE(id INT, nombre TEXT, correo TEXT, calle TEXT, ciudad TEXT, pais TEXT) AS $$
BEGIN
  RETURN QUERY
  SELECT c.id, c.nombre, c.correo, d.calle, d.ciudad, d.pais
  FROM clientes c
  JOIN direcciones d ON d.cliente_id = c.id
  WHERE d.pais = p_pais;
END;
$$ LANGUAGE plpgsql STABLE;
🔍 Validación de la migración
Conteos:
docker exec -it postgres psql -U demo -d demo -c "select count(*) from clientes;"
docker exec -it postgres psql -U demo -d demo -c "select count(*) from direcciones;"
Join:
docker exec -it postgres psql -U demo -d demo -c \
"select c.id,c.nombre,c.correo,d.calle,d.ciudad,d.pais from clientes c join direcciones d on d.cliente_id=c.id order by c.id limit 10;"
Consulta por país:
docker exec -it postgres psql -U demo -d demo -c "select * from clientes_por_pais('Colombia');"
Prueba de UPSERT (actualización por mismo correo):
# Cambiar correo en Mongo
docker exec -it mongo mongosh demo --eval 'db.clientes.updateOne({nombre:"Ana Pérez"},{$set:{correo:"ana.perez@nuevo.com"}})'

# Re-ejecutar app para refrescar
cd app && java -jar target/camel-mongo-postgres-1.0.0-shaded.jar
# Pruebas
Smoke test (requiere Mongo y Postgres arriba):


cd app
mvn -q test
# Makefile (si lo tienes)

make setup   # genera estructura y archivos (si usaste los scripts segmentados)
make up      # docker compose up -d
make build   # maven package
make run     # ejecuta el jar
make psql    # abre psql en el contenedor
make down    # docker compose down
# Solución a errores comunes
NoClassDefFoundError: org/apache/camel/spi/Registry
Ejecutabas el JAR sin dependencias. Solución: usar shade (JAR “gordo”) o mvn exec:java.

operation ... cannot appear on a consumer endpoint (Mongo)
No se puede from("mongodb:...&operation=findAll"). Solución: usar timer → to("mongodb:...&operation=findAll") → split.

No endpoint could be found for: timer://...
Falta dependencia camel-timer en pom.xml.

SLF4J: Failed to load class "StaticLoggerBinder"
Agregar org.slf4j:slf4j-simple (scope runtime).

NoClassDefFoundError: com/mongodb/internal/connection/StreamFactory
Mezcla de drivers. No declares el driver de Mongo directo; usa el transitivo de camel-mongodb.

Postgres no listo / “connection refused”
Esperar con pg_isready:


until docker exec postgres pg_isready -U demo -d demo >/dev/null 2>&1; do sleep 2; done
Puertos ocupados (5432/27017)
Edita docker-compose.yml y cambia puertos externos (ej. 15432:5432, 27018:27017).

Zsh interpreta ! en shebang
Usa heredocs con <<'EOF' o reemplaza #!/usr/bin/env bash por #!/bin/bash.