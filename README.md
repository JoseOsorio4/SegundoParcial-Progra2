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

- **Guardar cita** registra los datos en estado Pendiente.
- Fecha y hora: `dd/MM/aaaa HH:mm`, por ejemplo `20/09/2026 14:30`.
- Selecciona una fila para habilitar **Guardar cambios** y **Eliminar**.
- Para cancelar, selecciona **Cancelada** y pulsa **Guardar cambios**: conserva la fila.
- **Eliminar** pide confirmación y borra físicamente la fila seleccionada.
- **Nueva cita** limpia el formulario y prepara otro registro.
- **Refrescar** consulta MySQL. Las cabeceras permiten ordenar.
- La búsqueda filtra el listado; doble clic en una fila muestra su detalle.

## Organización

```text
pom.xml                         Padre Maven, packaging pom
agenda-citas-core/
  pom.xml                       Librería JAR y pruebas
  src/main/java/.../
    modelo/                     Cita y EstadoCita
    config/                     Configuración y conexiones
    dao/                        JDBC y mapeo de filas
    servicio/                   Validación y operaciones de negocio
    excepcion/                  Errores de aplicación
agenda-citas-ui/
  pom.xml                       Dependencia de core y JAR ejecutable
  sql/schema.sql                Esquema comentado
  src/main/java/.../
    MainUI.java                 Inicio de la aplicación
    ui/                         JFrame, formulario y modelo de JTable
```

Flujo: **formulario → servicio → DAO → MySQL → resultado en pantalla**.
Core no importa Swing; UI no importa JDBC. El servicio transforma las excepciones
SQL en mensajes de aplicación. SwingWorker ejecuta JDBC fuera del hilo gráfico.

Una tabla con ID INT AUTO_INCREMENT, cliente VARCHAR(100), servicio VARCHAR(255),
fecha/hora local DATETIME, duración INT y estado ENUM. Todas las columnas son
obligatorias; CHECK respalda textos no vacíos y duración positiva.
El script SQL explica las decisiones.

El servicio valida antes del DAO: textos vacíos o largos, duración positiva,
estado permitido y fecha representable en MySQL. La fecha pasada se rechaza
al **crear**; editar el estado de una cita histórica está permitido.
Las citas nuevas siempre quedan Pendientes.

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
