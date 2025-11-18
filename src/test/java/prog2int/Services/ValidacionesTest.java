package prog2int.Services;

import org.junit.jupiter.api.Test;
import prog2int.Models.Paciente;
import prog2int.Models.HistoriaClinica;
import prog2int.Models.HistoriaClinica.GrupoSanguineo;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de validaciones sin necesidad de BD
 */
class ValidacionesTest {

    @Test
    void testDniFormatoValido() {
        assertTrue("12345678".matches("^\\d{7,8}$"));
        assertTrue("1234567".matches("^\\d{7,8}$"));
        System.out.println("✓ DNI 7-8 dígitos válido");
    }

    @Test
    void testDniFormatoInvalido() {
        assertFalse("123".matches("^\\d{7,8}$"));
        assertFalse("123456789".matches("^\\d{7,8}$"));
        assertFalse("12.345.678".matches("^\\d{7,8}$"));
        System.out.println("✓ DNI inválido detectado");
    }

    @Test
    void testHistoriaFormatoValido() {
        assertTrue("HC-0001".matches("^HC-\\d{4}$"));
        assertTrue("HC-9999".matches("^HC-\\d{4}$"));
        System.out.println("✓ Formato HC-XXXX válido");
    }

    @Test
    void testHistoriaFormatoInvalido() {
        assertFalse("H001".matches("^HC-\\d{4}$"));
        assertFalse("HC-01".matches("^HC-\\d{4}$"));
        assertFalse("HC-00001".matches("^HC-\\d{4}$"));
        System.out.println("✓ Formato historia inválido detectado");
    }

    @Test
    void testFechaNoFutura() {
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);
        LocalDate manana = hoy.plusDays(1);
        
        assertFalse(ayer.isAfter(hoy));
        assertTrue(manana.isAfter(hoy));
        System.out.println("✓ Validación fecha futura funciona");
    }

    @Test
    void testPacienteConstructor() {
        Paciente p = new Paciente(1, "Juan", "Pérez", "12345678", 
            LocalDate.of(1990, 1, 1), false, null);
        
        assertEquals("Juan", p.getNombre());
        assertEquals("Pérez", p.getApellido());
        assertEquals("12345678", p.getDni());
        assertNull(p.getHistoriaClinica());
        System.out.println("✓ Constructor Paciente funciona");
    }

    @Test
    void testHistoriaClinicaConstructor() {
        HistoriaClinica h = new HistoriaClinica(1, false, "HC-0001", 
            GrupoSanguineo.O_POSITIVO, "Ninguno", null, null);
        
        assertEquals("HC-0001", h.getNroHistoria());
        assertEquals(GrupoSanguineo.O_POSITIVO, h.getGrupoSanguineo());
        assertEquals("O+", h.getGrupoSanguineoSymbol());
        System.out.println("✓ Constructor HistoriaClinica funciona");
    }

    @Test
    void testGrupoSanguineoEnum() {
        assertEquals("A+", GrupoSanguineo.A_POSITIVO.getSimbolo());
        assertEquals("A-", GrupoSanguineo.A_NEGATIVO.getSimbolo());
        assertEquals("B+", GrupoSanguineo.B_POSITIVO.getSimbolo());
        assertEquals("B-", GrupoSanguineo.B_NEGATIVO.getSimbolo());
        assertEquals("AB+", GrupoSanguineo.AB_POSITIVO.getSimbolo());
        assertEquals("AB-", GrupoSanguineo.AB_NEGATIVO.getSimbolo());
        assertEquals("O+", GrupoSanguineo.O_POSITIVO.getSimbolo());
        assertEquals("O-", GrupoSanguineo.O_NEGATIVO.getSimbolo());
        System.out.println("✓ Enum GrupoSanguineo funciona");
    }

    @Test
    void testPacienteConHistoria() {
        HistoriaClinica h = new HistoriaClinica(1, false, "HC-0001", 
            GrupoSanguineo.A_POSITIVO, "Diabetes", "Insulina", "Control diario");
        
        Paciente p = new Paciente(1, "María", "García", "87654321", 
            LocalDate.of(1995, 5, 15), false, h);
        
        assertNotNull(p.getHistoriaClinica());
        assertEquals("HC-0001", p.getHistoriaClinica().getNroHistoria());
        assertEquals("A+", p.getHistoriaClinica().getGrupoSanguineoSymbol());
        System.out.println("✓ Relación Paciente-Historia funciona");
    }

    @Test
    void testSoftDelete() {
        Paciente p = new Paciente(1, "Test", "Test", "11111111", 
            LocalDate.now(), false, null);
        
        assertFalse(p.isEliminado());
        p.setEliminado(true);
        assertTrue(p.isEliminado());
        System.out.println("✓ Soft delete flag funciona");
    }
}
