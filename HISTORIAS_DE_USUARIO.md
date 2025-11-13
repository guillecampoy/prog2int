# Historias de Usuario – TP Integrador Paciente ↔ Historia Clínica

## Épica 1: Gestión de Pacientes

### HU-001: Crear Paciente

**Como** usuario del sistema  
**Quiero** crear un registro de paciente con sus datos básicos  
**Para** almacenar información del paciente en la base de datos

#### Criterios de Aceptación

```gherkin
Escenario: Crear paciente exitosamente
  Dado que el usuario completa nombre "Juan", apellido "Pérez", DNI "12345678" y fecha de nacimiento "1990-01-01"
  Cuando confirma la creación
  Entonces el sistema crea el paciente con un ID generado automáticamente
  Y marca eliminado = FALSE por defecto

Escenario: Intento de crear paciente con DNI duplicado
  Dado que existe un paciente con DNI "12345678"
  Cuando el usuario intenta crear otro paciente con el mismo DNI
  Entonces el sistema muestra "Ya existe un paciente con el DNI: 12345678"
  Y no crea el registro

Escenario: Intento de crear paciente con campos obligatorios vacíos
  Dado que el usuario selecciona "Crear paciente"
  Cuando deja el nombre vacío (solo espacios o Enter)
  Entonces el sistema muestra "El nombre no puede estar vacío"
  Y no crea el registro

Escenario: Intento de crear paciente con fecha de nacimiento futura
  Dado que el usuario ingresa como fecha de nacimiento mañana
  Cuando confirma la creación
  Entonces el sistema muestra "La fecha de nacimiento no puede ser futura"
  Y no crea el registro
```

#### Reglas de Negocio Aplicables

- **RN-001**: Nombre, apellido y DNI son obligatorios.
- **RN-002**: El DNI debe ser único en el sistema (max 15 caracteres).
- **RN-003**: Espacios iniciales y finales se eliminan automáticamente con `trim()`.
- **RN-004**: El ID se genera automáticamente por la base de datos.
- **RN-005**: La Historia Clínica es opcional durante la creación del paciente (se puede asociar luego).
- **RN-006**: `fechaNacimiento` no puede ser futura.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.crearPaciente()`.
- **Servicio**: `PacienteServiceImpl.insertar(Paciente paciente)`.
- **Validación**: `PacienteServiceImpl.validatePaciente()` + `validateDniUnique()`.
- **DAO**: `PacienteDao.crear(Paciente paciente, Connection con)`.
- **Flujo**:
  1. Capturar entradas con `scanner.nextLine().trim()`.
  2. Validar campos obligatorios y formato de `dni`.
  3. Validar que `fechaNacimiento` ≤ fecha actual.
  4. Insertar paciente usando `PreparedStatement` y `RETURN_GENERATED_KEYS`.
  5. Mantener `historiaClinica` en `null` en esta HU.

---

### HU-002: Listar Todos los Pacientes

**Como** usuario del sistema  
**Quiero** ver un listado de todos los pacientes registrados  
**Para** consultar la información almacenada rápidamente

#### Criterios de Aceptación

```gherkin
Escenario: Listar todos los pacientes con historia clínica
  Dado que existen pacientes activos con historia clínica asociada
  Cuando el usuario selecciona "Listar pacientes" y elige opción "1" (listar todos)
  Entonces el sistema muestra todos los pacientes con eliminado = FALSE
  Y para cada paciente con historia clínica muestra "Historia Clínica: <nroHistoria>"

Escenario: Listar pacientes sin historia clínica
  Dado que existen pacientes activos sin historia clínica asociada
  Cuando el usuario lista todos los pacientes
  Entonces el sistema muestra esos pacientes
  Y no muestra ninguna línea de historia clínica para ellos

Escenario: No hay pacientes en el sistema
  Dado que no existen pacientes activos
  Cuando el usuario lista todos los pacientes
  Entonces el sistema muestra "No se encontraron pacientes."
```

#### Reglas de Negocio Aplicables

- **RN-007**: Solo se listan pacientes con `eliminado = FALSE`.
- **RN-008**: La historia clínica se obtiene mediante `LEFT JOIN` entre paciente e historia_clinica.
- **RN-009**: Si el paciente no tiene historia clínica asociada, no se muestra información de historia clínica.
- **RN-010**: Los resultados deben estar ordenados por apellido y nombre.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.listarPacientes()` (subopción "Listar todos").
- **Servicio**: `PacienteServiceImpl.getAll()`.
- **DAO**: `PacienteDao.getAll()` con `SELECT` + `LEFT JOIN` a `HistoriaClinica`.
- **Query base** (ejemplo):
  ```sql
  SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento,
         p.eliminado,
         hc.id AS hc_id, hc.nro_historia, hc.grupo_sanguineo
  FROM pacientes p
  LEFT JOIN historias_clinicas hc ON hc.paciente_id = p.id AND hc.eliminado = FALSE
  WHERE p.eliminado = FALSE
  ORDER BY p.apellido, p.nombre;
  ```

---

### HU-003: Buscar Pacientes por Nombre o Apellido

**Como** usuario del sistema  
**Quiero** buscar pacientes por nombre o apellido  
**Para** encontrar rápidamente un paciente específico

#### Criterios de Aceptación

```gherkin
Escenario: Buscar por nombre con coincidencia parcial
  Dado que existen pacientes "Juan Pérez" y "Juana García"
  Cuando el usuario busca por "Jua"
  Entonces el sistema muestra ambos pacientes

Escenario: Buscar por apellido exacto
  Dado que existen pacientes "Juan Pérez" y "María Pérez"
  Cuando el usuario busca por "Pérez"
  Entonces el sistema muestra ambos pacientes

Escenario: Búsqueda sin resultados
  Dado que no existen pacientes con apellido "Rodríguez"
  Cuando el usuario busca por "Rodríguez"
  Entonces el sistema muestra "No se encontraron pacientes."

Escenario: Búsqueda con espacios extra
  Dado que el usuario busca por "  Juan  "
  Cuando se ejecuta la búsqueda
  Entonces el sistema elimina espacios y busca por "Juan"
```

#### Reglas de Negocio Aplicables

