package prog2int.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import prog2int.Config.DatabaseConnection;

public class HistoriaClinicaDAO implements GenericDAO<HistoriaClinica> {

    private static final String INSERT_SQL = "INSERT INTO historias_clinicas (nro_historia, paciente_id, grupo_sanguineo, antecedentes, medicacion_actual, observaciones) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL = "UPDATE historias_clinicas SET nro_historia = ?, paciente_id = ?, grupo_sanguineo = ?, antecedentes = ?, medicacion_actual = ?, observaciones = ? WHERE id = ?";

    private static final String DELETE_SQL = "UPDATE historias_clinicas SET eliminado = TRUE WHERE id = ?";

    private static final String SELECT_BY_ID_SQL = "SELECT * FROM historias_clinicas WHERE id = ? AND eliminado = FALSE";

    private static final String SELECT_ALL_SQL = "SELECT * FROM historias_clinicas WHERE eliminado = FALSE";

    private static final String SELECT_BY_PACIENTE_ID = "SELECT * FROM historias_clinicas WHERE paciente_id = ? AND eliminado = FALSE";

    @Override
    public void insertar(HistoriaClinica historiaClinica, int pacienteId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setHistoriaClinicaParameters(stmt, historiaClinica, pacienteId);
            stmt.executeUpdate();
            setGeneratedId(stmt, historiaClinica);
        }
    }

    @Override
    public void insertTx(HistoriaClinica historiaClinica, int pacienteId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            setHistoriaClinicaParameters(stmt, historiaClinica, pacienteId);
            stmt.executeUpdate();
            setGeneratedId(stmt, historiaClinica);
        }
    }

    @Override
    public void actualizar(HistoriaClinica historiaClinica, int pacienteId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, historiaClinica.getNroHistoria());
            stmt.setInt(2, pacienteId);
            stmt.setString(3, historiaClinica.getGrupoSanguineo());
            stmt.setString(4, historiaClinica.getAntecedentes());
            stmt.setString(5, historiaClinica.getMedicacionActual());
            stmt.setString(6, historiaClinica.getObservaciones());
            stmt.setInt(7, historiaClinica.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el historial con ID: " + historiaClinica.getId());
            }
        }
    }

    public void actualizarTx(HistoriaClinica historiaClinica, int pacienteId, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setString(1, historiaClinica.getNroHistoria());
            stmt.setInt(2, pacienteId);
            stmt.setString(3, historiaClinica.getGrupoSanguineo());
            stmt.setString(4, historiaClinica.getAntecedentes());
            stmt.setString(5, historiaClinica.getMedicacionActual());
            stmt.setString(6, historiaClinica.getObservaciones());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el historial con ID: " + historiaClinica.getId());
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
                throw new SQLException("No se encontro historia con ID: " + id);
            }
        }
    }

    public void eliminarTx(int id, Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontro historia con ID: " + id);
            }
        }
    }

    @Override
    public HistoriaClinica getById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistoriaClinica(rs);
                }
            }
        }
    }

    public HistoriaClinica getByIdTx(int id, Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistoriaClinica(rs);
                }
            }
        }
    }

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

    public List<HistoriaClinica> getAllTx(Connection conn) {
        List<HistoriaClinica> historialClinico = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                historialClinico.add(mapResultSetToHistoriaClinica(rs));
            }
        }
    }

    private void setHistoriaClinicaParameters(PreparedStatement stmt, HistoriaClinica historiaClinica, int pacienteId) throws SQLException {
        
        stmt.setString(1, historiaClinica.getNroHistoria());
        stmt.setInt(2, pacienteId);
        stmt.setString(3, historiaClinica.getGrupoSanguineo());
        stmt.setString(4, historiaClinica.getAntecedentes());
        stmt.setString(5, historiaClinica.getMedicacionActual());
        stmt.setString(6, historiaClinica.getObservaciones());
    }

    private void setGeneratedId(PreparedStatement stmt, HistoriaClinica historiaClinica) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                historiaClinica.setId(generatedKeys.getInt(1));
            }
        }
    }

    private HistoriaClinica mapResultSetToHistoriaClinica(ResultSet rs) throws SQLException {
        return new HistoriaClinica(
            rs.getInt("id"),
            rs.getString("nro_historia"),
            rs.getString("grupo_sanguineo"),
            rs.getString("antecedentes"),
            rs.getString("medicacion_actual"),
            rs.getString("observaciones")
        );
    }

}
