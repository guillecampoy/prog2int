package prog2int.Dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import prog2int.Config.DatabaseConnection;
import prog2int.Models.HistoriaClinica;
import prog2int.Models.Paciente;
import prog2int.Models.HistoriaClinica.GrupoSanguineo;

/**
 * Data Access Object (DAO) para la entidad {@link Paciente}.
 * <p>
 * Esta clase implementa las operaciones CRUD (Crear, Leer, Actualizar, Eliminar) 
 * para la gestión de pacientes en la base de datos, incluyendo operaciones
 * transaccionales y consultas específicas.
 * <p>
 * Características principales:
 * <ul>
 *   <li>Implementa {@link GenericDAO} para operaciones estándar de persistencia</li>
 *   <li>Utiliza eliminación lógica mediante el campo "eliminado"</li>
 *   <li>Provee métodos transaccionales (sufijo "Tx") para operaciones en transacciones existentes</li>
 *   <li>Maneja la relación con historias clínicas mediante LEFT JOIN</li>
 *   <li>Incluye búsquedas específicas por nombre, apellido y DNI</li>
 *   <li>Provee métodos de auditoría que incluyen registros eliminados</li>
 * </ul>
 * <p>
 * Operaciones principales:
 * <ul>
 *   <li>Inserción y actualización de pacientes</li>
 *   <li>Búsqueda por ID, por nombre/apellido y por DNI</li>
 *   <li>Obtención de todos los registros activos</li>
 *   <li>Eliminación lógica de registros</li>
 *   <li>Consultas de auditoría que incluyen registros eliminados</li>
 * </ul>
 * <p>
 * Mapeo a base de datos:
 * <ul>
 *   <li>Tabla: <b>paciente</b></li>
 *   <li>Campos: id, nombre, apellido, dni, fecha_nacimiento, eliminado</li>
 * </ul>
 * <p>
 * Dependencias:
 * <ul>
 *   <li>{@link DatabaseConnection} - Gestión de conexiones a la base de datos</li>
 *   <li>{@link Paciente} - Entidad del modelo de datos</li>
 *   <li>{@link HistoriaClinicaDAO} - DAO para gestionar historias clínicas</li>
 * </ul>
 */
public class PacienteDAO implements GenericDAO<Paciente> {

    // ======================================
    // CONSTANTES SQL - OPERACIONES ESTÁNDAR
    // ======================================
    
    /**
     * Sentencia SQL para insertar un nuevo paciente en la base de datos
     * Inserta nombre, apellido, DNI y fecha de nacimiento
     * El ID es AUTO_INCREMENT y se obtiene con getGeneratedKeys()
     */
    private static final String INSERT_SQL = "INSERT INTO paciente (nombre, apellido, dni, fecha_nacimiento) VALUES (?, ?, ?, ?)";

    /**
     * Sentencia SQL para actualizar los datos de un paciente existente
     * Actualiza nombre, apellido, DNI y fecha de nacimiento
     * NO actualiza el flag eliminado (solo se modifica en soft delete)
     */
    private static final String UPDATE_SQL = "UPDATE paciente SET nombre = ?, apellido = ?, dni = ?, fecha_nacimiento = ? WHERE id = ?";

    /**
     * Sentencia SQL para eliminar lógicamente un paciente (soft delete)
     * Marca eliminado=TRUE sin borrar físicamente la fila
     * Preserva integridad referencial y datos históricos
     */
    private static final String DELETE_SQL = "UPDATE paciente SET eliminado = TRUE WHERE id = ?";

    // ===================================================
    // CONSTANTES SQL - CONSULTAS ESTÁNDAR (solo activos)
    // ===================================================
    
    /**
     * Sentencia SQL para obtener un paciente por su ID incluyendo su historia clínica
     * Solo retorna pacientes activos (eliminado=FALSE)
     * Realiza LEFT JOIN con la tabla historias_clinicas
     */
    private static final String SELECT_BY_ID_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " +
    "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
    "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
    "WHERE p.id = ? AND eliminado = FALSE";

    /**
     * Sentencia SQL para obtener todos los pacientes activos con sus historias clínicas
     * Filtra por eliminado=FALSE (solo pacientes activos)
     * Realiza LEFT JOIN con la tabla historias_clinicas
     */
    private static final String SELECT_ALL_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
        "WHERE eliminado = FALSE";

    /**
     * Sentencia SQL para buscar pacientes por nombre o apellido (búsqueda parcial)
     * Solo retorna pacientes activos (eliminado=FALSE)
     * Usa LIKE para búsqueda parcial en nombre o apellido
     */
    private static final String SEARCH_BY_NAME_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
        "WHERE eliminado = FALSE AND (p.nombre LIKE ? OR p.apellido LIKE ?)";

