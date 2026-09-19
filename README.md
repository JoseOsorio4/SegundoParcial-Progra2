# SegundoParcial-Progra2 — Agenda de citas

Aplicación de escritorio de la variante C de Programación II. Permite registrar,
listar, editar y eliminar citas con **Java 11, Swing, Maven multi-módulo y MySQL/JDBC**.

## Ejecutar en esta computadora

La base `segundo_parcial_progra2` y la configuración local ya fueron preparadas.

1. Mantén MySQL en ejecución.
2. Ejecuta `compilar.cmd` si cambiaste el código.
3. Ejecuta `ejecutar.cmd`.

El JAR `agenda-citas-ui/target/agenda-citas-ui-1.0-SNAPSHOT.jar` incluye las
dependencias. Los dos módulos Maven y sus fuentes siguen separados.

## Trabajar en Eclipse

1. **File > Import > Maven > Existing Maven Projects**.
2. En **Root Directory**, selecciona la raíz que contiene este README.
3. Marca los tres POM: padre, `agenda-citas-core` y `agenda-citas-ui`.
4. Pulsa **Finish** y deja que Eclipse resuelva las dependencias.
5. Configura el proyecto con JDK 11 o superior. El POM compila para Java 11;
   el Java con que inicia Eclipse puede ser más reciente.
6. Abre `agenda-citas-ui/src/main/java/edu/umg/programacion2/proyecto/MainUI.java`.
7. Clic derecho > **Run As > Java Application**.

Para mostrar cambios externos: selecciona los proyectos y pulsa **F5**.
Para actualizar dependencias: **Maven > Update Project (Alt+F5)**.
Para construir: proyecto padre > **Run As > Maven build...** > **Goals: install**.

## Preparar en otra computadora

Requisitos: JDK 11+, Maven 3.9.x o Maven integrado en Eclipse, MySQL 8.0.16+
y acceso a internet para descargar dependencias la primera vez. Probado con
Java 11.0.30 y MySQL 26.7.0.

1. Clona el repositorio.
2. Ejecuta `agenda-citas-ui/sql/schema.sql` en MySQL Workbench.
3. Copia `db-example.properties` como `db-local.properties` y configura tu conexión.
4. Ejecuta `mvn install` desde la raíz, o **Maven build > install** en Eclipse.
5. Ejecuta MainUI desde Eclipse, `ejecutar.cmd`, o:

```text
java -jar agenda-citas-ui/target/agenda-citas-ui-1.0-SNAPSHOT.jar
```

La configuración se busca en el directorio actual y sus padres. También puedes
indicar su ruta mediante `-Dagenda.db.config=RUTA` o `AGENDA_DB_CONFIG`.
Las opciones de conexión están destinadas a MySQL local.
**db-local.properties está excluido de Git: no subir contraseñas.**

En Windows, `compilar.cmd` usa Maven instalado o, si está disponible, el Maven
integrado en Eclipse m2e. El segundo caso guarda dependencias en
`.maven-repository` dentro del proyecto; esa caché no se sube.

## Uso

- **Guardar cita** registra una nueva cita. Toda cita nueva se guarda automáticamente en estado **Pendiente**.
- Fecha y hora: `dd/MM/aaaa HH:mm`, por ejemplo `20/09/2026 14:30`.
- Al seleccionar una fila de la tabla, la cita se carga en el formulario y se habilita el modo de edición.
- **Actualizar** guarda los cambios realizados sobre la cita seleccionada.
- Durante la edición se puede cambiar el estado a **Pendiente**, **Confirmada** o **Cancelada**.
- Para cancelar una cita sin eliminarla, selecciona **Cancelada** y pulsa **Actualizar**. La cita permanece almacenada en MySQL.
- **Eliminar** solicita confirmación y borra físicamente el registro seleccionado.
- **Nuevo / Limpiar** abandona el modo de edición y prepara el formulario para registrar una nueva cita.
- **Refrescar** vuelve a consultar los registros almacenados en MySQL.
- Las cabeceras de la tabla permiten ordenar los registros.
- Un clic sobre una fila permite editarla y un **doble clic** abre una ventana con el detalle completo de la cita.
## Organización

```text
pom.xml                         Proyecto padre Maven, packaging pom

agenda-citas-core/
  pom.xml                       Módulo librería JAR
  src/main/java/.../
    modelo/                     Cita y EstadoCita
    config/                     Configuración de la conexión a MySQL
    dao/                        Operaciones JDBC y mapeo de registros
    servicio/                   Validaciones y reglas de negocio
    excepcion/                  Excepciones controladas de la aplicación
  src/test/java/.../            Pruebas unitarias del servicio

agenda-citas-ui/
  pom.xml                       Módulo Swing que depende de core
  sql/schema.sql                Creación y estructura de la base de datos
  src/main/java/.../
    MainUI.java                 Punto de inicio de la aplicación
    ui/
      VentanaPrincipal.java     Ventana principal, formulario y JTable
      ModeloTablaCitas.java     Modelo utilizado por la tabla
      DetalleCitaDialog.java    Ventana con el detalle de una cita
      Estilos.java              Estilos reutilizables de Swing
      IconoVector.java          Iconos utilizados por la interfaz

scripts/
  maven.ps1                     Localiza y ejecuta Maven
  PruebaPersistencia.java       Pruebas opcionales contra MySQL real
  PruebaInterfaz.java           Pruebas opcionales de Swing + MySQL

compilar.cmd                    Compila, prueba e instala los módulos Maven
ejecutar.cmd                    Ejecuta el JAR de la aplicación
probar-bd.cmd                   Ejecuta la prueba de persistencia
probar-ui.cmd                   Ejecuta la prueba de interfaz
db-example.properties           Ejemplo de configuración de conexión

## Comprobaciones

- `mvn test`: pruebas unitarias de reglas y errores, sin MySQL.
- `probar-bd.cmd`: prueba optativa de persistencia real; crea una cita y
  la elimina por su identificador al terminar.
- `probar-ui.cmd`: prueba los componentes de Swing contra MySQL, incluyendo
  formulario, ordenación, errores y confirmación de eliminación. Limpia sus
  propios registros al terminar y genera `.tools/agenda-preview.png`.
- `mvn install` también ejecuta las pruebas unitarias.

Estas pruebas de integración requieren MySQL y `db-local.properties`. No se
ejecutan automáticamente al compilar. El informe final está en
[`docs/REVISION_FINAL.md`](docs/REVISION_FINAL.md).

Antes de presentar, comprueba en pantalla guardar, editar, cancelar, rechazar
y aceptar un borrado, y volver a abrir para confirmar persistencia.
Git guarda código y esquema; los registros de MySQL no se transfieren con Git.
