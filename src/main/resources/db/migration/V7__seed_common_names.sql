-- Nombres y descripciones del seed con el lenguaje de una tienda de plantas (no el nombre botánico).
-- Se actualizan por id para conservar las órdenes que ya los referencian.
UPDATE products p
SET name = v.name, description = v.description
FROM (VALUES
  (1, 'Costilla de Adán', 'Hojas grandes con cortes. Le gusta la luz indirecta y se riega cuando la tierra se seca.'),
  (2, 'Potus', 'Planta colgante muy fácil de cuidar. Aguanta poca luz y perdona los descuidos.'),
  (3, 'Lengua de suegra', 'Casi no pide cuidados: con un riego cada dos semanas está feliz.'),
  (4, 'Ficus lira', 'Hojas grandes en forma de violín. Necesita buena luz y no le gusta que la muevan.'),
  (5, 'Calatea', 'Hojas anchas con rayas plateadas. Le gusta la humedad y la luz suave.'),
  (6, 'Zamioculca', 'Resistente y de hoja brillante. Aguanta varios días sin riego.'),
  (7, 'Helecho', 'Follaje abundante. Mantén la tierra húmeda y lejos del sol directo.'),
  (8, 'Ramo de girasoles', 'Siete girasoles frescos con follaje verde.'),
  (9, 'Docena de rosas rojas', 'Doce rosas rojas de tallo largo, listas para regalar.'),
  (10, 'Tulipanes de colores', 'Ramo de tulipanes de varios colores, de temporada.'),
  (11, 'Lavanda en maceta', 'Lavanda aromática para un balcón con sol.'),
  (12, 'Orquídea blanca', 'Orquídea con dos varas de flores. Se riega una vez por semana.'),
  (13, 'Peonías rosadas', 'Ramo de peonías rosadas, edición limitada.'),
  (14, 'Maceta blanca de cerámica', 'Maceta sencilla de cerámica con plato, de 20 cm.'),
  (15, 'Maceta de barro 15 cm', 'La clásica maceta de barro cocido, con drenaje.'),
  (16, 'Base de madera para plantas', 'Base de madera para subir tus plantas del piso. Alcanza para tres macetas.'),
  (17, 'Colgante de macramé', 'Porta macetas colgante de algodón, tejido a mano.'),
  (18, 'Set de 3 macetas', 'Tres macetas de cemento en tonos neutros.'),
  (19, 'Tijeras de podar', 'Tijeras de acero con mango cómodo para podar y dar forma.'),
  (20, 'Regadera amarilla', 'Regadera metálica de 1,5 L con pico largo para llegar entre las hojas.'),
  (21, 'Kit de jardinería', 'Pala, rastrillo y trasplantador con mango de madera.'),
  (22, 'Tierra para plantas 10 L', 'Mezcla de tierra negra, perlita y fibra de coco para macetas.'),
  (23, 'Atomizador de agua', 'Para humedecer las hojas y dar humedad a tus plantas. 500 ml.'),
  (24, 'Abono orgánico', 'Abono granulado de liberación lenta para plantas de interior y de jardín.'),
  (25, 'Guía de cuidado de plantas', 'PDF de 60 páginas con riego, luz y plagas.'),
  (26, 'Curso de esquejes', 'Video curso para multiplicar tus plantas en casa: esquejes, acodos y semillas.'),
  (27, 'Calendario lunar de siembra', 'Calendario descargable con las fechas para sembrar y cosechar.'),
  (28, 'Etiquetas para plantas', 'Etiquetas imprimibles para identificar y organizar tu huerto.'),
  (29, 'Guía de huerto en casa', 'Planifica tu huerto en un balcón: qué sembrar y cuándo.'),
  (30, 'Pack de guías con licencia comercial', 'Todas las guías con licencia para uso comercial.')
) AS v(id, name, description)
WHERE p.id = v.id;
