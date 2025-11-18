package prog2int.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import prog2int.Config.DatabaseConnection;
import prog2int.Models.HistoriaClinica;
import prog2int.Models.HistoriaClinica.GrupoSanguineo;

/**
 * Data Access Object (DAO) para la entidad {@link HistoriaClinica}.
 * <p>
 * Esta clase implementa las operaciones CRUD (Crear, Leer, Actualizar, Eliminar) 
 * para la gestión de historias clínicas en la base de datos, incluyendo operaciones
 * transaccionales y consultas específicas.
 * <p>
 * Características principales:
 * <ul>
 *   <li>Implementa {@link GenericDAO} para operaciones estándar de persistencia</li>
 *   <li>Utiliza eliminación lógica mediante el campo "eliminado"</li>
 *   <li>Provee métodos transaccionales (sufijo "Tx") para operaciones en transacciones existentes</li>
 *   <li>Maneja la relación con pacientes mediante el campo "paciente_id"</li>
 *   <li>Incluye métodos de auditoría que retornan tanto registros activos como eliminados</li>
 * </ul>
 * <p>
 * Operaciones principales:
 * <ul>
 *   <li>Inserción y actualización de historias clínicas</li>
 *   <li>Asignación de historias clínicas a pacientes</li>
 *   <li>Búsqueda por ID, por paciente y obtención de todos los registros</li>
 *   <li>Eliminación lógica de registros</li>
 *   <li>Consultas de auditoría que incluyen registros eliminados</li>
 * </ul>
 * <p>
 * Mapeo a base de datos:
 * <ul>
 *   <li>Tabla: <b>historias_clinicas</b></li>
 *   <li>Campos: id, nro_historia, paciente_id, grupo_sanguineo, antecedentes, medicacion_actual, observaciones, eliminado</li>
 * </ul>
 * <p>
 * Dependencias:
 * <ul>
 *   <li>{@link DatabaseConnection} - Gestión de conexiones a la base de datos</li>
 *   <li>{@link HistoriaClinica} - Entidad del modelo de datos</li>
 * </ul>
 */
public class HistoriaClinicaDAO implements GenericDAO<HistoriaClinica> {
    
    // ======================================
    // CONSTANTES SQL - OPERACIONES ESTÁNDAR
    // ======================================
    
    /**
     * Sentencia SQL para insertar una nueva historia clínica en la base de datos
     * Inserta número de historia, grupo sanguíneo, antecedentes, medicación actual y observaciones
     * El ID es AUTO_INCREMENT y se obtiene con RETURN_GENERATED_KEYS
     * El campo eliminado tiene DEFAULT FALSE en la BD
     */
    private static final String INSERT_SQL = "INSERT INTO historias_clinicas (nro_historia, grupo_sanguineo, antecedentes, medicacion_actual, observaciones) VALUES (?, ?, ?, ?, ?)";
    
    private static final String INSERT_WITH_PACIENTE_SQL = "INSERT INTO historias_clinicas (nro_historia, paciente_id, grupo_sanguineo, antecedentes, medicacion_actual, observaciones) VALUES (?, ?, ?, ?, ?, ?)";

    /**
     * Sentencia SQL para actualizar los datos de una historia clínica existente
     * Actualiza número de historia, grupo sanguíneo, antecedentes, medicación actual y observaciones
     * NO actualiza el flag eliminado (solo se modifica en soft delete) ni el ID del paciente
     */
    private static final String UPDATE_SQL = "UPDATE historias_clinicas SET nro_historia = ?, grupo_sanguineo = ?, antecedentes = ?, medicacion_actual = ?, observaciones = ? WHERE id = ?";

    /**
     * Sentencia SQL para actualizar el ID del paciente asociado a una historia clínica
     * NO actualiza ningún otro campo de la tabla
     */
    private static final String UPDATE_ID_PACIENTE_SQL = "UPDATE historias_clinicas SET paciente_id = ? WHERE id = ?";

