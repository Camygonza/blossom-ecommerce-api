-- Las fotos del seed eran enlaces externos: varios dejaron de cargar y otros no correspondían al producto.
-- Ahora cada producto del seed apunta a una foto propia, servida por el frontend (public/products/NN.jpg).
UPDATE products SET image_url = '/products/' || LPAD(id::text, 2, '0') || '.jpg' WHERE id BETWEEN 1 AND 30;
