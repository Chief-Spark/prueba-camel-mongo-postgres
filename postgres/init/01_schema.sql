CREATE TABLE IF NOT EXISTS clientes (
  id SERIAL PRIMARY KEY,
  nombre TEXT NOT NULL,
  correo TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS direcciones (
  id SERIAL PRIMARY KEY,
  cliente_id INT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
  calle TEXT NOT NULL,
  ciudad TEXT NOT NULL,
  pais TEXT NOT NULL
);

CREATE OR REPLACE FUNCTION upsert_cliente(
  p_nombre TEXT,p_correo TEXT,p_calle TEXT,p_ciudad TEXT,p_pais TEXT
) RETURNS INT AS $$
DECLARE v_cliente_id INT;
BEGIN
  INSERT INTO clientes(nombre, correo) VALUES (p_nombre, p_correo)
  ON CONFLICT (correo) DO UPDATE SET nombre = EXCLUDED.nombre
  RETURNING id INTO v_cliente_id;

  IF EXISTS (SELECT 1 FROM direcciones WHERE cliente_id = v_cliente_id) THEN
     UPDATE direcciones SET calle=p_calle, ciudad=p_ciudad, pais=p_pais WHERE cliente_id=v_cliente_id;
  ELSE
     INSERT INTO direcciones(cliente_id,calle,ciudad,pais) VALUES (v_cliente_id,p_calle,p_ciudad,p_pais);
  END IF;

  RETURN v_cliente_id;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION clientes_por_pais(p_pais TEXT)
RETURNS TABLE(id INT, nombre TEXT, correo TEXT, calle TEXT, ciudad TEXT, pais TEXT) AS $$
BEGIN
  RETURN QUERY SELECT c.id,c.nombre,c.correo,d.calle,d.ciudad,d.pais
  FROM clientes c JOIN direcciones d ON d.cliente_id=c.id WHERE d.pais=p_pais;
END; $$ LANGUAGE plpgsql;
