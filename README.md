# Sistema de Gestión de Pacientes e Historias Clínicas 🩺

## Trabajo Práctico Integrador - Programación 2

![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk)
![Gradle](https://img.shields.io/badge/Gradle-8.x-02303A?logo=gradle)
![MySQL](https://img.shields.io/badge/MySQL-8%2B-4479A1?logo=mysql)
![Console App](https://img.shields.io/badge/UI-Consola-lightgrey?logo=gnometerminal)

---

## Integrantes 👥

> Trabajo realizado en equipo (4 integrantes). Completar con los datos reales del grupo.

|                                                            Integrante | Rol principal (sugerido) |
|----------------------------------------------------------------------:|-----------------|
|Luis Cisneros - [@luiscisneros356](https://github.com/luiscisneros356) | Diagrama UML, Modelo Paciente-HC                |
|           Nicolás Colman - [@ncolman94](https://github.com/ncolman94) | Services (Generic, Paciente, Historia Clínica)        |
|  Santiago Caiciia Massello - [@scaiciia](https://github.com/scaiciia) | DAOs, JDBC, Prepared Statement.        |
|   Guillermo Campoy - [@guillecampoy](https://github.com/guillecampoy) | Setup inicial, documentación, menú, UI        | 

---

### Descripción del Proyecto 🧾

Este Trabajo Práctico Integrador tiene como objetivo demostrar la aplicación práctica de los conceptos fundamentales de Programación Orientada a Objetos y Persistencia de Datos aprendidos durante el cursado de Programación 2.

El proyecto consiste en desarrollar un **sistema completo de gestión de pacientes e historias clínicas** que permita realizar operaciones CRUD (Crear, Leer, Actualizar, Eliminar lógico) sobre estas entidades, implementando una arquitectura robusta, profesional y alineada con un modelo de datos de **Historia Clínica ↔ Paciente (relación 1 a 1)**.

### Objetivos Académicos 🎓

El desarrollo de este sistema permite aplicar y consolidar los siguientes conceptos clave de la materia:

**1. Arquitectura en Capas (Layered Architecture)**  
- Implementación de separación de responsabilidades en 4 capas diferenciadas  
- Capa de Presentación (Main/UI): Interacción con el usuario mediante consola  
- Capa de Lógica de Negocio (Service): Validaciones y reglas de negocio  
- Capa de Acceso a Datos (DAO): Operaciones de persistencia  
- Capa de Modelo (Models): Representación de entidades del dominio (Paciente, Historia Clínica)

**2. Programación Orientada a Objetos**  
- Aplicación de principios SOLID (Single Responsibility, Dependency Injection)  
- Uso de herencia mediante clase abstracta `Base`  
- Implementación de interfaces genéricas (GenericDAO, GenericService)  
- Encapsulamiento con atributos privados y métodos de acceso  
- Sobrescritura de métodos (`equals`, `hashCode`, `toString`)

**3. Persistencia de Datos con JDBC**  
- Conexión a base de datos MySQL mediante JDBC  
- Implementación del patrón DAO (Data Access Object)  
- Uso de `PreparedStatement` para prevenir SQL Injection  
- Gestión de transacciones con `commit` y `rollback`  
- Manejo de claves autogeneradas (`AUTO_INCREMENT`)  
- Consultas con `JOIN`/`LEFT JOIN` para la relación 1 a 1 Paciente–Historia Clínica  

**4. Manejo de Recursos y Excepciones**  
- Uso del patrón `try-with-resources` para gestión automática de recursos JDBC  
- Implementación de `AutoCloseable` en `TransactionManager`  
- Manejo apropiado de excepciones con propagación controlada  
- Validación multi-nivel: base de datos y aplicación  

**5. Patrones de Diseño**  
- Factory Pattern (`DatabaseConnection`)  
- Service Layer Pattern (separación lógica de negocio)  
- DAO Pattern (abstracción del acceso a datos)  
- Soft Delete Pattern (eliminación lógica de registros)  
- Dependency Injection manual  

**6. Validación de Integridad de Datos**  
- Validación de unicidad (DNI único por paciente, número de historia único)  
- Validación de campos obligatorios en múltiples niveles  
- Validación de integridad referencial (Foreign Keys)  
- Implementación de eliminación segura y coordinada Paciente–Historia Clínica  

---

## Funcionalidades Implementadas ✅

El sistema permite gestionar dos entidades principales con las siguientes operaciones:

- **Pacientes**: Alta, baja lógica, modificación, búsqueda y listados (incluyendo modo auditoría)  
- **Historias Clínicas**: Alta, baja lógica, modificación, búsqueda y listados  
- **Operaciones integradas**:  
  - Crear paciente + historia clínica en una única transacción  
  - Asociar historia clínica a un paciente existente sin historia  
  - Eliminar paciente e historia clínica de forma consistente  

---

## Características Principales 🧬

- **Gestión de Pacientes**: Crear, listar, actualizar y eliminar pacientes con validación de DNI único  
- **Gestión de Historias Clínicas**: Crear, listar, actualizar y eliminar historias clínicas asociadas a pacientes  
- **Relación 1 a 1**: Cada paciente puede tener **como máximo una** historia clínica  
- **Búsqueda Inteligente**:  
  - Pacientes por nombre/apellido (coincidencias parciales)  
  - Pacientes por DNI  
  - Historias clínicas por número  
  - Pacientes por grupo sanguíneo (a través de su historia clínica)  
- **Modos de Listado**:  
  - Modo normal (solo activos)  
  - Modo auditor (incluye eliminados, con estado visible)  
- **Soft Delete**: Eliminación lógica que preserva la integridad de datos  
- **Seguridad**: Protección contra SQL injection mediante `PreparedStatement`  
- **Validación Multi-capa**: Validaciones en capa de servicio y base de datos  
- **Transacciones**:  
  - Crear Paciente + Historia Clínica  
  - Baja lógica coordinada Paciente + Historia Clínica  

---

## Requisitos del Sistema 💻

| Componente        | Versión Requerida         |
|-------------------|---------------------------|
| Java JDK          | 21 o superior             |
| MySQL             | 8.0 o superior            |
| Gradle            | 8.12 (incluido wrapper)   |
| Sistema Operativo | Windows, Linux o macOS    |

---

## Instalación ⚙️

### 1. Configurar Base de Datos

Ejecutar el siguiente script SQL en MySQL:

```sql
CREATE DATABASE IF NOT EXISTS db_historiaclinica;
USE db_historiaclinica;

CREATE TABLE paciente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    dni VARCHAR(15) NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    eliminado BOOLEAN DEFAULT FALSE
);

CREATE TABLE historias_clinicas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nro_historia VARCHAR(20) NOT NULL UNIQUE,
    paciente_id INT NOT NULL,
    grupo_sanguineo VARCHAR(3) NOT NULL,
    antecedentes TEXT NULL,
    medicacion_actual TEXT NULL,
    observaciones TEXT NULL,
    eliminado BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_historia_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT uk_historia_paciente UNIQUE (paciente_id)
);
```

### 2. Compilar el Proyecto

```bash
# Linux/macOS
./gradlew clean build

# Windows
gradlew.bat clean build
```

### 3. Configurar Conexión (Opcional)

Por defecto conecta a:
- **Host**: localhost:3306  
- **Base de datos**: db_historiaclinica  
- **Usuario**: root  
- **Contraseña**: (vacía)

Para cambiar la configuración, usar propiedades del sistema:

```bash
java -Ddb.url=jdbc:mysql://localhost:3306/db_historiaclinica      -Ddb.user=usuario      -Ddb.password=clave      -cp ...
```

---

## Ejecución ▶️

### Opción 1: Desde IDE

1. Abrir proyecto en IntelliJ IDEA o Eclipse  
2. Ejecutar clase `Main.Main`  

### Opción 2: Línea de comandos

**Windows:**

```bash
# Localizar JAR de MySQL
dir /s /b %USERPROFILE%\.gradle\caches\*mysql-connector-j-8.4.0.jar

# Ejecutar (reemplazar <ruta-mysql-jar>)
java -cp "build\classes\java\main;<ruta-mysql-jar>" Main.Main
```

**Linux/macOS:**

```bash
# Localizar JAR de MySQL
find ~/.gradle/caches -name "mysql-connector-j-8.4.0.jar"

# Ejecutar (reemplazar <ruta-mysql-jar>)
java -cp "build/classes/java/main:<ruta-mysql-jar>" Main.Main
```

### Verificar Conexión

```bash
# Usar TestConexion para verificar conexión a BD
java -cp "build/classes/java/main:<ruta-mysql-jar>" Main.TestConexion
```

Salida esperada:

```text
Conexion exitosa a la base de datos
Usuario conectado: root@localhost
Base de datos: db_historiaclinica
URL: jdbc:mysql://localhost:3306/db_historiaclinica
Driver: MySQL Connector/J v8.4.0
```

---

## Uso del Sistema 🖥️

### Menú Principal

```text
========= MENU =========
1. Crear paciente
2. Listar pacientes
3. Buscar paciente por DNI
4. Actualizar Paciente
5. Eliminar paciente
6. Crear historia clínica (independiente)
7. Listar historias clínicas
8. Actualizar historia clínica
9. Eliminar historia clínica
10.Buscar historia clínica por número
0. Salir
```

### Operaciones Disponibles

#### 1. Crear Paciente

- Captura nombre, apellido, DNI y fecha de nacimiento  
- Valida DNI único (no permite duplicados)  
- Valida que la fecha de nacimiento no sea futura  

**Ejemplo:**

```text
Nombre: Juan
Apellido: Pérez
DNI: 12345678
Fecha de nacimiento (YYYY-MM-DD): 1990-01-01
```

#### 2. Listar Pacientes

Dos opciones:

- **(1) Listar todos**: Muestra todos los pacientes activos (no eliminados)  
- **(2) Buscar por nombre/apellido**: Filtra por coincidencias parciales  

**Ejemplo de búsqueda:**

```text
Ingrese texto a buscar: Juan
```

**Resultado:**

```text
ID: 1, Nombre: Juan, Apellido: Pérez, DNI: 12345678
   Historia clínica: HC-0001, Grupo sanguíneo: O+
```

#### 3. Buscar Paciente por DNI

- Búsqueda exacta por DNI  
- Muestra datos del paciente y, si existe, su historia clínica asociada  

#### 4. Actualizar Paciente

- Permite modificar nombre, apellido, DNI y fecha de nacimiento  
- Presionar Enter sin escribir mantiene el valor actual  
- Opcionalmente, permite **crear y asociar una historia clínica** si el paciente no la tiene  

#### 5. Eliminar Paciente

- Eliminación lógica (marca `eliminado = TRUE`)  
- Coordinada con historia clínica: si el paciente tiene historia asociada también se marca eliminada dentro de una transacción  

#### 6. Crear Historia Clínica

- Se selecciona un paciente existente sin historia clínica  
- Captura número de historia, grupo sanguíneo, antecedentes, medicación actual y observaciones  
- Valida que el paciente no tenga ya historia y que el número de historia sea único  

#### 7. Listar Historias Clínicas

- Muestra todas las historias clínicas activas con:  
  - Número de historia  
  - Grupo sanguíneo  
  - Paciente asociado (nombre, apellido, DNI)  

#### 8. Actualizar Historia Clínica

- Permite modificar antecedentes, medicación actual, observaciones y, opcionalmente, número de historia y grupo sanguíneo  
- Enter mantiene valores actuales  

#### 9. Eliminar Historia Clínica

- Eliminación lógica (marca `eliminado = TRUE` en la historia clínica)  
- La relación con el paciente se mantiene a nivel histórico  

#### 10. Buscar Historia Clínica por Número

- Búsqueda exacta por `nro_historia`  
- Muestra datos clínicos y datos básicos del paciente asociado  

#### 11. Listar Pacientes por Grupo Sanguíneo

- Lista pacientes cuya historia clínica tenga el grupo sanguíneo seleccionado  
- Solo considera pacientes e historias activas  

#### 12. Listar Pacientes (Modo Auditor)

- Lista pacientes **activos y eliminados**  
- Muestra estado claramente:  
  - `ESTADO: ACTIVO`  
  - `ESTADO: ELIMINADO`  

---

## Arquitectura 🧱

### Estructura en Capas

```text
┌─────────────────────────────────────┐
│     Main / UI Layer                │
│  (Interacción con usuario)         │
│  AppMenu, MenuHandler, MenuDisplay │
└───────────┬────────────────────────┘
            │
┌───────────▼────────────────────────┐
│     Service Layer                  │
│  (Lógica de negocio y validación)  │
│  PacienteServiceImpl               │
│  HistoriaClinicaServiceImpl        │
│  PacienteHistoriaClinicaService    │
└───────────┬────────────────────────┘
            │
┌───────────▼────────────────────────┐
│     DAO Layer                      │
│  (Acceso a datos)                  │
│  PacienteDAO, HistoriaClinicaDAO   │
└───────────┬────────────────────────┘
            │
┌───────────▼────────────────────────┐
│     Models Layer                   │
│  (Entidades de dominio)            │
│  Paciente, HistoriaClinica, Base   │
└────────────────────────────────────┘
```

### Componentes Principales

**Config/**  
- `DatabaseConnection.java`: Gestión de conexiones JDBC con validación en inicialización estática  
- `TransactionManager.java`: Manejo de transacciones con `AutoCloseable`  

**Models/**  
- `Base.java`: Clase abstracta con campos `id` y `eliminado`  
- `Paciente.java`: Entidad Paciente (`nombre`, `apellido`, `dni`, `fechaNacimiento`, etc.)  
- `HistoriaClinica.java`: Entidad Historia Clínica (`nroHistoria`, `grupoSanguineo`, `antecedentes`, etc.)  

**Dao/**  
- `GenericDAO<T>`: Interface genérica con operaciones CRUD  
- `PacienteDAO`: Implementación con consultas y filtros por DNI/nombre/apellido  
- `HistoriaClinicaDAO`: Implementación para historias clínicas con `JOIN` a paciente  

**Service/**  
- `GenericService<T>`: Interface genérica para servicios  
- `PacienteServiceImpl`: Validaciones de paciente y coordinación básica  
- `HistoriaClinicaServiceImpl`: Validaciones específicas de historia clínica  
- `PacienteHistoriaClinicaService`: Coordinación transaccional Paciente–Historia Clínica  

**Main/**  
- `Main.java`: Punto de entrada  
- `AppMenu.java`: Orquestador del ciclo de menú  
- `MenuHandler.java`: Implementación de operaciones CRUD con captura de entrada  
- `MenuDisplay.java`: Lógica de visualización de menús  
- `TestConexion.java`: Utilidad para verificar conexión a BD  

---

## Modelo de Datos 🧬

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

**Reglas principales:**

- Un paciente puede tener **0 o 1** historia clínica.  
- `dni` es único (constraint en base de datos + validación en aplicación).  
- `nro_historia` es único en `historias_clinicas`.  
- Se utiliza eliminación lógica: campo `eliminado = TRUE`.  

---

## Patrones y Buenas Prácticas 🧠

### Seguridad

- **100% `PreparedStatement`**: Prevención de SQL injection  
- **Validación multi-capa**: Service layer valida antes de persistir  
- **DNI único**: Constraint en BD + validación en `PacienteServiceImpl.validateDniUnique()`  
- **Número de historia único**: Constraint en BD + validación en `HistoriaClinicaServiceImpl`  

### Gestión de Recursos

- **try-with-resources**: Todas las conexiones, statements y resultsets  
- **`AutoCloseable`**: `TransactionManager` cierra y hace rollback automático  
- **Scanner cerrado**: En `AppMenu.run()` al finalizar  

### Validaciones

- **Input trimming**: Todos los inputs usan `.trim()` inmediatamente  
- **Campos obligatorios**: Validación de `null` y `empty` en service layer  
- **IDs positivos**: Validación `id > 0` en todas las operaciones  
- **Verificación de `rowsAffected`**: En `UPDATE` y `DELETE`  

### Soft Delete

- DELETE ejecuta: `UPDATE tabla SET eliminado = TRUE WHERE id = ?`  
- SELECT filtra: `WHERE eliminado = FALSE`  
- No hay eliminación física de datos  

---

## Reglas de Negocio Principales (resumen) 📋

1. **DNI único**: No se permiten pacientes con DNI duplicado.  
2. **Número de historia único**: `nro_historia` no se puede repetir.  
3. **Relación 1 a 1**: Un paciente tiene como máximo una historia clínica.  
4. **Campos obligatorios**: Nombre, apellido, DNI y fecha de nacimiento en paciente; número de historia, grupo sanguíneo en historia clínica.  
5. **Fechas válidas**: La fecha de nacimiento no puede ser futura.  
6. **Preservación de valores**: En actualización, Enter mantiene el valor original.  
7. **Listados seguros**: Listados normales muestran sólo registros activos; modo auditor muestra todos.  
8. **Transacciones**: Crear Paciente + Historia Clínica y eliminar ambos se realiza en una sola transacción.  

---

## Solución de Problemas 🧯

### Error: `ClassNotFoundException: com.mysql.cj.jdbc.Driver`

**Causa**: JAR de MySQL no está en classpath.  
**Solución**: Incluir `mysql-connector-j-8.4.0.jar` en el comando `java -cp`.  

### Error: `Communications link failure`

**Causa**: MySQL no está ejecutándose.  
**Solución**:

```bash
# Linux/macOS
sudo systemctl start mysql
# O
brew services start mysql

# Windows
net start MySQL80
```

### Error: `Access denied for user 'root'@'localhost'`

**Causa**: Credenciales incorrectas.  
**Solución**: Verificar usuario/contraseña en `DatabaseConnection.java` o usar `-Ddb.user` y `-Ddb.password`.  

### Error: `Unknown database 'db_historiaclinica'`

**Causa**: Base de datos no creada.  
**Solución**: Ejecutar script de creación de base de datos (ver sección Instalación).  

### Error: `Table 'pacientes' doesn't exist`

**Causa**: Tablas no creadas.  
**Solución**: Ejecutar script de creación de tablas (ver sección Instalación).  

---

## Documentación Adicional 📚

- **Historias_Usuario.md**: Especificaciones funcionales completas  
  - 16 historias de usuario  
  - Reglas de negocio y flujos técnicos críticos  

---

## Tecnologías Utilizadas 🛠️

- **Lenguaje**: Java 21  
- **Build Tool**: Gradle 8.12  
- **Base de Datos**: MySQL 8.x  
- **JDBC Driver**: `mysql-connector-j 8.4.0`  

---

## Estructura de Directorios 📂

```text
TPI-Prog2-HistoriaClinica/
├── src/main/java/
│   ├── Config/               # Configuración de BD y transacciones
│   ├── Dao/                  # Capa de acceso a datos
│   ├── Main/                 # UI y punto de entrada
│   ├── Models/               # Entidades de dominio (Paciente, HistoriaClinica)
│   └── Service/              # Lógica de negocio
├── build.gradle              # Configuración de Gradle
├── gradlew                   # Gradle wrapper (Unix)
├── gradlew.bat               # Gradle wrapper (Windows)
├── README.md                 # Este archivo
└── Historias_Usuario.md      # Especificaciones funcionales
```

---

## Convenciones de Código ✏️

- **Idioma**: Español (nombres de clases, métodos, variables)  
- **Nomenclatura**:  
  - Clases: PascalCase (Ej: `PacienteServiceImpl`)  
  - Métodos: camelCase (Ej: `buscarPorDni`)  
  - Constantes SQL: UPPER_SNAKE_CASE (Ej: `SELECT_BY_ID_SQL`)  
- **Indentación**: 4 espacios  
- **Recursos**: Siempre usar `try-with-resources`  
- **SQL**: Constantes privadas `static final`  
- **Excepciones**: Capturar y manejar con mensajes al usuario  

---

## Contexto Académico 🎓

**Materia**: Programación 2  
**Tipo de Evaluación**: Trabajo Práctico Integrador (TPI)  
**Modalidad**: Desarrollo de sistema CRUD con persistencia en base de datos  
**Dominio**: Gestión de Pacientes e Historias Clínicas (1 a 1)  

Este proyecto representa la integración de todos los conceptos vistos durante el cuatrimestre, demostrando capacidad para:

- Diseñar sistemas con arquitectura profesional  
- Implementar persistencia de datos con JDBC  
- Aplicar patrones de diseño apropiados  
- Manejar recursos y excepciones correctamente  
- Validar integridad de datos en múltiples niveles  
- Documentar código de forma profesional  

---

**Versión**: 1.0  
**Java**: 21  
**MySQL**: 8.x  
**Gradle**: 8.12  
**Proyecto Educativo** – Trabajo Práctico Integrador de Programación 2 🧪