- **RN-011**: La búsqueda es `case-insensitive`.
- **RN-012**: Se busca con `LIKE %texto%`.
- **RN-013**: Se filtra solo sobre pacientes con `eliminado = FALSE`.
- **RN-014**: No se permiten búsquedas vacías después de `trim()`.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.listarPacientes()` (subopción "Buscar por nombre/apellido").
- **Servicio**: `PacienteServiceImpl.buscarPorNombreApellido(String filtro)`.
- **DAO**: `PacienteDao.buscarPorNombreApellido(String filtro)`.
- **Query**:
  ```sql
  WHERE p.eliminado = FALSE
    AND (LOWER(p.nombre) LIKE LOWER(?) OR LOWER(p.apellido) LIKE LOWER(?))
  ```
- **Parámetros**: `%filtro%` en ambos placeholders.

---

### HU-004: Actualizar Datos de Paciente

**Como** usuario del sistema  
**Quiero** modificar los datos de un paciente existente  
**Para** mantener la información actualizada

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar solo apellido
  Dado que existe paciente con ID 1, apellido "Pérez"
  Cuando el usuario actualiza el paciente ID 1
  Y deja nombre y DNI vacíos (solo Enter)
  Y escribe "González" en apellido
  Entonces el sistema actualiza solo el apellido
  Y mantiene nombre y DNI sin cambios

Escenario: Actualizar con DNI duplicado
  Dado que existen pacientes con DNI "111" y "222"
  Cuando el usuario intenta cambiar el DNI del paciente con DNI "222" a "111"
  Entonces el sistema muestra "Ya existe un paciente con el DNI: 111"
  Y no actualiza el registro

Escenario: Actualizar con misma fecha de nacimiento y cambio de domicilio
  Dado que existe paciente con fecha de nacimiento ya registrada
  Cuando el usuario mantiene la misma fecha de nacimiento
  Y solo modifica domicilio
  Entonces el sistema actualiza domicilio y conserva la fecha de nacimiento

Escenario: Intentar actualizar con fecha de nacimiento futura
  Dado que existe paciente con fecha de nacimiento "1990-01-01"
  Cuando el usuario intenta cambiarla a mañana
  Entonces el sistema muestra "La fecha de nacimiento no puede ser futura"
  Y no actualiza el registro
```

#### Reglas de Negocio Aplicables

- **RN-015**: Se valida DNI único excepto para el mismo paciente.
- **RN-016**: Campos vacíos (Enter) mantienen el valor original.
- **RN-017**: Se requiere ID válido (> 0) para actualizar.
- **RN-018**: `trim()` se aplica antes de validar si un campo está vacío.
- **RN-019**: `fechaNacimiento` no puede ser futura.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.actualizarPaciente()`.
- **Servicio**: `PacienteServiceImpl.actualizar(Paciente paciente)`.
- **Validación**: `validateDniUnique(dni, pacienteId)` permite el mismo DNI del paciente.
- **Pattern**:
  ```java
  String nombre = scanner.nextLine().trim();
  if (!nombre.isEmpty()) {
      paciente.setNombre(nombre);
  }
  ```

---

## Épica 2: Gestión de Historias Clínicas

### HU-005: Crear Historia Clínica para Paciente Existente

**Como** usuario del sistema  
**Quiero** crear una historia clínica para un paciente existente  
**Para** registrar su información médica inicial

#### Criterios de Aceptación

```gherkin
Escenario: Crear historia clínica para paciente sin historia asociada
  Dado que existe un paciente con ID 1 sin historia clínica
  Cuando el usuario ingresa nroHistoria "HC-0001", grupo sanguíneo "O+", antecedentes y medicación actual
  Entonces el sistema crea la historia clínica
  Y la asocia al paciente ID 1

Escenario: Intentar crear historia clínica para paciente ya asociado
  Dado que el paciente con ID 2 ya tiene una historia clínica
  Cuando el usuario intenta crear una nueva historia clínica para el paciente ID 2
  Entonces el sistema muestra "El paciente ya posee una historia clínica asociada."
  Y no crea el registro

Escenario: Intentar crear historia clínica con número duplicado
  Dado que existe una historia clínica con nroHistoria "HC-0001"
  Cuando el usuario intenta crear otra historia clínica con el mismo nroHistoria
  Entonces el sistema muestra "Ya existe una historia clínica con el número: HC-0001"
  Y no crea el registro

Escenario: Intentar crear historia clínica con grupo sanguíneo inválido
  Dado que el usuario ingresa grupo sanguíneo "X0"
  Cuando confirma la creación
  Entonces el sistema muestra "Grupo sanguíneo inválido. Valores permitidos: A+, A-, B+, B-, AB+, AB-, O+, O-"
  Y no crea el registro
```

#### Reglas de Negocio Aplicables

- **RN-020**: El paciente debe existir y estar con `eliminado = FALSE`.
- **RN-021**: Cada paciente puede tener como máximo una historia clínica.
- **RN-022**: `nroHistoria` es único en el sistema (max 20 caracteres).
- **RN-023**: `grupoSanguineo` debe ser uno de `{A+, A-, B+, B-, AB+, AB-, O+, O-}`.
- **RN-024**: Los campos de texto largos (`antecedentes`, `medicacionActual`, `observaciones`) pueden ser nulos, pero se almacenan con `trim()`.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.crearHistoriaClinica()`.
- **Servicio**: `HistoriaClinicaServiceImpl.insertar(Long pacienteId, HistoriaClinica hc)`.
- **DAO**: `HistoriaClinicaDao.crear(HistoriaClinica hc, Connection con)`.
- **Validaciones**:
  - Verificar existencia de paciente por ID.
  - Verificar que no haya ya una historia clínica asociada a ese paciente.
  - Validar unicidad de `nroHistoria` y el enum de `grupoSanguineo`.

---

### HU-006: Actualizar Historia Clínica

**Como** usuario del sistema  
**Quiero** actualizar los datos de una historia clínica existente  
**Para** reflejar cambios en el estado de salud del paciente

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar solo observaciones
  Dado que existe una historia clínica con ID 10
  Cuando el usuario deja vacíos antecedentes y medicación actual
  Y escribe un nuevo texto en observaciones
  Entonces el sistema actualiza solo las observaciones
  Y mantiene los demás campos sin cambios

Escenario: Intentar cambiar grupo sanguíneo a un valor inválido
  Dado que existe una historia clínica con grupo sanguíneo "A+"
  Cuando el usuario intenta cambiarlo a "Z+"
  Entonces el sistema muestra "Grupo sanguíneo inválido. Valores permitidos: A+, A-, B+, B-, AB+, AB-, O+, O-"
  Y no actualiza el registro

