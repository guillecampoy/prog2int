package prog2int.Services;

import org.junit.jupiter.api.*;
import prog2int.Dao.HistoriaClinicaDAO;
import prog2int.Dao.PacienteDAO;
import prog2int.Models.Paciente;
import prog2int.Models.HistoriaClinica;
import prog2int.Models.HistoriaClinica.GrupoSanguineo;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PacienteServiceTest {
    
    private static PacienteServiceImpl pacienteService;
    private static HistoriaClinicaServiceImpl historiaService;
    private static int pacienteId1;
    private static int pacienteId2;
    private static int historiaId;

    @BeforeAll
    static void setup() throws Exception {
        // Limpiar BD antes de tests
        try (java.sql.Connection conn = prog2int.Config.DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("DELETE FROM historias_clinicas");
            stmt.execute("DELETE FROM paciente");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
        
        HistoriaClinicaDAO historiaDAO = new HistoriaClinicaDAO();
        PacienteDAO pacienteDAO = new PacienteDAO(historiaDAO);
        historiaService = new HistoriaClinicaServiceImpl(historiaDAO);
        pacienteService = new PacienteServiceImpl(pacienteDAO, historiaService);
    }

    @Test
    @Order(1)
    void testInsertarPacienteSinHistoria() throws Exception {
        Paciente paciente = new Paciente(0, "Juan", "Pérez", "12345678", 
            LocalDate.of(1990, 1, 1), false, null);
        
        pacienteService.insertar(paciente);
        pacienteId1 = paciente.getId();
        
        assertTrue(pacienteId1 > 0, "ID debe ser generado");
        System.out.println("✓ Paciente insertado con ID: " + pacienteId1);
    }

    @Test
    @Order(2)
    void testBuscarPorDni() throws Exception {
        Paciente encontrado = pacienteService.buscarPorDni("12345678");
        
        assertNotNull(encontrado);
        assertEquals("Juan", encontrado.getNombre());
        assertEquals("Pérez", encontrado.getApellido());
        System.out.println("✓ Paciente encontrado por DNI");
    }

    @Test
    @Order(3)
    void testValidacionDniDuplicado() {
        Paciente duplicado = new Paciente(0, "Pedro", "López", "12345678", 
            LocalDate.of(1985, 5, 10), false, null);
        
        Exception ex = assertThrows(Exception.class, () -> pacienteService.insertar(duplicado));
        assertTrue(ex.getMessage().contains("Ya existe un paciente con el DNI"));
        System.out.println("✓ Validación DNI duplicado funciona");
    }

    @Test
    @Order(4)
    void testValidacionDniFormato() {
        Paciente invalido = new Paciente(0, "Ana", "Gómez", "123", 
            LocalDate.of(1992, 3, 15), false, null);
        
        Exception ex = assertThrows(Exception.class, () -> pacienteService.insertar(invalido));
        assertTrue(ex.getMessage().contains("7 y 8 dígitos"));
        System.out.println("✓ Validación formato DNI funciona");
    }

    @Test
    @Order(5)
    void testValidacionFechaFutura() {
        Paciente invalido = new Paciente(0, "Carlos", "Ruiz", "11111111", 
            LocalDate.now().plusDays(1), false, null);
        
        Exception ex = assertThrows(Exception.class, () -> pacienteService.insertar(invalido));
        assertTrue(ex.getMessage().contains("no puede ser futura"));
        System.out.println("✓ Validación fecha futura funciona");
    }

    @Test
    @Order(6)
    void testActualizarPaciente() throws Exception {
        Paciente paciente = pacienteService.getById(pacienteId1);
        paciente.setNombre("Juan Carlos");
        
        pacienteService.actualizar(paciente);
        
        Paciente actualizado = pacienteService.getById(pacienteId1);
        assertEquals("Juan Carlos", actualizado.getNombre());
        System.out.println("✓ Paciente actualizado correctamente");
    }

    @Test
    @Order(7)
    void testInsertarPacienteConHistoria() throws Exception {
        HistoriaClinica historia = new HistoriaClinica(0, false, "HC-0001", 
            GrupoSanguineo.A_POSITIVO, "Hipertensión", "Enalapril", "Control mensual");
        
        Paciente paciente = new Paciente(0, "María", "García", "87654321", 
            LocalDate.of(1995, 5, 15), false, historia);
        
        pacienteService.insertar(paciente);
        pacienteId2 = paciente.getId();
        historiaId = paciente.getHistoriaClinica().getId();
        
        assertTrue(pacienteId2 > 0);
        assertTrue(historiaId > 0);
        System.out.println("✓ Paciente con historia insertado (transacción) - ID: " + pacienteId2 + ", Historia ID: " + historiaId);
    }

    @Test
    @Order(8)
    void testValidacionFormatoHistoria() {
        HistoriaClinica invalida = new HistoriaClinica(0, false, "H001", 
            GrupoSanguineo.A_POSITIVO, "Test", null, null);
        
        Exception ex = assertThrows(Exception.class, () -> historiaService.insertar(invalida));
        assertTrue(ex.getMessage().contains("HC-XXXX"));
        System.out.println("✓ Validación formato historia funciona");
    }

    @Test
    @Order(9)
    void testGetAllPacientes() throws Exception {
        List<Paciente> pacientes = pacienteService.getAll();
        
        assertFalse(pacientes.isEmpty());
        assertTrue(pacientes.size() >= 1);
        System.out.println("✓ Listado de pacientes: " + pacientes.size() + " encontrados");
    }

    @Test
    @Order(10)
    void testBuscarPorNombreApellido() throws Exception {
        List<Paciente> resultados = pacienteService.buscarPorNombreApellido("Juan");
        
        assertFalse(resultados.isEmpty());
        System.out.println("✓ Búsqueda por nombre: " + resultados.size() + " resultados");
    }

    @Test
    @Order(11)
    void testBuscarPacienteConHistoria() throws Exception {
        Paciente encontrado = pacienteService.getById(pacienteId2);
        
        assertNotNull(encontrado);
        assertNotNull(encontrado.getHistoriaClinica());
        assertEquals("HC-0001", encontrado.getHistoriaClinica().getNroHistoria());
        assertEquals(historiaId, encontrado.getHistoriaClinica().getId());
        System.out.println("✓ Paciente con historia recuperado correctamente");
    }

    @Test
    @Order(12)
    void testEliminarPaciente() throws Exception {
        pacienteService.eliminar(pacienteId1);
        
        Paciente eliminado = pacienteService.getById(pacienteId1);
        assertNull(eliminado, "Paciente eliminado no debe aparecer en consultas normales");
        System.out.println("✓ Soft delete funciona correctamente");
    }

    @Test
    @Order(13)
    void testGetAllHistorias() throws Exception {
        List<HistoriaClinica> historias = historiaService.getAll();
        
        assertFalse(historias.isEmpty());
        assertTrue(historias.size() >= 1);
        System.out.println("✓ Listado de historias: " + historias.size() + " encontradas");
    }

    @Test
    @Order(14)
    void testActualizarHistoria() throws Exception {
        HistoriaClinica historia = historiaService.getById(historiaId);
        assertNotNull(historia);
        
        historia.setAntecedentes("Hipertensión controlada");
        historiaService.actualizar(historia);
        
        HistoriaClinica actualizada = historiaService.getById(historiaId);
        assertEquals("Hipertensión controlada", actualizada.getAntecedentes());
        System.out.println("✓ Historia clínica actualizada correctamente");
    }

    @Test
    @Order(15)
    void testEliminarHistoria() throws Exception {
        historiaService.eliminar(historiaId);
        
        HistoriaClinica eliminada = historiaService.getById(historiaId);
        assertNull(eliminada);
        System.out.println("✓ Historia clínica eliminada correctamente");
    }
}
