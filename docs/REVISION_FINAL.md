# Revisión final — SegundoParcial-Progra2

Fecha: 15 de septiembre de 2026.

Se continuó el proyecto existente. Se conservaron los módulos, el CRUD y el
diseño visual actual. No se configuraron remotos, no se hicieron commits ni
se subió contenido a GitHub, conforme a la última indicación del usuario.

## Trabajo completado en esta revisión

- Ejecutada y ampliada la prueba pendiente de Swing con MySQL real.
- Corregido el recorte del selector Estado en el formulario.
- Añadido desplazamiento de campos para el tamaño mínimo de ventana.
- Corregida alineación de botones, anchuras de etiquetas y legibilidad.
- Adaptados buscador y pie del listado al espacio disponible.
- Corregida una prueba de ordenación que suponía posiciones de filas fijas.
- Incorporadas comprobaciones de duración no numérica, cancelación desde UI,
  error de conexión y accesibilidad del formulario al reducir la ventana.
- Añadido `probar-ui.cmd` y actualizadas las instrucciones de uso.

## Resultados ejecutados

| Verificación | Resultado |
|---|---|
| Maven `install`: padre, core y UI | BUILD SUCCESS |
| Pruebas JUnit del core | 14; cero fallos, errores u omisiones |
| Prueba de persistencia MySQL | 20 comprobaciones correctas |
| Integración Swing + MySQL | 30 comprobaciones correctas |
| Inicio de configuración desde el directorio UI | Consulta correcta; encuentra la configuración de la raíz |
| Separación de dependencias en fuentes | Cero imports JDBC en UI; cero imports Swing/AWT en core |
| Revisión visual | Correcta a tamaño inicial y mínimo; al mínimo el formulario permite desplazamiento |
| Configuración local y cachés | Excluidas de Git |

Entorno probado: Java 11.0.30, Maven 3.9.11 y MySQL 26.7.0.
La prueba Swing ejecuta los componentes de la aplicación y responde sus
diálogos dentro de su propio proceso, sin controlar otras aplicaciones.
Las pruebas de integración crearon registros identificables y eliminaron
exclusivamente esos registros al terminar.

Los registros de prueba no permanecen como datos iniciales de la aplicación.
Las vistas previas los muestran para poder revisar formulario y tabla.

Evidencia local generada (excluida de Git):

- `.tools/build-final.log` y `agenda-citas-core/target/surefire-reports/`.
- `.tools/agenda-preview.png`: tamaño inicial.
- `.tools/agenda-preview-minima.png`: tamaño mínimo.
- `agenda-citas-ui/target/agenda-citas-ui-1.0-SNAPSHOT.jar`: aplicación actualizada.

## Correspondencia con la asignación

Fuentes contrastadas: README del instructor, variante C y apuntes de clase
proporcionados en el escritorio del usuario.

| Requisito | Evidencia |
|---|---|
| Padre Maven y dos módulos JAR reales | Tres POM; UI declara core como dependencia |
| Modelo y DAO separados de Swing | Paquetes del core y revisión de imports |
| SQL propio con tipos, NOT NULL y PK autoincremental | `agenda-citas-ui/sql/schema.sql`, con decisiones comentadas |
| Persistencia real y PreparedStatement | `CitaDAO` y pruebas contra MySQL |
| Crear, listar, editar y eliminar desde Swing | Prueba de integración del formulario y JTable |
| Confirmación antes de borrar | Rechazo conserva la fila; aceptación la elimina |
| Cancelar conserva el registro | Actualización a Cancelada comprobada desde servicio y UI |
| Nuevas citas pendientes | Estado inicial forzado en servicio y formulario |
| Cliente y servicio obligatorios | Pruebas de vacíos, espacios y límites de longitud |
| Fecha no pasada al crear | Validación con reloj controlado; edición histórica permitida |
| Duración positiva y estados permitidos | Validación Java y restricciones del esquema |
| Errores sin cerrar la aplicación | Prueba de fallo de conexión y botón Refrescar disponible |
| Sin stacktrace al usuario ni catch vacíos | Revisión de excepciones y mensajes de JOptionPane |
| DAO incluye buscarPorId | Contrato presente y consulta comprobada |

Los requisitos técnicos revisados están cubiertos. La revisión no sustituye
la explicación personal del diseño que evalúa el instructor ni verifica la
autoría exigida en su sección sobre uso de IA.

## Archivos modificados o añadidos al retomar

- `agenda-citas-ui/src/main/java/edu/umg/programacion2/proyecto/ui/VentanaPrincipal.java`.
- `scripts/PruebaInterfaz.java`.
- `probar-ui.cmd` (nuevo).
- `README.md`.
- `docs/PLAN_IMPLEMENTACION.md`.
- `docs/REVISION_FINAL.md` (este informe).

También se regeneraron JAR, reportes y vistas previas. No se modificaron los
documentos originales del instructor ni el contenido de las claves SSH.

## Pendientes

No se detectaron pendientes funcionales del CRUD base tras estas comprobaciones.
La publicación en GitHub queda pendiente por indicación expresa del usuario.
El archivo `examen-parcial-mejoras.md` no fue proporcionado, por lo que las
mejoras exactas del parcial no forman parte de esta implementación.

Para utilizarlo: `ejecutar.cmd`, o ejecutar `MainUI.java` desde Eclipse como
Java Application. Después de cambiar código, compilar con `compilar.cmd` o
Maven `install` desde Eclipse.