Escenario: Intentar actualizar historia clínica eliminada
  Dado que la historia clínica con ID 11 tiene eliminado = TRUE
  Cuando el usuario intenta actualizarla
  Entonces el sistema muestra "La historia clínica se encuentra eliminada."
  Y no aplica cambios

Escenario: Mantener nroHistoria sin cambios
  Dado que existe una historia clínica con nroHistoria "HC-0005"
  Cuando el usuario actualiza antecedentes y medicación actual
  Y deja vacío el campo nroHistoria (solo Enter)
  Entonces el sistema mantiene el mismo nroHistoria
```

#### Reglas de Negocio Aplicables

- **RN-025**: Solo se pueden actualizar historias clínicas con `eliminado = FALSE`.
- **RN-026**: `nroHistoria` solo puede cambiarse si el nuevo valor es único.
- **RN-027**: `grupoSanguineo` debe seguir perteneciendo al conjunto permitido.
- **RN-028**: Campos de texto vacíos (Enter) mantienen el valor anterior.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.actualizarHistoriaClinica()`.
- **Servicio**: `HistoriaClinicaServiceImpl.actualizar(HistoriaClinica hc)`.
- **DAO**: `HistoriaClinicaDao.actualizar(HistoriaClinica hc, Connection con)`.
- Validaciones análogas al paciente: usar patrón de "Enter mantiene valor actual".

---

### HU-007: Listar Historias Clínicas

**Como** usuario del sistema  
**Quiero** listar todas las historias clínicas activas  
**Para** tener una vista rápida del estado médico de los pacientes

#### Criterios de Aceptación

```gherkin
Escenario: Listar historias clínicas activas
  Dado que existen historias clínicas con eliminado = FALSE
  Cuando el usuario selecciona "Listar historias clínicas"
  Entonces el sistema muestra una tabla con nroHistoria, grupo sanguíneo y nombre completo del paciente asociado

Escenario: No hay historias clínicas activas
  Dado que todas las historias clínicas están eliminadas lógicamente
  Cuando el usuario selecciona "Listar historias clínicas"
  Entonces el sistema muestra "No se encontraron historias clínicas activas."
```

#### Reglas de Negocio Aplicables

- **RN-029**: Solo se listan historias clínicas con `eliminado = FALSE`.
- **RN-030**: Siempre se muestra al menos nombre, apellido y DNI del paciente asociado.
- **RN-031**: Si el paciente está eliminado pero su historia clínica no, debe acordarse la política (sugerido: no listar o marcar claramente).

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.listarHistoriasClinicas()`.
- **Servicio**: `HistoriaClinicaServiceImpl.getAllActivas()`.
- **DAO**: `HistoriaClinicaDao.getAllActivas()` con JOIN a Paciente.
- **Query base**:
  ```sql
  SELECT hc.id, hc.nro_historia, hc.grupo_sanguineo,
         p.nombre, p.apellido, p.dni
  FROM historias_clinicas hc
  JOIN pacientes p ON hc.paciente_id = p.id
  WHERE hc.eliminado = FALSE
  ORDER BY hc.nro_historia;
  ```

---

### HU-008: Eliminar (Baja Lógica) Historia Clínica

**Como** usuario del sistema  
**Quiero** eliminar lógicamente una historia clínica  
**Para** evitar que siga apareciendo en los listados habituales

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar historia clínica existente
  Dado que existe historia clínica con ID 5 y eliminado = FALSE
  Cuando el usuario la elimina
  Entonces el sistema actualiza eliminado = TRUE
  Y muestra "Historia clínica eliminada exitosamente."

Escenario: Eliminar historia clínica inexistente
  Dado que no existe historia clínica con ID 999
  Cuando el usuario intenta eliminarla
  Entonces el sistema muestra "No se encontró historia clínica con ID: 999"

Escenario: Historia clínica eliminada no aparece en listados
  Dado que se eliminó la historia clínica con ID 5
  Cuando el usuario lista historias clínicas activas
  Entonces la historia clínica con ID 5 no aparece en los resultados
```

#### Reglas de Negocio Aplicables

- **RN-032**: La eliminación es lógica (`eliminado = TRUE`), no física.
- **RN-033**: Se verifica `rowsAffected` para confirmar la operación.
- **RN-034**: No se permite reactivar desde esta HU (eventual HU futura).

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.eliminarHistoriaClinica()`.
- **Servicio**: `HistoriaClinicaServiceImpl.eliminar(Long id)`.
- **DAO**: `HistoriaClinicaDao.eliminar(Long id, Connection con)`.
- **Query**:
  ```sql
  UPDATE historias_clinicas SET eliminado = TRUE WHERE id = ?;
  ```

---

## Épica 3: Integridad y Transacciones Paciente–Historia Clínica (1→1)

### HU-009: Crear Paciente y Historia Clínica en una Única Transacción

**Como** usuario del sistema  
**Quiero** crear un paciente y su historia clínica en una sola operación  
**Para** garantizar la consistencia de los datos

#### Criterios de Aceptación

```gherkin
Escenario: Creación integrada exitosa
  Dado que el usuario ingresa datos válidos de paciente y de historia clínica
  Cuando confirma la operación "Crear paciente + historia clínica"
  Entonces el sistema crea el paciente
  Y crea la historia clínica asociada
  Y realiza commit de la transacción

Escenario: Falla por DNI duplicado
  Dado que ya existe un paciente con DNI "12345678"
  Cuando el usuario intenta crear otro paciente con el mismo DNI más una historia clínica
  Entonces el sistema muestra el error de DNI duplicado
  Y realiza rollback
  Y no se crea ni el nuevo paciente ni la historia clínica

Escenario: Falla por nroHistoria duplicado
  Dado que ya existe una historia clínica con nroHistoria "HC-0002"
  Cuando el usuario intenta crear una nueva pareja paciente + historia clínica con ese mismo nroHistoria
  Entonces el sistema muestra el error de número de historia duplicado
  Y realiza rollback
  Y no se crea ni el paciente ni la historia clínica
