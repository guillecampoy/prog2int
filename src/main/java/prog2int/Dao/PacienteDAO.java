package prog2int.Dao;

import java.sql.Connection;
import java.util.List;

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
        "WHERE eliminado = FALSE AND (p.nombre LLIKE ? OR p.apellido LIKE ?)";

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
    public void insertar(Paciente entidad) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'insertar'");
    }

    @Override
    public void insertTx(Paciente entidad, Connection conn) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'insertTx'");
    }

    @Override
    public void actualizar(Paciente entidad) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'actualizar'");
    }

    public void updateTx(Paciente entidad, Connection conn) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'actualizar'");
    }

    @Override
    public void eliminar(int id) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'eliminar'");
    }

    public void deleteTx(int id, Connection conn) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'eliminar'");
    }

    @Override
    public Paciente getById(int id) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    public Paciente getByIdTx(int id, Connection conn) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public List<Paciente> getAll() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }

    public List<Paciente> getAllTx(Connection conn) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }

}
