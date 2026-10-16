-- Bloqueo optimista en órdenes: evita que dos cambios de estado simultáneos (p. ej. enviar y cancelar) se pisen.
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
