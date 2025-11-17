package prog2int.Services;

import prog2int.Config.DatabaseConnection;
import prog2int.Config.TransactionManager;
import prog2int.Dao.HistoriaClinicaDAO;
import prog2int.Dao.PacienteDAO;
import prog2int.Models.HistoriaClinica;
import prog2int.Models.Paciente;

/**
 * Servicio coordinador para operaciones transaccionales entre Paciente e HistoriaClinica.
 * 
 * Responsabilidades:
 * - Coordinar operaciones compuestas en una sola transacción
 * - Garantizar atomicidad (commit/rollback)
 * - Mantener consistencia entre ambas entidades
 */
public class PacienteHistoriaClinicaService {
    
    private final PacienteDAO pacienteDAO;
    private final HistoriaClinicaDAO historiaClinicaDAO;
    
    public PacienteHistoriaClinicaService(PacienteDAO pacienteDAO, 
                                          HistoriaClinicaDAO historiaClinicaDAO) {
        if (pacienteDAO == null || historiaClinicaDAO == null) {
            throw new IllegalArgumentException("Los DAOs no pueden ser null");
        }
        this.pacienteDAO = pacienteDAO;
        this.historiaClinicaDAO = historiaClinicaDAO;
    }
    
    /**
     * Crea un paciente con su historia clínica en una transacción atómica.
     * Si falla cualquier operación, se hace rollback completo.
     */
    public void crearPacienteConHistoria(Paciente paciente, HistoriaClinica historia) 
        throws Exception {
        
        try (TransactionManager tm = new TransactionManager(
                DatabaseConnection.getConnection())) {
            
            tm.startTransaction();
            
            try {
                // 1. Insertar historia clínica
                historiaClinicaDAO.insertTx(historia, tm.getConnection());
                
                // 2. Asociar historia al paciente
                paciente.setHistoriaClinica(historia);
                
                // 3. Insertar paciente
                pacienteDAO.insertTx(paciente, tm.getConnection());
                
                tm.commit();
            } catch (Exception e) {
                tm.rollback();
                throw new Exception("Error al crear paciente con historia: " + 
                                    e.getMessage(), e);
            }
        }
    }
    
    /**
     * Elimina un paciente y su historia clínica de forma coordinada.
     * Ambas operaciones se ejecutan en una transacción.
     */
    public void eliminarPacienteConHistoria(int pacienteId) throws Exception {
        try (TransactionManager tm = new TransactionManager(
                DatabaseConnection.getConnection())) {
            
            tm.startTransaction();
            
            try {
                // 1. Obtener paciente
                Paciente paciente = pacienteDAO.getByIdTx(pacienteId, tm.getConnection());
                if (paciente == null) {
                    throw new IllegalArgumentException("Paciente no encontrado");
                }
                
                // 2. Eliminar historia si existe
                if (paciente.getHistoriaClinica() != null) {
                    historiaClinicaDAO.eliminarTx(
                        paciente.getHistoriaClinica().getId(), 
                        tm.getConnection()
                    );
                }
                
                // 3. Eliminar paciente
                pacienteDAO.eliminarTx(pacienteId, tm.getConnection());
                
                tm.commit();
            } catch (Exception e) {
                tm.rollback();
                throw new Exception("Error al eliminar paciente con historia: " + 
                                    e.getMessage(), e);
            }
        }
    }
}