```

#### Reglas de Negocio Aplicables

- **RN-035**: La operación se ejecuta en una única transacción (`setAutoCommit(false)`).
- **RN-036**: Si falla cualquiera de las validaciones o inserts, se ejecuta `rollback()` y no queda nada a medio camino.
- **RN-037**: El servicio de alto nivel orquesta ambos DAO con la misma `Connection`.
- **RN-038**: Se reutilizan las validaciones de paciente e historia clínica ya definidas.

#### Implementación Técnica Sugerida

- **Servicio orquestador**: `PacienteHistoriaClinicaService.crearPacienteConHistoria(Paciente p, HistoriaClinica hc)`.
- Flujo:
  1. Abrir conexión.
  2. `setAutoCommit(false)`.
  3. Validar paciente e historia clínica.
  4. Insertar paciente y obtener ID generado.
  5. Setear `paciente_id` en historia clínica y crearla.
  6. `commit()` si todo OK, `rollback()` ante cualquier excepción.
  7. Restaurar `autoCommit(true)` y cerrar recursos.

---

### HU-010: Asociar Historia Clínica a Paciente sin Historia desde la Actualización

**Como** usuario del sistema  
**Quiero** asociar una historia clínica a un paciente que aún no la tiene  
**Para** completar su información médica sin recrear el paciente

#### Criterios de Aceptación

```gherkin
Escenario: Asociar historia clínica a paciente sin historia
  Dado que el paciente ID 3 no tiene historia clínica asociada
  Cuando el usuario ingresa al menú de actualización del paciente
  Y responde "S" a la opción "¿Desea crear historia clínica?"
  Y completa datos válidos de historia clínica
  Entonces el sistema crea la historia clínica
  Y la asocia al paciente ID 3 en una transacción

Escenario: Intentar asociar historia a paciente que ya tiene una
  Dado que el paciente ID 4 ya posee historia clínica
  Cuando el usuario responde "S" a la opción "¿Desea crear historia clínica?"
  Entonces el sistema muestra "El paciente ya posee una historia clínica."
  Y no crea ni asocia una nueva
```

#### Reglas de Negocio Aplicables

- **RN-039**: Solo se puede asociar historia clínica si el paciente no tiene una.
- **RN-040**: La operación completa (crear historia + asociar) debe ser transaccional.
- **RN-041**: Se reutilizan las validaciones de `nroHistoria` y `grupoSanguineo`.

#### Implementación Técnica Sugerida

- Extender `MenuHandler.actualizarPaciente()` para incluir la pregunta de asociar historia clínica.
- Llamar a `PacienteHistoriaClinicaService.crearHistoriaParaPacienteExistente(pacienteId, HistoriaClinica hc)` que use transacciones.

---

### HU-011: Impedir Múltiples Historias Clínicas por Paciente

**Como** administrador del sistema  
**Quiero** asegurar que un paciente tenga como máximo una historia clínica  
**Para** cumplir la relación 1→1 definida en el dominio

#### Criterios de Aceptación

```gherkin
Escenario: Intentar crear segunda historia clínica desde menú de historias
  Dado que el paciente ID 7 ya tiene una historia clínica
  Cuando el usuario intenta crear otra historia clínica seleccionando paciente ID 7
  Entonces el sistema muestra "El paciente ya posee una historia clínica asociada."
  Y no crea el registro

Escenario: Intentar crear segunda historia clínica desde creación integrada
  Dado que el paciente con DNI "99999999" ya tiene historia clínica
  Cuando el usuario intenta usar la opción "Crear paciente + historia clínica" con el mismo DNI
  Entonces el sistema muestra que el DNI ya existe
  Y no crea una nueva historia clínica
```

#### Reglas de Negocio Aplicables

- **RN-042**: El sistema debe validar la regla 1→1 en todas las rutas posibles (creación aislada, integrada, actualización).
- **RN-043**: La base de datos debe reforzar la regla con una restricción de unicidad (`UNIQUE(paciente_id)` o PK compartida).
- **RN-044**: Ante violación de la restricción de base, se captura la excepción y se traduce a un mensaje legible.

#### Implementación Técnica Sugerida

- Agregar constraint en la tabla `historias_clinicas`:
  ```sql
  UNIQUE (paciente_id)
  ```
- Manejar `SQLIntegrityConstraintViolationException` en `HistoriaClinicaDao` y mapearla a mensaje de negocio.

---

### HU-012: Baja Lógica Consistente de Paciente y Historia Clínica

**Como** usuario del sistema  
**Quiero** eliminar lógicamente un paciente y su historia clínica de forma consistente  
**Para** que no aparezcan en listados habituales manteniendo la integridad

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar paciente con historia clínica
  Dado que existe paciente ID 10 con historia clínica activa
  Cuando el usuario selecciona "Eliminar paciente" para ID 10
  Entonces el sistema marca eliminado = TRUE en el paciente
  Y marca eliminado = TRUE en la historia clínica asociada
  Y realiza commit de la transacción

Escenario: Eliminar paciente sin historia clínica
  Dado que existe paciente ID 11 sin historia clínica
  Cuando el usuario lo elimina
  Entonces el sistema marca eliminado = TRUE solo en el paciente
  Y realiza commit de la transacción

Escenario: Falla durante actualización de historia clínica
  Dado que el sistema simula un error al marcar eliminada la historia clínica asociada
  Cuando el usuario intenta eliminar el paciente
  Entonces el sistema ejecuta rollback
  Y no queda el paciente eliminado si la historia clínica no pudo ser actualizada
```

#### Reglas de Negocio Aplicables

- **RN-045**: La baja lógica de paciente + historia clínica debe ser transaccional.
- **RN-046**: No se permite que quede paciente eliminado con historia clínica activa, ni al revés, por esta HU.
- **RN-047**: Los listados por defecto deben excluir registros con `eliminado = TRUE`.

#### Implementación Técnica Sugerida

- **Servicio**: `PacienteHistoriaClinicaService.eliminarPacienteConHistoria(Long pacienteId)`.
- Flujo:
  1. Abrir conexión y `setAutoCommit(false)`.
  2. Marcar `paciente.eliminado = TRUE`.
  3. Si existe historia clínica asociada, marcar `hc.eliminado = TRUE`.
  4. `commit()` si todo sale bien; `rollback()` ante fallos.

---

## Épica 4: Consultas y Búsquedas Clínicas

### HU-013: Buscar Paciente por DNI

**Como** usuario del sistema  
**Quiero** buscar un paciente por su DNI  
**Para** acceder rápidamente a su registro y a su historia clínica

#### Criterios de Aceptación