    /**
     * Sentencia SQL para buscar un paciente por su DNI exacto
     * Solo retorna pacientes activos (eliminado=FALSE)
     * Usa comparación exacta del campo DNI
     */
    private static final String SEARCH_BY_DNI_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
        "WHERE p.dni = ? AND eliminado = FALSE";

    // =============================================================
    // CONSTANTES SQL - CONSULTAS DE AUDITORÍA (incluye eliminados)
    // =============================================================
    
    /**
     * Sentencia SQL para obtener un paciente por su ID incluyendo registros eliminados
     * Usado para propósitos de auditoría y reportes
     * Realiza LEFT JOIN con la tabla historias_clinicas
     */
    private static final String SELECT_BY_ID_AUDIT_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " +
    "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
    "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
    "WHERE p.id = ?";

    /**
     * Sentencia SQL para obtener todos los pacientes incluyendo registros eliminados
     * Usado para propósitos de auditoría y reportes
     * Realiza LEFT JOIN con la tabla historias_clinicas
     */
    private static final String SELECT_ALL_AUDIT_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id";

    /**
     * Sentencia SQL para buscar pacientes por nombre o apellido incluyendo registros eliminados
     * Usado para propósitos de auditoría y reportes
     * Usa LIKE para búsqueda parcial en nombre o apellido
     */
    private static final String SEARCH_BY_NAME_AUDIT_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
        "WHERE (p.nombre LIKE ? OR p.apellido LIKE ?)";

    /**
     * Sentencia SQL para buscar un paciente por su DNI incluyendo registros eliminados
     * Usado para propósitos de auditoría y reportes
     * Usa comparación exacta del campo DNI
     */
    private static final String SEARCH_BY_DNI_AUDIT_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
        "WHERE p.dni = ?";
    
    /**
     * DAO para la gestión de historias clínicas
     */
    private final HistoriaClinicaDAO historiaClinicaDAO;
    
    // ============
    // CONSTRUCTOR
    // ============
    
    /**
     * Constructor que inicializa el DAO con una instancia de HistoriaClinicaDAO
     * 
     * @param historiaClinicaDAO Instancia de HistoriaClinicaDAO para gestionar historias clínicas
     * @throws IllegalArgumentException Si historiaClinicaDAO es null
     */
    public PacienteDAO(HistoriaClinicaDAO historiaClinicaDAO) {
        if (historiaClinicaDAO == null) {
            throw new IllegalArgumentException("HistoriaClinicaDao no puede ser null.");
        }

        this.historiaClinicaDAO = historiaClinicaDAO;
    }
    
    // =========================
    // MÉTODOS CRUD PRINCIPALES
    // =========================
    
