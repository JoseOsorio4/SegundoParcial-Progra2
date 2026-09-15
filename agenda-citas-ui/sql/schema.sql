-- SegundoParcial-Progra2: variante C, agenda de citas de servicios.
-- Ejecutar en MySQL Workbench. El script conserva los datos existentes.
CREATE DATABASE IF NOT EXISTS segundo_parcial_progra2
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE segundo_parcial_progra2;

-- Una sola tabla cubre la entidad solicitada. El servicio es texto libre.
-- INT permite usar int en Java; el ID lo genera la BD y nunca se edita en UI.
-- VARCHAR limita cliente a 100 caracteres y servicio a 255, con validación Java.
-- DATETIME conserva fecha y hora local del negocio juntas, sin conversión de zona.
-- INT para minutos enteros; CHECK y el servicio Java exigen duración positiva.
-- ENUM restringe los tres estados también al escribir directamente en la BD.
-- NOT NULL impide ausencias; TRIM en CHECK evita nombres/descripciones vacíos.
-- La fecha futura se valida en Java AL CREAR: una cita guardada puede envejecer.
CREATE TABLE IF NOT EXISTS citas (
    id INT NOT NULL AUTO_INCREMENT,
    cliente VARCHAR(100) NOT NULL,
    fecha_hora DATETIME NOT NULL,
    servicio VARCHAR(255) NOT NULL,
    duracion_minutos INT NOT NULL,
    estado ENUM('pendiente', 'confirmada', 'cancelada') NOT NULL DEFAULT 'pendiente',
    PRIMARY KEY (id),
    CONSTRAINT chk_citas_cliente CHECK (CHAR_LENGTH(TRIM(cliente)) > 0),
    CONSTRAINT chk_citas_servicio CHECK (CHAR_LENGTH(TRIM(servicio)) > 0),
    CONSTRAINT chk_citas_duracion CHECK (duracion_minutos > 0)
) ENGINE=InnoDB;

-- Cancelar se realiza con UPDATE del estado: la fila sigue existiendo.
-- Eliminar se realiza con DELETE por ID, previa confirmación en Swing.