```gherkin
Escenario: Búsqueda exitosa por DNI
  Dado que existe un paciente con DNI "30111222"
  Cuando el usuario ingresa "30111222" en la opción "Buscar por DNI"
  Entonces el sistema muestra los datos del paciente
  Y si tiene historia clínica asociada muestra su nroHistoria

Escenario: Búsqueda por DNI con espacios
  Dado que existe un paciente con DNI "30111222"
  Cuando el usuario ingresa " 30111222  "
  Entonces el sistema elimina los espacios
  Y encuentra al paciente correctamente

Escenario: Búsqueda sin resultados
  Dado que no existe paciente con DNI "99999999"
  Cuando el usuario realiza la búsqueda
  Entonces el sistema muestra "No se encontró paciente con el DNI: 99999999"

Escenario: Búsqueda con DNI vacío
  Dado que el usuario presiona Enter sin escribir nada
  Cuando el sistema valida la entrada
  Entonces muestra "El DNI no puede estar vacío"
  Y no ejecuta la consulta
```

#### Reglas de Negocio Aplicables

- **RN-048**: El DNI se busca exactamente (no por patrón).
- **RN-049**: El DNI no puede ser vacío tras aplicar `trim()`.
- **RN-050**: Solo se consideran pacientes con `eliminado = FALSE`.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.buscarPacientePorDni()`.
- **Servicio**: `PacienteServiceImpl.buscarPorDni(String dni)`.
- **DAO**: `PacienteDao.buscarPorDni(String dni)`.

---

### HU-014: Buscar Historia Clínica por Número

**Como** usuario del sistema  
**Quiero** buscar una historia clínica por su número  
**Para** acceder directamente al registro médico asociado

#### Criterios de Aceptación

```gherkin
Escenario: Búsqueda exitosa por nroHistoria
  Dado que existe una historia clínica con nroHistoria "HC-0010"
  Cuando el usuario ingresa "HC-0010"
  Entonces el sistema muestra los datos de la historia clínica
  Y el nombre completo del paciente asociado

Escenario: Búsqueda sin resultados
  Dado que no existe historia clínica con nroHistoria "HC-9999"
  Cuando el usuario realiza la búsqueda
  Entonces el sistema muestra "No se encontró historia clínica con número: HC-9999"
```

#### Reglas de Negocio Aplicables

- **RN-051**: `nroHistoria` se busca por coincidencia exacta (respetando unicidad).
- **RN-052**: Solo se devuelven historias clínicas con `eliminado = FALSE`.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.buscarHistoriaClinicaPorNumero()`.
- **Servicio**: `HistoriaClinicaServiceImpl.buscarPorNumero(String nroHistoria)`.
- **DAO**: `HistoriaClinicaDao.buscarPorNumero(String nroHistoria)`.

---

### HU-015: Listar Pacientes por Grupo Sanguíneo

**Como** profesional de la salud  
**Quiero** listar pacientes por grupo sanguíneo  
**Para** identificar rápidamente a pacientes compatibles ante una necesidad específica

#### Criterios de Aceptación

```gherkin
Escenario: Listar pacientes con grupo sanguíneo dado
  Dado que existen pacientes con historias clínicas con grupo sanguíneo "O+"
  Cuando el usuario selecciona grupo "O+"
  Entonces el sistema lista nombre, apellido, DNI y nroHistoria de esos pacientes

Escenario: No hay pacientes para el grupo sanguíneo seleccionado
  Dado que no existen pacientes con historias clínicas de grupo "AB-"
  Cuando el usuario selecciona grupo "AB-"
  Entonces el sistema muestra "No se encontraron pacientes para el grupo sanguíneo AB-"
```

#### Reglas de Negocio Aplicables

- **RN-053**: Solo se consideran historias clínicas con `eliminado = FALSE`.
- **RN-054**: El grupo sanguíneo debe validarse contra el conjunto permitido antes de ejecutar la consulta.
- **RN-055**: Pacientes eliminados no deben aparecer en este listado.

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.listarPacientesPorGrupoSanguineo()`.
- **Servicio**: `PacienteServiceImpl.getByGrupoSanguineo(String grupo)`.
- **DAO**: método que haga `JOIN` con `historias_clinicas`.

---

### HU-016: Listar Pacientes Incluyendo Eliminados (Auditoría)

**Como** usuario con rol de auditor  
**Quiero** listar pacientes incluyendo los eliminados  
**Para** revisar el historial de altas y bajas lógicas

#### Criterios de Aceptación

```gherkin
Escenario: Listar pacientes activos e inactivos
  Dado que existen pacientes con eliminado = FALSE y eliminado = TRUE
  Cuando el usuario selecciona "Listar pacientes (modo auditor)"
  Entonces el sistema muestra todos los pacientes
  Y para cada paciente indica claramente "ESTADO: ACTIVO" o "ESTADO: ELIMINADO"

Escenario: No hay pacientes registrados
  Dado que la base de pacientes está vacía
  Cuando el usuario lista en modo auditor
  Entonces el sistema muestra "No se encontraron pacientes."
