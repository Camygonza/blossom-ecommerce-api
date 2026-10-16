-- Datos de demostración (solo desarrollo). Credenciales en knowledge/decisiones.md.
INSERT INTO users (email, password_hash, full_name, role, created_at, updated_at) VALUES
  ('admin@bloom.test', '$2a$10$RbFDYcvhjf2fsRErj4IS0eLfXZT2AX61K36fxlYdC30jmjCv6nO4C', 'Admin Bloom', 'ADMIN', now(), now()),
  ('cliente@bloom.test', '$2a$10$vj/AOG4TBBMqMLSClUQsBeSSt0yl6fECmGsPMTSJdyCCnnJfUtKia', 'Cliente Demo', 'CUSTOMER', now(), now());

INSERT INTO categories (id, name, description, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Plantas de interior', 'Plantas para dar vida a cada rincón del hogar', now(), now()),
  (2, 'Flores', 'Ramos y flores frescas de temporada', now(), now()),
  (3, 'Macetas y decoración', 'Macetas, soportes y objetos decorativos', now(), now()),
  (4, 'Herramientas', 'Todo lo necesario para cuidar tus plantas', now(), now()),
  (5, 'Guías digitales', 'Guías y cursos descargables de cultivo', now(), now());

INSERT INTO products (id, name, description, price, image_url, category_id, version, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Monstera Deliciosa', 'Planta tropical de hojas grandes y perforadas, fácil de cuidar.', 45.9, 'https://images.unsplash.com/photo-1614594975525-e45190c55d0b?auto=format&fit=crop&w=800&q=80', 1, 0, now(), now()),
  (2, 'Pothos Dorado', 'Enredadera resistente ideal para principiantes y poca luz.', 22.5, 'https://images.unsplash.com/photo-1572688484438-313a6e50c333?auto=format&fit=crop&w=800&q=80', 1, 0, now(), now()),
  (3, 'Sansevieria Trifasciata', 'Lengua de suegra: casi indestructible y purifica el aire.', 28.0, 'https://images.unsplash.com/photo-1593482892290-f54927ae1bb6?auto=format&fit=crop&w=800&q=80', 1, 0, now(), now()),
  (4, 'Ficus Lyrata', 'Higuera de hoja de violín, protagonista de cualquier sala.', 69.9, 'https://images.unsplash.com/photo-1545241047-6083a3684587?auto=format&fit=crop&w=800&q=80', 1, 0, now(), now()),
  (5, 'Calathea Orbifolia', 'Hojas anchas con rayas plateadas, ama la humedad.', 38.0, 'https://images.unsplash.com/photo-1598880940371-c756e015faf1?auto=format&fit=crop&w=800&q=80', 1, 0, now(), now()),
  (6, 'Zamioculca', 'Planta de interior muy tolerante a la sequía.', 34.9, 'https://images.unsplash.com/photo-1632207691143-643e2a9a9361?auto=format&fit=crop&w=800&q=80', 1, 0, now(), now()),
  (7, 'Helecho Boston', 'Follaje frondoso y delicado para espacios luminosos.', 24.9, 'https://images.unsplash.com/photo-1512428813834-c702c7702b78?auto=format&fit=crop&w=800&q=80', 1, 0, now(), now()),
  (8, 'Ramo de Girasoles', 'Ramo de siete girasoles frescos con follaje.', 29.9, 'https://images.unsplash.com/photo-1597848212624-a19eb35e2651?auto=format&fit=crop&w=800&q=80', 2, 0, now(), now()),
  (9, 'Rosas Rojas x12', 'Docena de rosas rojas de tallo largo.', 54.0, 'https://images.unsplash.com/photo-1518895949257-7621c3c786d7?auto=format&fit=crop&w=800&q=80', 2, 0, now(), now()),
  (10, 'Tulipanes Mixtos', 'Ramo de tulipanes de colores de temporada.', 32.5, 'https://images.unsplash.com/photo-1520763185298-1b434c919102?auto=format&fit=crop&w=800&q=80', 2, 0, now(), now()),
  (11, 'Lavanda en Maceta', 'Lavanda aromática, perfecta para balcón soleado.', 18.9, 'https://images.unsplash.com/photo-1499002238440-d264edd596ec?auto=format&fit=crop&w=800&q=80', 2, 0, now(), now()),
  (12, 'Orquídea Phalaenopsis', 'Orquídea mariposa con dos varas florales.', 49.9, 'https://images.unsplash.com/photo-1566873535350-a3f5d4a804b7?auto=format&fit=crop&w=800&q=80', 2, 0, now(), now()),
  (13, 'Peonías Rosadas', 'Ramo de peonías de temporada, edición limitada.', 62.0, 'https://images.unsplash.com/photo-1490750967868-88aa4486c946?auto=format&fit=crop&w=800&q=80', 2, 0, now(), now()),
  (14, 'Maceta de Cerámica Blanca', 'Maceta minimalista de cerámica con plato, 20 cm.', 19.9, 'https://images.unsplash.com/photo-1485955900006-10f4d324d411?auto=format&fit=crop&w=800&q=80', 3, 0, now(), now()),
  (15, 'Maceta Terracota 15 cm', 'Clásica maceta de barro cocido con drenaje.', 8.5, 'https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=800&q=80', 3, 0, now(), now()),
  (16, 'Soporte de Madera para Plantas', 'Soporte escalonado de pino para tres macetas.', 39.9, 'https://images.unsplash.com/photo-1463320898484-cdee8141c787?auto=format&fit=crop&w=800&q=80', 3, 0, now(), now()),
  (17, 'Colgante Macramé', 'Porta macetas colgante de algodón trenzado a mano.', 15.9, 'https://images.unsplash.com/photo-1508013837942-7a1b2d2d1d2c?auto=format&fit=crop&w=800&q=80', 3, 0, now(), now()),
  (18, 'Set de 3 Macetas Nórdicas', 'Tres macetas de cemento en tonos neutros.', 44.0, 'https://images.unsplash.com/photo-1459156212016-c812468e2115?auto=format&fit=crop&w=800&q=80', 3, 0, now(), now()),
  (19, 'Tijeras de Podar', 'Tijeras de acero inoxidable con mango ergonómico.', 16.5, 'https://images.unsplash.com/photo-1416879595882-3373a0480b5c?auto=format&fit=crop&w=800&q=80', 4, 0, now(), now()),
  (20, 'Regadera de Cuello Largo', 'Regadera metálica de 1.5 L para plantas de interior.', 21.9, 'https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?auto=format&fit=crop&w=800&q=80', 4, 0, now(), now()),
  (21, 'Set de Herramientas de Jardín', 'Pala, rastrillo y transplantador en estuche.', 34.5, 'https://images.unsplash.com/photo-1617576683096-00fc8eecb3af?auto=format&fit=crop&w=800&q=80', 4, 0, now(), now()),
  (22, 'Sustrato Universal 10 L', 'Mezcla de turba, perlita y fibra de coco.', 12.9, 'https://images.unsplash.com/photo-1416879595882-3373a0480b5d?auto=format&fit=crop&w=800&q=80', 4, 0, now(), now()),
  (23, 'Pulverizador de Cobre', 'Atomizador de 500 ml para hojas y orquídeas.', 14.9, 'https://images.unsplash.com/photo-1558904541-efa843a96f01?auto=format&fit=crop&w=800&q=80', 4, 0, now(), now()),
  (24, 'Fertilizante Orgánico', 'Abono líquido concentrado, botella de 500 ml.', 11.5, 'https://images.unsplash.com/photo-1523348837708-15d4a09cfac2?auto=format&fit=crop&w=800&q=80', 4, 0, now(), now()),
  (25, 'Guía de Cuidado de Plantas de Interior', 'PDF de 60 páginas con riego, luz y plagas.', 9.9, 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=800&q=80', 5, 0, now(), now()),
  (26, 'Curso de Propagación en Casa', 'Video curso de esquejes, acodos y semillas.', 29.0, 'https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=800&q=80', 5, 0, now(), now()),
  (27, 'Calendario Lunar de Siembra', 'Calendario descargable con fechas de siembra.', 5.9, 'https://images.unsplash.com/photo-1506485338023-6ce5f36692df?auto=format&fit=crop&w=800&q=80', 5, 0, now(), now()),
  (28, 'Kit Imprimible de Etiquetas', 'Etiquetas y tarjetas para organizar tu huerto.', 4.9, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=800&q=80', 5, 0, now(), now()),
  (29, 'Guía de Huerto Urbano', 'Planifica tu huerto en balcón: cultivos y calendarios.', 12.9, 'https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=800&q=80', 5, 0, now(), now()),
  (30, 'Licencia Comercial: Pack de Guías', 'Todas las guías con licencia para uso comercial.', 59.0, 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80', 5, 0, now(), now());

INSERT INTO physical_products (id, stock, weight_grams) VALUES
  (1, 12, 1800),
  (2, 30, 600),
  (3, 25, 1200),
  (4, 8, 3500),
  (5, 14, 900),
  (6, 18, 1500),
  (7, 20, 800),
  (8, 15, 700),
  (9, 10, 900),
  (10, 16, 600),
  (11, 40, 500),
  (12, 9, 1100),
  (13, 6, 800),
  (14, 50, 1100),
  (15, 80, 600),
  (16, 22, 2500),
  (17, 35, 200),
  (18, 18, 3000),
  (19, 45, 300),
  (20, 28, 700),
  (21, 20, 1400),
  (22, 60, 5000),
  (23, 33, 350),
  (24, 70, 600);

INSERT INTO digital_products (id, download_url, license_type) VALUES
  (25, 'https://downloads.bloom.test/guias/interior.pdf', 'PERSONAL'),
  (26, 'https://downloads.bloom.test/cursos/propagacion.zip', 'PERSONAL'),
  (27, 'https://downloads.bloom.test/guias/calendario-lunar.pdf', 'PERSONAL'),
  (28, 'https://downloads.bloom.test/kits/etiquetas.zip', 'PERSONAL'),
  (29, 'https://downloads.bloom.test/guias/huerto-urbano.pdf', 'PERSONAL'),
  (30, 'https://downloads.bloom.test/packs/guias-comercial.zip', 'COMMERCIAL');

SELECT setval(pg_get_serial_sequence('categories', 'id'), 100);
SELECT setval(pg_get_serial_sequence('products', 'id'), 100);
