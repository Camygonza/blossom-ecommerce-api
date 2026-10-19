-- La tienda vende en pesos colombianos (COP). Los precios del seed estaban en una escala de dólares.
-- Los precios nuevos son valores de tienda reales, redondeados a 100 COP.
UPDATE products p
SET price = v.price
FROM (VALUES
  (1, 58900), (2, 24900), (3, 32900), (4, 89900), (5, 46900), (6, 42900), (7, 28900),
  (8, 39900), (9, 79900), (10, 54900), (11, 22900), (12, 69900), (13, 94900),
  (14, 24900), (15, 9900), (16, 49900), (17, 19900), (18, 54900),
  (19, 21900), (20, 27900), (21, 44900), (22, 15900), (23, 12900), (24, 14900),
  (25, 19900), (26, 59900), (27, 9900), (28, 7900), (29, 24900), (30, 119900)
) AS v(id, price)
WHERE p.id = v.id;

-- Las órdenes ya creadas (de prueba) conservan su proporción: se convierten con una tasa de 4000 COP por unidad
-- de la escala anterior, para que sus importes sigan siendo coherentes con la nueva moneda.
UPDATE order_items SET unit_price = unit_price * 4000;
UPDATE orders SET total = total * 4000;