```

#### Reglas de Negocio Aplicables

- **RN-056**: Esta opción debe estar separada del listado por defecto (solo activos).
- **RN-057**: El estado debe ser claramente visible en la salida (ej. etiqueta ACTIVO/ELIMINADO).
- **RN-058**: Esta funcionalidad está pensada para perfiles autorizados (regla de negocio, aunque no se implemente seguridad compleja en la materia).

#### Implementación Técnica Sugerida

- **Clase menú**: `MenuHandler.listarPacientesAuditoria()`.
- **Servicio**: `PacienteServiceImpl.getAllIncluyendoEliminados()`.
- **DAO**: `PacienteDao.getAllIncluyendoEliminados()` sin filtro de `eliminado`.

---

## Reglas de Negocio

### Validación de Datos

| Código | Regla | Implementación |
|--------|-------|----------------|
| RN-001 | Nombre, apellido y DNI de paciente son obligatorios | `PacienteServiceImpl.validatePaciente()` valida campos no nulos ni vacíos |
| RN-002 | DNI debe ser único en el sistema (hasta 15 caracteres) | `PacienteServiceImpl.validateDniUnique()` + constraint `UNIQUE(dni)` en tabla `pacientes` |
| RN-003 | Se eliminan espacios iniciales y finales en entradas de texto | Se aplica `.trim()` en `MenuHandler` antes de invocar a la capa service |
| RN-006 | La fecha de nacimiento no puede ser futura | Comparación contra la fecha del sistema en `validatePaciente()` y `actualizar()` |
| RN-011 | Búsquedas por nombre/apellido son case-insensitive | Uso de `LOWER()` en SQL y/o collation de MySQL en `PacienteDao.buscarPorNombreApellido()` |
| RN-014 | No se permiten búsquedas vacías tras aplicar `trim()` | Validación en `PacienteServiceImpl.buscarPorNombreApellido()` y `buscarPorDni()` |
| RN-015 | En actualización se valida que el nuevo DNI siga siendo único | `validateDniUnique(dni, pacienteId)` permite el mismo DNI sólo para el propio paciente |
| RN-018 | Antes de validar si un campo está vacío se aplica `trim()` | Normalización de entradas en `MenuHandler` y services |
| RN-019 | Al actualizar paciente, la nueva fecha de nacimiento tampoco puede ser futura | Misma regla que RN-006 aplicada en `PacienteServiceImpl.actualizar()` |
| RN-020 | La historia clínica sólo puede crearse para pacientes existentes y no eliminados | `HistoriaClinicaServiceImpl.insertar()` consulta y valida el paciente antes de insertar |
| RN-022 | El número de historia (`nro_historia`) es único (hasta 20 caracteres) | Validación en `HistoriaClinicaServiceImpl.validateHistoria()` + constraint `UNIQUE(nro_historia)` |
| RN-023 | El grupo sanguíneo debe pertenecer al conjunto permitido | Validación contra enum/lista en `HistoriaClinicaServiceImpl.validateGrupoSanguineo()` |
| RN-024 | Campos largos (antecedentes, medicación, observaciones) admiten nulos pero se almacenan trimeados | Normalización en `HistoriaClinicaServiceImpl` antes de persistir |
| RN-027 | Cualquier cambio de grupo sanguíneo debe respetar el set permitido | Se reutiliza validación de RN-023 en actualización de historia clínica |
| RN-028 | En actualización de historia clínica, Enter mantiene el valor anterior | Patrón `if (!input.isEmpty()) { ... }` en `MenuHandler.actualizarHistoriaClinica()` |
| RN-048 | La búsqueda por DNI es exacta (no patrón) | SQL `WHERE dni = ?` en `PacienteDao.buscarPorDni()` |
| RN-049 | El DNI no puede ser vacío tras `trim()` | Validación en `PacienteServiceImpl.buscarPorDni()` |
| RN-051 | La búsqueda de historia clínica por número es exacta | SQL `WHERE nro_historia = ?` en `HistoriaClinicaDao.buscarPorNumero()` |
| RN-054 | Antes de listar por grupo sanguíneo se valida el valor ingresado | `PacienteServiceImpl.getByGrupoSanguineo()` verifica el grupo contra el enum permitido |

### Listados, Búsquedas y Soft Delete

| Código | Regla | Implementación |
|--------|-------|----------------|
| RN-007 | Los listados de pacientes por defecto sólo muestran `eliminado = FALSE` | Cláusula `WHERE eliminado = FALSE` en las SELECT estándar de `PacienteDao` |
| RN-008 | La historia clínica se trae con `LEFT JOIN` para pacientes sin historia | Query con `LEFT JOIN historias_clinicas` en `PacienteDao.getAll()` |
| RN-009 | Si el paciente no tiene historia clínica, no se imprime información clínica | En el mapper de resultados se permiten `NULL` y se omite la sección clínica en `MenuHandler` |
| RN-010 | El listado de pacientes ordena por apellido y nombre | `ORDER BY p.apellido, p.nombre` en las consultas de listado |
| RN-013 | Todas las búsquedas de pacientes excluyen registros eliminados | `WHERE eliminado = FALSE` en consultas de búsqueda |
| RN-029 | Listar historias clínicas muestra sólo registros con `eliminado = FALSE` | `HistoriaClinicaDao.getAllActivas()` filtra por `eliminado = FALSE` |
| RN-030 | En listados de historias se incluye siempre nombre, apellido y DNI del paciente | `JOIN pacientes` en todas las consultas de `HistoriaClinicaDao` que listan historias |
| RN-031 | Si existieran historias activas de pacientes eliminados debe definirse política explícita | Por defecto se sugiere no listarlas en `getAllActivas()` o identificarlas claramente |
| RN-047 | Los listados "normales" no muestran registros con baja lógica | Convención aplicada en todos los DAO de lectura (pacientes e historias) |
| RN-050 | Las búsquedas por DNI trabajan sólo sobre pacientes activos | Filtro `eliminado = FALSE` en `PacienteDao.buscarPorDni()` |
| RN-052 | Las búsquedas por número de historia consideran sólo historias activas | Filtro `eliminado = FALSE` en `HistoriaClinicaDao.buscarPorNumero()` |
| RN-053 | Los listados por grupo sanguíneo consideran sólo historias activas | `WHERE hc.eliminado = FALSE` en la consulta de grupo sanguíneo |
| RN-055 | Pacientes eliminados no aparecen en listados por grupo sanguíneo | Filtro `p.eliminado = FALSE` en esa misma consulta |
| RN-056 | El listado de pacientes para auditoría es una opción separada del menú | `MenuHandler.listarPacientesAuditoria()` |
| RN-057 | En modo auditoría se muestra estado ACTIVO/ELIMINADO por paciente | Lógica de presentación en `MenuHandler.listarPacientesAuditoria()` |
| RN-058 | El acceso a listados de auditoría está limitado a perfiles autorizados | Regla de negocio documentada (no se implementa gestión de usuarios en este TP) |

### Integridad Paciente–Historia Clínica (1 a 1)

| Código | Regla | Implementación |
|--------|-------|----------------|
| RN-021 | Cada paciente puede tener como máximo una historia clínica | Relación 1:1 entre `pacientes` y `historias_clinicas` |
| RN-025 | Sólo pueden actualizarse historias clínicas activas | Validación `eliminado = FALSE` en `HistoriaClinicaServiceImpl.actualizar()` |
| RN-026 | El cambio de `nro_historia` requiere que el nuevo número sea único | Validación en service + constraint `UNIQUE(nro_historia)` |
| RN-039 | Sólo se puede asociar una historia a pacientes que no tengan una | Chequeo previo en `PacienteHistoriaClinicaService.crearHistoriaParaPacienteExistente()` |
| RN-040 | La creación/asociación de historia desde actualización es transaccional | Se usa una única `Connection` con `setAutoCommit(false)` |
| RN-041 | Se reutilizan las mismas validaciones de historia clínica en todas las rutas | `HistoriaClinicaServiceImpl.validateHistoria()` es invocado desde todos los servicios de creación/actualización |
| RN-042 | La relación 1→1 se respeta en todos los flujos (creación integrada, aislada, actualización) | Todos los servicios consultan si el paciente ya tiene historia antes de crear una nueva |
| RN-043 | La base refuerza la relación 1→1 con constraint `UNIQUE(paciente_id)` | Constraint en tabla `historias_clinicas` |
| RN-044 | Violaciones de constraints de integridad se transforman en mensajes legibles | Manejo de `SQLIntegrityConstraintViolationException` en DAO/Service |
| RN-053 | Listar por grupo sanguíneo usa siempre la relación Paciente–Historia 1:1 | `JOIN historias_clinicas` con `UNIQUE(paciente_id)` garantiza una historia por paciente |

### Transacciones y Coordinación

| Código | Regla | Implementación |
|--------|-------|----------------|
| RN-035 | Crear paciente y su historia clínica se realiza en una única transacción | `PacienteHistoriaClinicaService.crearPacienteConHistoria()` abre transacción explícita |
| RN-036 | Si falla cualquier validación/insert, toda la operación se revierte | Uso de `rollback()` en bloque `catch` dentro del servicio orquestador |
| RN-037 | El service de alto nivel orquesta ambos DAO con la misma conexión | `PacienteDao` y `HistoriaClinicaDao` reciben la misma `Connection` |
| RN-038 | Se reutilizan las validaciones de paciente/historia clínica antes de persistir | `validatePaciente()` y `validateHistoria()` se ejecutan antes de los DAO |
| RN-045 | La baja lógica de paciente e historia clínica se hace de forma transaccional | `PacienteHistoriaClinicaService.eliminarPacienteConHistoria()` coordina ambas actualizaciones |
| RN-046 | No se permite dejar al paciente eliminado con historia activa, ni al revés | Ambos `UPDATE ... SET eliminado = TRUE` forman parte de la misma transacción |
| RN-032 | La eliminación de historias clínicas es lógica (no se hace DELETE físico) | `UPDATE historias_clinicas SET eliminado = TRUE WHERE id = ?` |
| RN-033 | Después de un soft delete se verifica la cantidad de filas afectadas | Comprobación de `rowsAffected` en `HistoriaClinicaDao.eliminar()` y `PacienteDao.eliminar()` |
| RN-034 | No es posible reactivar una historia clínica desde las HU actuales | Restricción funcional documentada; no existe opción de "reactivar" en el menú |

---

## Modelo de Datos

### Diagrama Entidad-Relación

```text
┌────────────────────────────────────────────┐
│                 pacientes                  │
├────────────────────────────────────────────┤
│ id: INT PK AUTO_INCREMENT                  │
│ nombre: VARCHAR(50) NOT NULL               │
│ apellido: VARCHAR(50) NOT NULL             │
│ dni: VARCHAR(15) NOT NULL UNIQUE           │
│ fecha_nacimiento: DATE NOT NULL            │
│ eliminado: BOOLEAN DEFAULT FALSE           │
└──────────────┬─────────────────────────────┘
               │ 1
               │
               │ 1:1
               │
               ▼
