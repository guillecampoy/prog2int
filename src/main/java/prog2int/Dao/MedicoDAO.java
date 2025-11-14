package prog2int.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import prog2int.Config.DatabaseConnection;

public class MedicoDAO implements GenericDAO<Medico> {

    private static final String INSERT_SQL = "INSERT INTO medico (matricula, nombre, apellido, especialidad, telefono, email, activo) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL = "UPDATE medico SET matricula = ?, nombre = ?, apellido = ?, especialidad = ?, telefono = ?, email = ?, activo = ? WHERE id = ?";

    private static final String DELETE_SQL = "UPDATE medico SET activo = FALSE WHERE id = ?";

    private static final String SELECT_BY_ID_SQL = "SELECT * FROM medico WHERE id = ? AND activo = TRUE";

    private static final String SELECT_ALL_SQL = "SELECT * FROM medico WHERE activo = TRUE";

    private static final String SELECT_BY_MATRICULA = "SELECT * FROM medico WHERE matricula = ? AND activo = TRUE";

    private static final String SELECT_BY_ESPECIALIDAD = "SELECT * FROM medico WHERE especialidad LIKE ? AND activo = TRUE";

    @Override
    public void insertar(Medico medico) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setMedicoParameters(stmt, medico);
            stmt.executeUpdate();

            setGeneratedId(stmt, medico);
        }
    }

    @Override
    public void insertTx(Medico medico, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setMedicoParameters(stmt, medico);
            stmt.executeUpdate();
            setGeneratedId(stmt, medico);
        }
    }

    @Override
    public void actualizar(Medico medico) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setString(1, medico.getMatricula());
            stmt.setString(2, medico.getNombre());
            stmt.setString(3, medico.getApellido());
            stmt.setString(4, medico.getEspecialidad());
            stmt.setString(5, medico.getTelefono());
            stmt.setString(6, medico.getEmail());
            stmt.setInt(7, medico.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el medico con ID: " + medico.getId());
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
                throw new SQLException("No se encontro medico con ID: " + id);
            }
        }
    }

    @Override
    public Medico getById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return mapResultSetToMedico(rs);
            }
        }
    }

    @Override
    public List<Medico> getAll() throws Exception {
        List<Medico> medicos = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {
            
            while (rs.next()) {
                medicos.add(mapResultSetToMedico(rs));
            }
        }

        return medicos;
    }

    public Medico getByMatricula(String matricula) throws SQLException {
        if (matricula == null || matricula.trim().isEmpty()) {
            throw new IllegalArgumentException("La matricula no puede estar vacia");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_MATRICULA)) {
            
            stmt.setString(1, matricula);

            try (ResultSet rs = stmt.executeQuery()) {
                return mapResultSetToMedico(rs);
            }
        }
    }

    public List<Medico> getByEspecialidad(String especialidad) throws Exception {
        if (especialidad == null || especialidad.trim().isEmpty()) {
            throw new IllegalArgumentException("La especialidad no puede estar vacia");
        }

        List<Medico> medicos = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ESPECIALIDAD)) {

            String searchPattern = "%" + especialidad + "%";
            stmt.setString(1, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    medicos.add(mapResultSetToMedico(rs));
                }
            }
        }

        return medicos;
    }

    private void setMedicoParameters(PreparedStatement stmt, Medico medico) throws SQLException {
        stmt.setString(1, medico.getMatricula());
        stmt.setString(2, medico.getNombre());
        stmt.setString(3, medico.getApellido());
        stmt.setString(4, medico.getEspecialidad());
        stmt.setString(5, medico.getTelefono());
        stmt.setString(6, medico.getEmail());
        stmt.setBoolean(7, medico.getActivo());
    }

    private void setGeneratedId(PreparedStatement stmt, Medico medico) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                medico.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La insercion del medico fallo, no se obtuvo el ID generado");
            }
        }
    }

    private Medico mapResultSetToMedico(ResultSet rs) throws SQLException {
        return new Medico(
            rs.getInt("id_medico"),
            rs.getString("matricula"),
            rs.getString("nombre"),
            rs.getString("apellido"),
            rs.getString("especialidad"),
            rs.getString("telefono"),
            rs.getString("email")
        );
    }

}
