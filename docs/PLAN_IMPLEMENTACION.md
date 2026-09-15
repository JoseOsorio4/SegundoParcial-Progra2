# Estado y continuidad

Objetivo: SegundoParcial-Progra2, variante C. Continuado el 15 de septiembre de 2026.
Entrega sábado 19 de septiembre de 2026. Eclipse + GitHub + MySQL local.

## Implementado

- Padre Maven y módulos core/ui, modelo, DAO, servicio, excepciones.
- Swing: tabla/formulario, CRUD, confirmaciones, errores, SwingWorker.
- Esquema comentado en ui/sql/schema.sql y BD segundo_parcial_progra2 creada.
- Configuración local fuera de Git; nunca copiar su contraseña a esta nota.
- Comandos compilar.cmd, ejecutar.cmd, probar-bd.cmd, probar-ui.cmd y JAR ejecutable.
- README con Eclipse, preparación, uso y decisiones.

## Verificación

- Maven 3.9.11 / Java 11.0.30: install correcto.
- 14 pruebas JUnit: cero fallos y errores.
- 20 comprobaciones MySQL 26.7.0: CRUD, segunda conexión, actualización sin
  cambios, cancelación histórica, validaciones, CHECK SQL, registro ausente
  y fallo de conexión. Registro de prueba eliminado.
- 30 comprobaciones Swing + MySQL correctas: formulario, ordenación,
  validaciones, cancelación, borrado confirmado, error de conexión y tamaño mínimo.
- Revisión visual completada a tamaño inicial y mínimo; corregido recorte de Estado.
- Revisión técnica y documental finalizada. Consultar REVISION_FINAL.md.

## Entorno

Abrir la carpeta raíz del proyecto como proyecto Maven en Eclipse.
El proyecto compila para Java 11.
Workbench: conexión JoseOsorio, host 127.0.0.1, puerto 3306, usuario root.
Configuración: db-local.properties, ignorada por Git.

Maven no está en PATH. scripts/maven.ps1 usa Maven disponible o runtime m2e
de .p2/pool/plugins, con caché local .maven-repository. Puede aparecer aviso al
cerrar el puente m2e fuera de Eclipse; comprobar BUILD SUCCESS y exit code.

## GitHub

El usuario proporcionó git@github.com:JoseOsorio4/SegundoParcial-Progra2.git
y pidió explícitamente NO subir el proyecto todavía. Mantener todo local.
No configurar ni publicar cambios remotos sin nueva instrucción.
La clave SSH privada no debe leerse ni copiarse. No subir secretos ni cachés.

## Asignación

Fuentes del escritorio: README.md, variante-C-citas-servicio.md y
comentario para proyecto de progra.txt. Fotos de libros como referencia.
README restringe generación completa de SQL/DAO con IA y pide diseño propio
comprensible; esto ya se explicó al usuario. Sus instrucciones actuales
priorizan implementación rápida. Explicar decisiones y respetar su dirección.

Requisitos: PreparedStatement, separación core/UI, validación previa, errores
sin crash ni stacktrace en pantalla, esquema justificado, CRUD Swing.
Cancelar conserva; eliminar borra con confirmación.
Fecha futura exigida al crear; edición histórica permitida.

Parcial: dos mejoras en dos horas según último dígito del carné.
Falta examen-parcial-mejoras.md; no inventar ejercicios.