┌────────────────────────────────────────────┐
│            historias_clinicas              │
├────────────────────────────────────────────┤
│ id: INT PK AUTO_INCREMENT                  │
│ nro_historia: VARCHAR(20) NOT NULL UNIQUE  │
│ paciente_id: INT NOT NULL UNIQUE           │
│ grupo_sanguineo: VARCHAR(3) NOT NULL       │
│ antecedentes: TEXT NULL                    │
│ medicacion_actual: TEXT NULL               │
│ observaciones: TEXT NULL                   │
│ eliminado: BOOLEAN DEFAULT FALSE           │
└────────────────────────────────────────────┘
```

### Constraints y Validaciones en BD

```sql
-- Constraint de unicidad en DNI del paciente
ALTER TABLE pacientes
  ADD CONSTRAINT uk_paciente_dni UNIQUE (dni);

-- Constraint de unicidad en número de historia clínica
ALTER TABLE historias_clinicas
  ADD CONSTRAINT uk_historia_nro UNIQUE (nro_historia);

-- Relación 1:1 Paciente ↔ Historia Clínica
ALTER TABLE historias_clinicas
  ADD CONSTRAINT fk_historia_paciente
    FOREIGN KEY (paciente_id) REFERENCES pacientes(id);

ALTER TABLE historias_clinicas
  ADD CONSTRAINT uk_historia_paciente UNIQUE (paciente_id);

-- Índices recomendados para performance
CREATE INDEX idx_paciente_apellido ON pacientes(apellido);
CREATE INDEX idx_paciente_eliminado ON pacientes(eliminado);
CREATE INDEX idx_historia_eliminado ON historias_clinicas(eliminado);
CREATE INDEX idx_historia_grupo ON historias_clinicas(grupo_sanguineo);
```

### Queries Principales

#### Listado de pacientes con historia clínica (LEFT JOIN)

```sql
SELECT p.id,
       p.nombre,
       p.apellido,
       p.dni,
       p.fecha_nacimiento,
       p.eliminado,
       hc.id AS hc_id,
       hc.nro_historia,
       hc.grupo_sanguineo
FROM pacientes p
LEFT JOIN historias_clinicas hc
       ON hc.paciente_id = p.id
      AND hc.eliminado = FALSE
WHERE p.eliminado = FALSE
ORDER BY p.apellido, p.nombre;
```

#### Búsqueda de pacientes por nombre/apellido

```sql
SELECT p.id,
       p.nombre,
       p.apellido,
       p.dni,
       p.fecha_nacimiento,
       p.eliminado
FROM pacientes p
WHERE p.eliminado = FALSE
  AND (LOWER(p.nombre) LIKE LOWER(?)
       OR LOWER(p.apellido) LIKE LOWER(?));
```

#### Búsqueda de paciente por DNI

```sql
SELECT p.id,
       p.nombre,
       p.apellido,
       p.dni,
       p.fecha_nacimiento,
       p.eliminado
FROM pacientes p
WHERE p.eliminado = FALSE
  AND p.dni = ?;
