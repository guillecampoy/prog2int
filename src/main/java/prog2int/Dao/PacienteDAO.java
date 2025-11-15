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

public class PacienteDAO implements GenericDAO<Paciente> {

    private static final String INSERT_SQL = "INSERT INTO paciente (nombre, apellido, dni, fecha_nacimiento) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_SQL = "UPDATE paciente SET nombre = ?, apellido = ?, dni = ?, fecha_nacimiento = ? WHERE id = ?";

    private static final String DELETE_SQL = "UPDATE paciente SET eliminado = TRUE WHERE id = ?";

    private static final String SELECT_BY_ID_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " +
    "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
    "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
    "WHERE p.id = ? AND eliminado = FALSE";

    private static final String SELECT_ALL_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
        "WHERE eliminado = FALSE";

    private static final String SEARCH_BY_NAME_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
        "WHERE eliminado = FALSE AND (p.nombre LIKE ? OR p.apellido LIKE ?)";

    private static final String SEARCH_BY_DNI_SQL = "SELECT p.id, p.nombre, p.apellido, p.dni, p.fecha_nacimiento, " + 
        "hc.id, hc.nro_historia, hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, hc.observaciones " + 
        "FROM paciente p LEFT JOIN historias_clinicas hc ON p.id = hc.paciente_id " + 
        "WHERE p.dni = ? AND eliminado = FALSE";

    private final HistoriaClinicaDAO historiaClinicaDAO;

    public PacienteDAO(HistoriaClinicaDAO historiaClinicaDAO) {
        if (historiaClinicaDAO == null) {
            throw new IllegalArgumentException("HistoriaClinicaDao no puede ser null.");
        }

        this.historiaClinicaDAO = historiaClinicaDAO;
    }

    @Override
    public void insertar(Paciente paciente) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            
            setPacienteParameters(stmt, paciente);
            stmt.executeUpdate();
            setGeneratedId(stmt, paciente);
        }
    }

    @Override
    public void insertTx(Paciente paciente, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            
            setPacienteParameters(stmt, paciente);
            stmt.executeUpdate();
            setGeneratedId(stmt, paciente);
        }
    }

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

    public void deleteTx(int id, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el paciente con ID: " + id);
            }
        }
    }

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

    private void setPacienteParameters(PreparedStatement stmt, Paciente paciente) throws SQLException {
        stmt.setString(1, paciente.getNombre());
        stmt.setString(2, paciente.getApellido());
        stmt.setString(3, paciente.getDni());
        stmt.setDate(4, Date.valueOf(paciente.getFechaNacimiento()));
    }

    private void setGeneratedId(PreparedStatement stmt, Paciente paciente) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                paciente.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción del paciente falló, no se obtuvo ID generado");
            }
        }
    }

    private Paciente mapResultSetToPaciente(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente();
        paciente.setId(rs.getInt("id"));
        paciente.setNombre(rs.getString("nombre"));
        paciente.setApellido(rs.getString("apellido"));
        paciente.setDni(rs.getString("dni"));
        paciente.setFechaNacimiento(rs.getDate("fecha_nacimiento").toLocalDate());

        if (rs.getInt("hs.id") > 0 && !rs.wasNull()) {
            HistoriaClinica historiaClinica = new HistoriaClinica();
            historiaClinica.setId(rs.getInt("hc.id"));
            historiaClinica.setNroHistoria(rs.getString("hc.nro_historia"));
            historiaClinica.setGrupoSanguineo(GrupoSanguineo.valueOf(rs.getString("grupo_sanguineo")));
            historiaClinica.setAntecedentes(rs.getString("hc.antecedentes"));
            historiaClinica.setMedicacionActual(rs.getString("hc.medicacion_actual"));
            historiaClinica.setObservaciones(rs.getString("observaciones"));
            paciente.setHistoriaClinica(historiaClinica);
        }

        return paciente;
    }

}
