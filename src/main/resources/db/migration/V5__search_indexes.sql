-- Búsqueda de productos (GET /products): el LIKE '%texto%' sobre LOWER(name) no puede usar un btree;
-- un índice GIN de trigramas sí. El compuesto sirve al filtro categoría + rango de precio.
-- La medición con EXPLAIN está resumida en docs/decisiones.md (D18).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_name_trgm ON products USING gin (LOWER(name) gin_trgm_ops);
CREATE INDEX idx_products_category_price ON products (category_id, price);

-- Se reemplazan por el compuesto: category_id es prefijo de idx_products_category_price.
DROP INDEX idx_products_category;