    /**
     * Sentencia SQL para eliminación lógica (soft delete)
     * Marca eliminado=TRUE sin borrar físicamente la fila
     * Preserva integridad referencial y datos históricos
     */
    private static final String DELETE_SQL = "UPDATE historias_clinicas SET eliminado = TRUE WHERE id = ?";

    // ===================================================
    // CONSTANTES SQL - CONSULTAS ESTÁNDAR (solo activos)
    // ===================================================
    
    /**
     * Sentencia SQL para obtener una historia clínica por su ID
     * Solo retorna historias clínicas activas (eliminado=FALSE)
     */
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM historias_clinicas WHERE id = ? AND eliminado = FALSE";

    /**
     * Sentencia SQL para obtener todas las historias clínicas activas
     * Filtra por eliminado=FALSE (solo historias clínicas activas)
     */
    private static final String SELECT_ALL_SQL = "SELECT * FROM historias_clinicas WHERE eliminado = FALSE";

    /**
     * Sentencia SQL para obtener historias clínicas por ID de paciente
     * Solo retorna historias clínicas activas (eliminado=FALSE)
     */
    private static final String SELECT_BY_PACIENTE_ID_SQL = "SELECT * FROM historias_clinicas WHERE paciente_id = ? AND eliminado = FALSE";

    // =============================================================
    // CONSTANTES SQL - CONSULTAS DE AUDITORÍA (incluye eliminados)
    // =============================================================
    
    /**
     * Sentencia SQL para obtener una historia clínica por su ID incluyendo registros eliminados
     * Usado para propósitos de auditoría y reportes
     */
    private static final String SELECT_BY_ID_AUDIT_SQL = "SELECT * FROM historias_clinicas WHERE id = ?";

    /**
     * Sentencia SQL para obtener todas las historias clínicas incluyendo registros eliminados
     * Usado para propósitos de auditoría y reportes
     */
    private static final String SELECT_ALL_AUDIT_SQL = "SELECT * FROM historias_clinicas";

    /**
     * Sentencia SQL para obtener historias clínicas por ID de paciente incluyendo registros eliminados
     * Usado para propósitos de auditoría y reportes
     */
    private static final String SELECT_BY_PACIENTE_ID_AUDIT_SQL = "SELECT * FROM historias_clinicas WHERE paciente_id = ?";

    // =========================
    // MÉTODOS CRUD PRINCIPALES
    // =========================
    
