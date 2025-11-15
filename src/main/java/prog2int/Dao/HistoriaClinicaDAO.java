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

public class HistoriaClinicaDAO implements GenericDAO<HistoriaClinica> {

    private static final String INSERT_SQL = "INSERT INTO historias_clinicas (nro_historia, grupo_sanguineo, antecedentes, medicacion_actual, observaciones) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL = "UPDATE historias_clinicas SET nro_historia = ?, grupo_sanguineo = ?, antecedentes = ?, medicacion_actual = ?, observaciones = ? WHERE id = ?";

    private static final String UPDATE_ID_PACIENTE_SQL = "UPDATE historias_clinicas SET paciente_id = ? WHERE id = ?";

    private static final String DELETE_SQL = "UPDATE historias_clinicas SET eliminado = TRUE WHERE id = ?";

    private static final String SELECT_BY_ID_SQL = "SELECT * FROM historias_clinicas WHERE id = ? AND eliminado = FALSE";

    private static final String SELECT_ALL_SQL = "SELECT * FROM historias_clinicas WHERE eliminado = FALSE";

    private static final String SELECT_BY_PACIENTE_ID = "SELECT * FROM historias_clinicas WHERE paciente_id = ? AND eliminado = FALSE";

    @Override
    public void insertar(HistoriaClinica historiaClinica) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setHistoriaClinicaParameters(stmt, historiaClinica);
            stmt.executeUpdate();
            setGeneratedId(stmt, historiaClinica);
        }
    }

    @Override
    public void insertTx(HistoriaClinica historiaClinica, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            setHistoriaClinicaParameters(stmt, historiaClinica);
            stmt.executeUpdate();
            setGeneratedId(stmt, historiaClinica);
        }
    }

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

    public void actualizarTx(HistoriaClinica historiaClinica, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setString(1, historiaClinica.getNroHistoria());
            stmt.setString(2, historiaClinica.getGrupoSanguineo().getSimbolo());
            stmt.setString(3, historiaClinica.getAntecedentes());
            stmt.setString(4, historiaClinica.getMedicacionActual());
            stmt.setString(5, historiaClinica.getObservaciones());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el historial con ID: " + historiaClinica.getId());
            }
        }
    }

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

    public void eliminarTx(int id, Connection conn) throws Exception {
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
                return null;
            }
        }
    }

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

    public List<HistoriaClinica> getByPacienteId(int pacienteId) throws Exception {
        List<HistoriaClinica> historial = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_PACIENTE_ID)) {

            stmt.setInt(1, pacienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historial.add(mapResultSetToHistoriaClinica(rs));
                }
            }
        }

        return historial;
    }

    public List<HistoriaClinica> getByPacienteIdTx(int pacienteId, Connection conn) throws Exception {
        List<HistoriaClinica> historial = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_PACIENTE_ID)) {

            stmt.setInt(1, pacienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historial.add(mapResultSetToHistoriaClinica(rs));
                }
            }
        }

        return historial;
    }

    private void setHistoriaClinicaParameters(PreparedStatement stmt, HistoriaClinica historiaClinica) throws SQLException {
        
        stmt.setString(1, historiaClinica.getNroHistoria());
        stmt.setString(2, historiaClinica.getGrupoSanguineo().getSimbolo());
        stmt.setString(3, historiaClinica.getAntecedentes());
        stmt.setString(4, historiaClinica.getMedicacionActual());
        stmt.setString(5, historiaClinica.getObservaciones());
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
            rs.getBoolean("eliminado"),
            rs.getString("nro_historia"),
            GrupoSanguineo.valueOf(rs.getString("grupo_sanguineo")),
            rs.getString("antecedentes"),
            rs.getString("medicacion_actual"),
            rs.getString("observaciones")
        );
    }

}