```

#### Búsqueda de historia clínica por número

```sql
SELECT hc.id,
       hc.nro_historia,
       hc.grupo_sanguineo,
       hc.antecedentes,
       hc.medicacion_actual,
       hc.observaciones,
       p.id   AS paciente_id,
       p.nombre,
       p.apellido,
       p.dni
FROM historias_clinicas hc
JOIN pacientes p
  ON hc.paciente_id = p.id
WHERE hc.eliminado = FALSE
  AND hc.nro_historia = ?;
```

#### Listado de pacientes por grupo sanguíneo

```sql
SELECT p.id,
       p.nombre,
       p.apellido,
       p.dni,
       hc.nro_historia,
       hc.grupo_sanguineo
FROM pacientes p
JOIN historias_clinicas hc
  ON hc.paciente_id = p.id
WHERE p.eliminado = FALSE
  AND hc.eliminado = FALSE
  AND hc.grupo_sanguineo = ?;
```

---

## Flujos Técnicos Críticos

### Flujo 1: Crear Paciente + Historia Clínica en una única transacción (HU-009)

```text
Usuario (MenuHandler)
    ↓ captura datos de paciente e historia clínica (con .trim())
PacienteHistoriaClinicaService.crearPacienteConHistoria()
    ↓ validatePaciente(paciente)
    ↓ validateDniUnique(dni, null)
    ↓ validateHistoria(historiaClinica)
    ↓ abrir conexión y setAutoCommit(false)
    ↓ PacienteDao.insertar(paciente, connection)
        ↓ INSERT pacientes ...
        ↓ obtiene ID autogenerado → paciente.setId(id)
    ↓ historiaClinica.setPacienteId(paciente.getId())
    ↓ HistoriaClinicaDao.insertar(historiaClinica, connection)
        ↓ INSERT historias_clinicas ...
    ↓ commit()
    ↓ restaurar autoCommit(true) y cerrar recursos
Usuario recibe: "Paciente y historia clínica creados exitosamente"
```

### Flujo 2: Eliminar Paciente + Historia Clínica con baja lógica coordinada (HU-012)

```text
Usuario (MenuHandler)
    ↓ ingresa pacienteId
PacienteHistoriaClinicaService.eliminarPacienteConHistoria(pacienteId)
    ↓ abrir conexión y setAutoCommit(false)
    ↓ paciente = PacienteDao.getById(pacienteId)
    ↓ if paciente == null → error "Paciente no encontrado"
    ↓ historia = HistoriaClinicaDao.getByPacienteId(pacienteId)
    ↓ PacienteDao.marcarEliminado(pacienteId, connection)
        ↓ UPDATE pacientes SET eliminado = TRUE WHERE id = ?
    ↓ if historia != null:
        ↓ HistoriaClinicaDao.marcarEliminado(historia.getId(), connection)
            ↓ UPDATE historias_clinicas SET eliminado = TRUE WHERE id = ?
    ↓ commit()
    ↓ restaurar autoCommit(true) y cerrar recursos
Usuario recibe: "Paciente e historia clínica eliminados lógicamente"
```

### Flujo 3: Asociar Historia Clínica a Paciente existente desde actualización (HU-010)

```text
Usuario (MenuHandler.actualizarPaciente())
    ↓ selecciona pacienteId
    ↓ modifica datos básicos (opcional)
    ↓ responde "S" a "¿Desea crear historia clínica?"
    ↓ completa datos de historia clínica
PacienteHistoriaClinicaService.crearHistoriaParaPacienteExistente(pacienteId, historiaClinica)
    ↓ abrir conexión y setAutoCommit(false)
    ↓ paciente = PacienteDao.getById(pacienteId)
    ↓ if paciente == null OR paciente.eliminado → error
    ↓ historiaExistente = HistoriaClinicaDao.getByPacienteId(pacienteId)
    ↓ if historiaExistente != null → error "El paciente ya posee historia clínica"
    ↓ validateHistoria(historiaClinica)
    ↓ HistoriaClinicaDao.insertar(historiaClinica con pacienteId, connection)
    ↓ commit()
    ↓ restaurar autoCommit(true) y cerrar recursos
Usuario recibe: "Historia clínica creada y asociada al paciente"
```

---

## Resumen de Operaciones del Menú

| Opción | Operación | Handler | Historias de Usuario |
|--------|-----------|---------|----------------------|
| 1 | Crear paciente | `MenuHandler.crearPaciente()` | HU-001 |
| 2 | Listar pacientes (modo normal) | `MenuHandler.listarPacientes()` | HU-002, HU-003 |
| 3 | Buscar paciente por DNI | `MenuHandler.buscarPacientePorDni()` | HU-013 |
| 4 | Actualizar datos de paciente | `MenuHandler.actualizarPaciente()` | HU-004, HU-010 |
| 5 | Eliminar paciente (baja lógica coordinada con historia) | `MenuHandler.eliminarPaciente()` / service orquestador | HU-012 |
| 6 | Crear historia clínica para paciente existente | `MenuHandler.crearHistoriaClinica()` | HU-005 |
| 7 | Listar historias clínicas | `MenuHandler.listarHistoriasClinicas()` | HU-007 |
| 8 | Actualizar historia clínica | `MenuHandler.actualizarHistoriaClinica()` | HU-006 |
| 9 | Eliminar historia clínica (baja lógica) | `MenuHandler.eliminarHistoriaClinica()` | HU-008 |
| 10 | Buscar historia clínica por número | `MenuHandler.buscarHistoriaClinicaPorNumero()` | HU-014 |
| 11 | Listar pacientes por grupo sanguíneo | `MenuHandler.listarPacientesPorGrupoSanguineo()` | HU-015 |
| 12 | Listar pacientes (modo auditor) | `MenuHandler.listarPacientesAuditoria()` | HU-016 |

---

## Documentación Relacionada

- **Trabajo Final Integrador - Programación 2 (PDF)**: Documento original de enunciado y modelo de datos base.
- **README.md**: Guía de instalación, configuración y ejecución de la consola.
- **Historias_Usuario.md**: Este documento, con épicas, historias de usuario, criterios de aceptación y reglas de negocio.
- **Modelo de Datos**: Diagrama ER generado en la herramienta de tu preferencia, alineado con la sección *Modelo de Datos*. (PENDIENTE)

---

**Versión**: 1.0  
**Total Historias de Usuario**: 16  
**Total Reglas de Negocio**: 58  
**Arquitectura**: 4 capas (App/Main → MenuHandler → Services → DAO → Models)