    /**
     * Inserta un nuevo paciente en la base de datos
     * 
     * @param paciente El paciente a insertar
     * @throws Exception Si ocurre un error durante la inserción
     */
    @Override
    public void insertar(Paciente paciente) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            
            setPacienteParameters(stmt, paciente);
            stmt.executeUpdate();
            setGeneratedId(stmt, paciente);
        }
    }

    /**
     * Inserta un nuevo paciente dentro de una transacción existente
     * 
     * @param paciente El paciente a insertar
     * @param conn Conexión a la base de datos existente
     * @throws Exception Si ocurre un error durante la inserción
     */
    @Override
    public void insertTx(Paciente paciente, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            
            setPacienteParameters(stmt, paciente);
            stmt.executeUpdate();
            setGeneratedId(stmt, paciente);
        }
    }

    /**
     * Actualiza los datos de un paciente existente en la base de datos
     * 
     * @param paciente El paciente con los datos actualizados
     * @throws Exception Si ocurre un error durante la actualización o si no se encuentra el paciente
     */
    @Override
    public void actualizar(Paciente paciente) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, paciente.getNombre());
            stmt.setString(2, paciente.getApellido());
            stmt.setString(3, paciente.getDni());
            stmt.setDate(4, Date.valueOf(paciente.getFechaNacimiento()));
            stmt.setInt(5, paciente.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el paciente con ID: " + paciente.getId());
            }
        }
    }

    /**
     * Actualiza los datos de un paciente dentro de una transacción existente
     * 
     * @param paciente El paciente con los datos actualizados
     * @param conn Conexión a la base de datos existente
     * @throws Exception Si ocurre un error durante la actualización o si no se encuentra el paciente
     */
    public void updateTx(Paciente paciente, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, paciente.getNombre());
            stmt.setString(2, paciente.getApellido());
            stmt.setString(3, paciente.getDni());
            stmt.setDate(4, Date.valueOf(paciente.getFechaNacimiento()));
            stmt.setInt(5, paciente.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el paciente con ID: " + paciente.getId());
            }
        }
    }

    /**
     * Elimina lógicamente un paciente de la base de datos (marca como eliminado)
     * 
     * @param id ID del paciente a eliminar
     * @throws Exception Si ocurre un error durante la eliminación o si no se encuentra el paciente
     */
    @Override
    public void eliminar(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el paciente con ID: " + id);
            }
        }     
    }

    /**
     * Elimina lógicamente un paciente dentro de una transacción existente
     * 
     * @param id ID del paciente a eliminar
     * @param conn Conexión a la base de datos existente
     * @throws Exception Si ocurre un error durante la eliminación o si no se encuentra el paciente
     */
    public void deleteTx(int id, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el paciente con ID: " + id);
            }
        }
    }
    
    // ======================================================
    // MÉTODOS DE CONSULTA ESTÁNDAR (solo registros activos)
    // ======================================================
    
    /**
     * Obtiene un paciente por su ID incluyendo su historia clínica
     * Solo retorna pacientes activos (eliminado=FALSE)
     * 
     * @param id ID del paciente a buscar
     * @return El paciente encontrado o null si no existe
     * @throws Exception Si ocurre un error durante la consulta
     */
    @Override
    public Paciente getById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaciente(rs);
                }
                return null;
            }
        }
    }

    /**
     * Obtiene un paciente por su ID dentro de una transacción existente
     * Solo retorna pacientes activos (eliminado=FALSE)
     * 
     * @param id ID del paciente a buscar
     * @param conn Conexión a la base de datos existente
     * @return El paciente encontrado o null si no existe
     * @throws Exception Si ocurre un error durante la consulta
     */
    public Paciente getByIdTx(int id, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaciente(rs);
                }
                return null;
            }
        }
    }

    /**
     * Obtiene todos los pacientes activos de la base de datos con sus historias clínicas
     * Solo retorna pacientes activos (eliminado=FALSE)
     * 
     * @return Lista de todos los pacientes activos
     * @throws Exception Si ocurre un error durante la consulta
     */
    @Override
    public List<Paciente> getAll() throws Exception {
        List<Paciente> pacientes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                pacientes.add(mapResultSetToPaciente(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todas los pacientes: " + e.getMessage(), e);
        }
        return pacientes;
    }

    /**
     * Obtiene todos los pacientes activos dentro de una transacción existente
     * Solo retorna pacientes activos (eliminado=FALSE)
     * 
     * @param conn Conexión a la base de datos existente
     * @return Lista de todos los pacientes activos
     * @throws Exception Si ocurre un error durante la consulta
     */
    public List<Paciente> getAllTx(Connection conn) throws Exception {
        List<Paciente> pacientes = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                pacientes.add(mapResultSetToPaciente(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todas los pacientes: " + e.getMessage(), e);
        }
        return pacientes;
    }

    /**
     * Busca pacientes por nombre o apellido (búsqueda parcial)
     * Solo retorna pacientes activos (eliminado=FALSE)
     * 
     * @param filtro Texto a buscar en nombre o apellido
     * @return Lista de pacientes que coinciden con el filtro
     * @throws Exception Si ocurre un error durante la búsqueda
     */
    public List<Paciente> buscarPorNombreApellido(String filtro) throws Exception {
        List<Paciente> pacientes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_NAME_SQL)) {

            String searchPattern = "%" + filtro + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pacientes.add(mapResultSetToPaciente(rs));
                }
            }
        }

        return pacientes;
    }

    /**
     * Busca pacientes por nombre o apellido dentro de una transacción existente
     * Solo retorna pacientes activos (eliminado=FALSE)
     * 
     * @param filtro Texto a buscar en nombre o apellido
     * @param conn Conexión a la base de datos existente
     * @return Lista de pacientes que coinciden con el filtro
     * @throws Exception Si ocurre un error durante la búsqueda
     */
    public List<Paciente> searchForNombreApellidoTx(String filtro, Connection conn) throws Exception {
        List<Paciente> pacientes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_NAME_SQL)) {

            String searchPattern = "%" + filtro + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pacientes.add(mapResultSetToPaciente(rs));
                }
            }
        }

        return pacientes;
    }

    /**
     * Busca un paciente por su DNI exacto
     * Solo retorna pacientes activos (eliminado=FALSE)
     * 
     * @param dni DNI del paciente a buscar
     * @return El paciente encontrado o null si no existe
     * @throws SQLException Si ocurre un error durante la búsqueda
     * @throws IllegalArgumentException Si el DNI está vacío o es null
     */
    public Paciente buscarPorDni(String dni) throws SQLException {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_DNI_SQL)) {

            stmt.setString(1, dni);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaciente(rs);
                }
                return null;
            }
        }
    }

    /**
     * Busca un paciente por su DNI exacto dentro de una transacción existente
     * Solo retorna pacientes activos (eliminado=FALSE)
     * 
     * @param dni DNI del paciente a buscar
     * @param conn Conexión a la base de datos existente
     * @return El paciente encontrado o null si no existe
     * @throws SQLException Si ocurre un error durante la búsqueda
     * @throws IllegalArgumentException Si el DNI está vacío o es null
     */
    public Paciente searchPorDniTx(String dni, Connection conn) throws SQLException {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }

        try (PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_DNI_SQL)) {

            stmt.setString(1, dni);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaciente(rs);
                }
                return null;
            }
        }
    }

    // =====================================================
    // MÉTODOS DE AUDITORÍA (incluyen registros eliminados)
    // =====================================================
    
    /**
     * Obtiene un paciente por su ID incluyendo registros eliminados
     * Usado para propósitos de auditoría, reportes y recuperación de datos
     * 
     * @param id ID del paciente a buscar
     * @return El paciente encontrado (puede estar eliminado) o null si no existe
     * @throws Exception Si ocurre un error durante la consulta
     */
    public Paciente getByIdAudit(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_AUDIT_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaciente(rs);
                }
                return null;
            }
        }
    }

    /**
     * Obtiene un paciente por su ID incluyendo registros eliminados dentro de una transacción existente
     * Usado para propósitos de auditoría, reportes y recuperación de datos
     * 
     * @param id ID del paciente a buscar
     * @param conn Conexión a la base de datos existente
     * @return El paciente encontrado (puede estar eliminado) o null si no existe
     * @throws Exception Si ocurre un error durante la consulta
     */
    public Paciente getByIdAuditTx(int id, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_AUDIT_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaciente(rs);
                }
                return null;
            }
        }
    }

    /**
     * Obtiene todos los pacientes incluyendo registros eliminados
     * Usado para propósitos de auditoría, reportes y análisis históricos
     * 
     * @return Lista de todos los pacientes (activos y eliminados)
     * @throws Exception Si ocurre un error durante la consulta
     */
    public List<Paciente> getAllAudit() throws Exception {
        List<Paciente> pacientes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_AUDIT_SQL)) {

            while (rs.next()) {
                pacientes.add(mapResultSetToPaciente(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todas los pacientes: " + e.getMessage(), e);
        }
        return pacientes;
    }

    /**
     * Obtiene todos los pacientes incluyendo registros eliminados dentro de una transacción existente
     * Usado para propósitos de auditoría, reportes y análisis históricos
     * 
     * @param conn Conexión a la base de datos existente
     * @return Lista de todos los pacientes (activos y eliminados)
     * @throws Exception Si ocurre un error durante la consulta
     */
    public List<Paciente> getAllAuditTx(Connection conn) throws Exception {
        List<Paciente> pacientes = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_AUDIT_SQL)) {

            while (rs.next()) {
                pacientes.add(mapResultSetToPaciente(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todas los pacientes: " + e.getMessage(), e);
        }
        return pacientes;
    }

    /**
     * Busca pacientes por nombre o apellido incluyendo registros eliminados
     * Usado para propósitos de auditoría, reportes y análisis históricos
     * 
     * @param filtro Texto a buscar en nombre o apellido
     * @return Lista de pacientes que coinciden con el filtro (activos y eliminados)
     * @throws Exception Si ocurre un error durante la búsqueda
     */
    public List<Paciente> buscarPorNombreApellidoAudit(String filtro) throws Exception {
        List<Paciente> pacientes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_NAME_AUDIT_SQL)) {

            String searchPattern = "%" + filtro + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pacientes.add(mapResultSetToPaciente(rs));
                }
            }
        }

        return pacientes;
    }

    /**
     * Busca pacientes por nombre o apellido incluyendo registros eliminados dentro de una transacción existente
     * Usado para propósitos de auditoría, reportes y análisis históricos
     * 
     * @param filtro Texto a buscar en nombre o apellido
     * @param conn Conexión a la base de datos existente
     * @return Lista de pacientes que coinciden con el filtro (activos y eliminados)
     * @throws Exception Si ocurre un error durante la búsqueda
     */
    public List<Paciente> searchForNombreApellidoAuditTx(String filtro, Connection conn) throws Exception {
        List<Paciente> pacientes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_NAME_AUDIT_SQL)) {

            String searchPattern = "%" + filtro + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pacientes.add(mapResultSetToPaciente(rs));
                }
            }
        }

        return pacientes;
    }

    /**
     * Busca un paciente por su DNI exacto incluyendo registros eliminados
     * Usado para propósitos de auditoría, reportes y recuperación de datos
     * 
     * @param dni DNI del paciente a buscar
     * @return El paciente encontrado (puede estar eliminado) o null si no existe
     * @throws SQLException Si ocurre un error durante la búsqueda
     * @throws IllegalArgumentException Si el DNI está vacío o es null
     */
    public Paciente buscarPorDniAudit(String dni) throws SQLException {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_DNI_AUDIT_SQL)) {

            stmt.setString(1, dni);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaciente(rs);
                }
                return null;
            }
        }
    }

    /**
     * Busca un paciente por su DNI exacto incluyendo registros eliminados dentro de una transacción existente
     * Usado para propósitos de auditoría, reportes y recuperación de datos
     * 
     * @param dni DNI del paciente a buscar
     * @param conn Conexión a la base de datos existente
     * @return El paciente encontrado (puede estar eliminado) o null si no existe
     * @throws SQLException Si ocurre un error durante la búsqueda
     * @throws IllegalArgumentException Si el DNI está vacío o es null
     */
    public Paciente searchPorDniAuditTx(String dni, Connection conn) throws SQLException {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }

        try (PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_DNI_AUDIT_SQL)) {

            stmt.setString(1, dni);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaciente(rs);
                }
                return null;
            }
        }
    }
    
    // ============================
    // MÉTODOS PRIVADOS AUXILIARES
    // ============================
    
    /**
     * Establece los parámetros del paciente en un PreparedStatement para inserción
     * 
     * @param stmt PreparedStatement al que se le asignarán los parámetros
     * @param paciente Paciente del que se obtienen los datos
     * @throws SQLException Si ocurre un error al establecer los parámetros
     */
    private void setPacienteParameters(PreparedStatement stmt, Paciente paciente) throws SQLException {
        stmt.setString(1, paciente.getNombre());
        stmt.setString(2, paciente.getApellido());
        stmt.setString(3, paciente.getDni());
        stmt.setDate(4, Date.valueOf(paciente.getFechaNacimiento()));
    }

    /**
     * Obtiene el ID generado automáticamente después de una inserción y lo asigna al paciente
     * 
     * @param stmt PreparedStatement utilizado para la inserción
     * @param paciente Paciente al que se le asignará el ID generado
     * @throws SQLException Si ocurre un error al obtener el ID generado
     */
    private void setGeneratedId(PreparedStatement stmt, Paciente paciente) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                paciente.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción del paciente falló, no se obtuvo ID generado");
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto Paciente, incluyendo su historia clínica si existe
     * <p>
     * Mapeo de columnas:
     * <ul>
     *   <li>id, nombre, apellido, dni, fecha_nacimiento, eliminado → Paciente</li>
     *   <li>hc.id, hc.nro_historia, grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, observaciones, eliminado → HistoriaClinica</li>
     * </ul>
     * 
     * @param rs ResultSet con los datos del paciente
     * @return Objeto Paciente con los datos mapeados
     * @throws SQLException Si ocurre un error durante el mapeo
     */
    private Paciente mapResultSetToPaciente(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente();
        paciente.setId(rs.getInt("id"));
        paciente.setNombre(rs.getString("nombre"));
        paciente.setApellido(rs.getString("apellido"));
        paciente.setDni(rs.getString("dni"));
        paciente.setFechaNacimiento(rs.getDate("fecha_nacimiento").toLocalDate());
        paciente.setEliminado(rs.getBoolean("eliminado"));

        if (rs.getInt("hs.id") > 0 && !rs.wasNull()) {
            HistoriaClinica historiaClinica = new HistoriaClinica();
            historiaClinica.setId(rs.getInt("hc.id"));
            historiaClinica.setNroHistoria(rs.getString("hc.nro_historia"));
            historiaClinica.setGrupoSanguineo(GrupoSanguineo.valueOf(rs.getString("grupo_sanguineo")));
            historiaClinica.setAntecedentes(rs.getString("hc.antecedentes"));
            historiaClinica.setMedicacionActual(rs.getString("hc.medicacion_actual"));
            historiaClinica.setObservaciones(rs.getString("observaciones"));
            historiaClinica.setEliminado(rs.getBoolean("eliminado"));
            paciente.setHistoriaClinica(historiaClinica);
        }

        return paciente;
    }

}