    /**
     * Inserta una nueva historia clínica en la base de datos (versión sin transacción)
     * Crea su propia conexión y la cierra automáticamente
     * <p>
     * Flujo:
     * <ol>
     *   <li>Abre conexión con DatabaseConnection.getConnection()</li>
     *   <li>Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS</li>
     *   <li>Setea parámetros (nro_historia, grupo_sanguineo, antecedentes, medicacion_actual, observaciones)</li>
     *   <li>Ejecuta INSERT</li>
     *   <li>Obtiene el ID autogenerado y lo asigna a historiaClinica.id</li>
     *   <li>Cierra recursos automáticamente (try-with-resources)</li>
     * </ol>
     * <p>
     * IMPORTANTE: El ID generado se asigna al objeto historiaClinica
     * Esto permite que PacienteServiceImpl.insertar() use historiaClinica.getId()
     * inmediatamente después de insertar
     *
     * @param historiaClinica HistoriaClinica a insertar (ID será ignorado y regenerado)
     * @throws Exception Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(HistoriaClinica historiaClinica) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setHistoriaClinicaParameters(stmt, historiaClinica);
            stmt.executeUpdate();
            setGeneratedId(stmt, historiaClinica);
        }
    }

    /**
     * Inserta una historia clínica dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado por:
     * <ul>
     *   <li>Operaciones que requieren múltiples inserts coordinados</li>
     *   <li>Rollback automático si alguna operación falla</li>
     * </ul>
     *
     * @param historiaClinica HistoriaClinica a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertTx(HistoriaClinica historiaClinica, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            setHistoriaClinicaParameters(stmt, historiaClinica);
            stmt.executeUpdate();
            setGeneratedId(stmt, historiaClinica);
        }
    }
    
    public void insertWithPacienteTx(HistoriaClinica historiaClinica, int pacienteId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_WITH_PACIENTE_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, historiaClinica.getNroHistoria());
            stmt.setInt(2, pacienteId);
            stmt.setString(3, historiaClinica.getGrupoSanguineo().getSimbolo());
            stmt.setString(4, historiaClinica.getAntecedentes());
            stmt.setString(5, historiaClinica.getMedicacionActual());
            stmt.setString(6, historiaClinica.getObservaciones());
            stmt.executeUpdate();
            setGeneratedId(stmt, historiaClinica);
        }
    }

    /**
     * Actualiza una historia clínica existente en la base de datos
     * Actualiza nro_historia, grupo_sanguineo, antecedentes, medicacion_actual y observaciones
     * <p>
     * Validaciones:
     * <ul>
     *   <li>Si rowsAffected == 0 → La historia clínica no existe o ya está eliminada</li>
     * </ul>
     *
     * @param historiaClinica HistoriaClinica con los datos actualizados (ID debe ser > 0)
     * @throws Exception Si la historia clínica no existe o hay error de BD
     */
    @Override
    public void actualizar(HistoriaClinica historiaClinica) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, historiaClinica.getNroHistoria());
            stmt.setString(2, historiaClinica.getGrupoSanguineo().getSimbolo());
            stmt.setString(3, historiaClinica.getAntecedentes());
            stmt.setString(4, historiaClinica.getMedicacionActual());
            stmt.setString(5, historiaClinica.getObservaciones());
            stmt.setInt(6, historiaClinica.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el historial con ID: " + historiaClinica.getId());
            }
        }
    }

    /**
     * Actualiza una historia clínica dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado por:
     * <ul>
     *   <li>Operaciones que requieren múltiples updates coordinados</li>
     *   <li>Rollback automático si alguna operación falla</li>
     * </ul>
     *
     * @param historiaClinica HistoriaClinica a actualizar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la actualización
     */
    public void actualizarTx(HistoriaClinica historiaClinica, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setString(1, historiaClinica.getNroHistoria());
            stmt.setString(2, historiaClinica.getGrupoSanguineo().getSimbolo());
            stmt.setString(3, historiaClinica.getAntecedentes());
            stmt.setString(4, historiaClinica.getMedicacionActual());
            stmt.setString(5, historiaClinica.getObservaciones());
            stmt.setInt(6, historiaClinica.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el historial con ID: " + historiaClinica.getId());
            }
        }
    }

    /**
     * Actualiza el ID del paciente en una historia clínica existente en la base de datos
     * <p>
     * Validaciones:
     * <ul>
     *   <li>Si rowsAffected == 0 → La historia clínica no existe o ya está eliminada</li>
     * </ul>
     *
     * @param pacienteId ID del paciente a asignar (debe ser > 0)
     * @param historiaId ID de la historia clínica a actualizar (debe ser > 0)
     * @throws Exception Si la historia clínica no existe o hay error de BD
     */
    public void actualizarPaciente(int pacienteId, int historiaId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_ID_PACIENTE_SQL)) {

            stmt.setInt(1, pacienteId);
            stmt.setInt(2, historiaId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el historial con ID: " + historiaId);
            }
        }
    }

    /**
     * Actualiza el ID del paciente en una historia clínica dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado por:
     * <ul>
     *   <li>Operaciones que requieren múltiples updates coordinados</li>
     *   <li>Rollback automático si alguna operación falla</li>
     * </ul>
     *
     * @param pacienteId ID del paciente a asignar (debe ser > 0)
     * @param historiaId ID de la historia clínica a actualizar (debe ser > 0)
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la actualización
     */
    public void updatePacienteTx(int pacienteId, int historiaId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_ID_PACIENTE_SQL)) {

            stmt.setInt(1, pacienteId);
            stmt.setInt(2, historiaId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el historial con ID: " + historiaId);
            }
        }
    }

    /**
     * Elimina lógicamente una historia clínica (soft delete)
     * Marca eliminado=TRUE sin borrar físicamente la fila
     * <p>
     * Validaciones:
     * <ul>
     *   <li>Si rowsAffected == 0 → La historia clínica no existe o ya está eliminada</li>
     * </ul>
     *
     * @param id ID de la historia clínica a eliminar
     * @throws Exception Si la historia clínica no existe o hay error de BD
     */
    @Override
    public void eliminar(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontro historia con ID: " + id);
            }
        }
    }

    /**
     * Elimina lógicamente una historia clínica dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado por:
     * <ul>
     *   <li>Operaciones que requieren múltiples eliminaciones coordinadas</li>
     *   <li>Rollback automático si alguna operación falla</li>
     * </ul>
     *
     * @param id ID de la historia clínica a eliminar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la eliminación
     */
    public void eliminarTx(int id, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontro historia con ID: " + id);
            }
        }
    }
    
    // ======================================================
    // MÉTODOS DE CONSULTA ESTÁNDAR (solo registros activos)
    // ======================================================
    
    /**
     * Obtiene una historia clínica por su ID
     * Solo retorna historias clínicas activas (eliminado=FALSE)
     *
     * @param id ID de la historia clínica a buscar
     * @return HistoriaClinica encontrada, o null si no existe o está eliminada
     * @throws Exception Si hay error de BD
     */
    @Override
    public HistoriaClinica getById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistoriaClinica(rs);
                }
                return null;
            }
        }
    }

    /**
     * Obtiene una historia clínica por su ID dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado por:
     * <ul>
     *   <li>Operaciones que requieren múltiples consultas coordinadas</li>
     *   <li>Rollback automático si alguna operación falla</li>
     * </ul>
     *
     * @param id ID de la historia clínica a buscar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @return HistoriaClinica encontrada, o null si no existe o está eliminada
     * @throws Exception Si hay error de BD
     */
    public HistoriaClinica getByIdTx(int id, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistoriaClinica(rs);
                }
                return null;
            }
        }
    }

    /**
     * Obtiene todas las historias clínicas activas (eliminado=FALSE)
     * <p>
     * Nota: Usa Statement (no PreparedStatement) porque no hay parámetros
     *
     * @return Lista de historias clínicas activas (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<HistoriaClinica> getAll() throws Exception {
        List<HistoriaClinica> historialClinico = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {
            
            while (rs.next()) {
                historialClinico.add(mapResultSetToHistoriaClinica(rs));
            }
        }

        return historialClinico;
    }

    /**
     * Obtiene todas las historias clínicas activas (eliminado=FALSE) de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado por:
     * <ul>
     *   <li>Operaciones que requieren múltiples consultas coordinadas</li>
     *   <li>Rollback automático si alguna operación falla</li>
     * </ul>
     *
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @return Lista de historias clínicas activas (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    public List<HistoriaClinica> getAllTx(Connection conn) throws Exception {
        List<HistoriaClinica> historialClinico = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                historialClinico.add(mapResultSetToHistoriaClinica(rs));
            }
        }

        return historialClinico;
    }

    /**
     * Obtiene historias clínicas por el ID del paciente
     * Solo retorna historias clínicas activas (eliminado=FALSE)
     *
     * @param pacienteId ID del paciente dueño de las historias clínicas a buscar
     * @return Lista de historias clínicas encontradas (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    public List<HistoriaClinica> getByPacienteId(int pacienteId) throws Exception {
        List<HistoriaClinica> historial = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_PACIENTE_ID_SQL)) {

            stmt.setInt(1, pacienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historial.add(mapResultSetToHistoriaClinica(rs));
                }
            }
        }

        return historial;
    }

    /**
     * Obtiene historias clínicas por el ID del paciente dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado por:
     * <ul>
     *   <li>Operaciones que requieren múltiples consultas coordinadas</li>
     *   <li>Rollback automático si alguna operación falla</li>
     * </ul>
     *
     * @param pacienteId ID del paciente dueño de las historias clínicas a buscar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @return Lista de historias clínicas encontradas (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    public List<HistoriaClinica> getByPacienteIdTx(int pacienteId, Connection conn) throws Exception {
        List<HistoriaClinica> historial = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_PACIENTE_ID_SQL)) {

            stmt.setInt(1, pacienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historial.add(mapResultSetToHistoriaClinica(rs));
                }
            }
        }

        return historial;
    }

    // =====================================================
    // MÉTODOS DE AUDITORÍA (incluyen registros eliminados)
    // =====================================================
    
    /**
     * Obtiene una historia clínica por su ID incluyendo registros eliminados
     * Usado para propósitos de auditoría, reportes y recuperación de datos
     *
     * @param id ID de la historia clínica a buscar
     * @return HistoriaClinica encontrada (puede estar eliminada), o null si no existe
     * @throws Exception Si hay error de BD
     */
    public HistoriaClinica getByIdAudit(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_AUDIT_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistoriaClinica(rs);
                }
                return null;
            }
        } 
    }

    /**
     * Obtiene una historia clínica por su ID incluyendo registros eliminados dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado para propósitos de auditoría, reportes y recuperación de datos
     *
     * @param id ID de la historia clínica a buscar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @return HistoriaClinica encontrada (puede estar eliminada), o null si no existe
     * @throws Exception Si hay error de BD
     */
    public HistoriaClinica getByIdAuditTx(int id, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_AUDIT_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistoriaClinica(rs);
                }
                return null;
            }
        }
    }

    /**
     * Obtiene todas las historias clínicas incluyendo registros eliminados
     * Usado para propósitos de auditoría, reportes y análisis históricos
     *
     * @return Lista de todas las historias clínicas (activas y eliminadas)
     * @throws Exception Si hay error de BD
     */
    public List<HistoriaClinica> getAllAudit() throws Exception {
        List<HistoriaClinica> historialClinico = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_AUDIT_SQL)) {
            
            while (rs.next()) {
                historialClinico.add(mapResultSetToHistoriaClinica(rs));
            }
        }

        return historialClinico;
    }

    /**
     * Obtiene todas las historias clínicas incluyendo registros eliminados dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado para propósitos de auditoría, reportes y análisis históricos
     *
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @return Lista de todas las historias clínicas (activas y eliminadas)
     * @throws Exception Si hay error de BD
     */
    public List<HistoriaClinica> getAllAuditTx(Connection conn) throws Exception {
        List<HistoriaClinica> historialClinico = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_AUDIT_SQL)) {

            while (rs.next()) {
                historialClinico.add(mapResultSetToHistoriaClinica(rs));
            }
        }

        return historialClinico;
    }

    /**
     * Obtiene historias clínicas por el ID del paciente incluyendo registros eliminados
     * Usado para propósitos de auditoría, reportes y análisis históricos del paciente
     *
     * @param pacienteId ID del paciente dueño de las historias clínicas a buscar
     * @return Lista de historias clínicas encontradas (activas y eliminadas)
     * @throws Exception Si hay error de BD
     */
    public List<HistoriaClinica> getByPacienteIdAudit(int pacienteId) throws Exception {
        List<HistoriaClinica> historial = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_PACIENTE_ID_AUDIT_SQL)) {

            stmt.setInt(1, pacienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historial.add(mapResultSetToHistoriaClinica(rs));
                }
            }
        }

        return historial;
    }

    /**
     * Obtiene historias clínicas por el ID del paciente incluyendo registros eliminados dentro de una transacción existente
     * NO crea nueva conexión, recibe una Connection externa
     * NO cierra la conexión (responsabilidad del caller con TransactionManager)
     * <p>
     * Usado para propósitos de auditoría, reportes y análisis históricos del paciente
     *
     * @param pacienteId ID del paciente dueño de las historias clínicas a buscar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @return Lista de historias clínicas encontradas (activas y eliminadas)
     * @throws Exception Si hay error de BD
     */
    public List<HistoriaClinica> getByPacienteIdAuditTx(int pacienteId, Connection conn) throws Exception {
        List<HistoriaClinica> historial = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_PACIENTE_ID_AUDIT_SQL)) {

            stmt.setInt(1, pacienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historial.add(mapResultSetToHistoriaClinica(rs));
                }
            }
        }

        return historial;
    }
    
    // ============================
    // MÉTODOS PRIVADOS AUXILIARES
    // ============================
    
    /**
     * Establece los parámetros de la historia clínica en un PreparedStatement
     * Método auxiliar usado por insertar() e insertTx()
     * <p>
     * Parámetros seteados:
     * <ol>
     *   <li>nro_historia (String)</li>
     *   <li>grupo_sanguineo (String)</li>
     *   <li>antecedentes (String)</li>
     *   <li>medicacion_actual (String)</li>
     *   <li>observaciones (String)</li>
     * </ol>
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param historiaClinica HistoriaClinica con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setHistoriaClinicaParameters(PreparedStatement stmt, HistoriaClinica historiaClinica) throws SQLException {
        
        stmt.setString(1, historiaClinica.getNroHistoria());
        stmt.setString(2, historiaClinica.getGrupoSanguineo().getSimbolo());
        stmt.setString(3, historiaClinica.getAntecedentes());
        stmt.setString(4, historiaClinica.getMedicacionActual());
        stmt.setString(5, historiaClinica.getObservaciones());
    }

    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT
     * Asigna el ID generado al objeto historiaClinica
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param historiaClinica Objeto historia clínica a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado (indica problema grave)
     */
    private void setGeneratedId(PreparedStatement stmt, HistoriaClinica historiaClinica) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                historiaClinica.setId(generatedKeys.getInt(1));
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto HistoriaClinica
     * Reconstruye el objeto usando el constructor completo
     * <p>
     * Mapeo de columnas:
     * <ul>
     *   <li>id → id</li>
     *   <li>nro_historia → nroHistoria</li>
     *   <li>grupo_sanguineo → grupoSanguineo</li>
     *   <li>antecedentes → antecedentes</li>
     *   <li>medicacion_actual → medicacionActual</li>
     *   <li>observaciones → observaciones</li>
     *   <li>eliminado → eliminado</li>
     * </ul>
     * <p>
     * Nota: Aunque las queries estándar filtran por eliminado=FALSE, el campo se mapea
     * para mantener la integridad del objeto, especialmente en consultas de auditoría
     *
     * @param rs ResultSet posicionado en una fila con datos de la historia clínica
     * @return HistoriaClinica reconstruida
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private HistoriaClinica mapResultSetToHistoriaClinica(ResultSet rs) throws SQLException {
        return new HistoriaClinica(
            rs.getInt("id"),
            rs.getBoolean("eliminado"),
            rs.getString("nro_historia"),
            parseGrupoSanguineo(rs.getString("grupo_sanguineo")),
            rs.getString("antecedentes"),
            rs.getString("medicacion_actual"),
            rs.getString("observaciones")
        );
    }
    
    private GrupoSanguineo parseGrupoSanguineo(String simbolo) {
        for (GrupoSanguineo gs : GrupoSanguineo.values()) {
            if (gs.getSimbolo().equals(simbolo)) {
                return gs;
            }
        }
        return GrupoSanguineo.O_POSITIVO;
    }

}