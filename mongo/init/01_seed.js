db = db.getSiblingDB('demo'); db.clientes.drop();
db.clientes.insertMany([
  { nombre:"Ana Pérez",correo:"ana@example.com",direccion:{calle:"Calle 10 #5-12",ciudad:"Bogotá",pais:"Colombia"} },
  { nombre:"Luis Gómez",correo:"luis@example.com",direccion:{calle:"Av. Reforma 123",ciudad:"CDMX",pais:"México"} },
  { nombre:"María López",correo:"maria@example.com",direccion:{calle:"Gran Vía 45",ciudad:"Madrid",pais:"España"} },
  { nombre:"John Smith",correo:"john@example.com",direccion:{calle:"742 Evergreen",ciudad:"Springfield",pais:"USA"} },
  { nombre:"Sofía Ramírez",correo:"sofia@example.com",direccion:{calle:"Cra 7 #12-80",ciudad:"Medellín",pais:"Colombia"} },
  { nombre:"Pedro Sánchez",correo:"pedro@example.com",direccion:{calle:"Av. Libertador 500",ciudad:"Buenos Aires",pais:"Argentina"} },
  { nombre:"Lucía Fernández",correo:"lucia@example.com",direccion:{calle:"Rua Augusta 100",ciudad:"São Paulo",pais:"Brasil"} },
  { nombre:"Carlos Ruiz",correo:"carlos@example.com",direccion:{calle:"Av. Arequipa 200",ciudad:"Lima",pais:"Perú"} },
  { nombre:"Elena Petrova",correo:"elena@example.com",direccion:{calle:"Nevsky 1",ciudad:"San Petersburgo",pais:"Rusia"} },
  { nombre:"Marta Díaz",correo:"marta@example.com",direccion:{calle:"C/ Colón 8",ciudad:"Valencia",pais:"España"} }
]);
