package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.domain.Cita;
import com.VetSphere.Backend.modules.clinic.domain.EtapaFlujoRecepcion;
import com.VetSphere.Backend.modules.clinic.dto.EtapaFlujoRecepcionRequest;
import com.VetSphere.Backend.modules.clinic.repository.CitaRepository;
import com.VetSphere.Backend.modules.clinic.repository.EtapaFlujoRecepcionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlujoRecepcionServiceTest {
    private EtapaFlujoRecepcionRepository etapaRepository;
    private CitaRepository citaRepository;
    private FlujoRecepcionService service;

    @BeforeEach
    void setUp() {
        etapaRepository = mock(EtapaFlujoRecepcionRepository.class);
        citaRepository = mock(CitaRepository.class);
        service = new FlujoRecepcionService(etapaRepository, citaRepository);
    }

    @Test
    void inicializaLasEtapasPorDefectoCuandoSeConsultaElFlujoVacio() {
        when(etapaRepository.count()).thenReturn(0L);
        when(etapaRepository.findAllByOrderByPosicionAscIdAsc()).thenReturn(List.of());

        service.listar();

        verify(etapaRepository, org.mockito.Mockito.times(8)).save(any(EtapaFlujoRecepcion.class));
    }

    @Test
    void crearNuevaEtapaAsociaUnEstadoDeCitaYGuardaSuPosicion() {
        EtapaFlujoRecepcion existente = etapa("Agenda", "Programada", 1);
        when(etapaRepository.count()).thenReturn(1L);
        when(etapaRepository.findAllByOrderByPosicionAscIdAsc()).thenReturn(List.of(existente));
        when(etapaRepository.findByNombreIgnoreCase("Recepcion" )).thenReturn(Optional.empty());
        when(etapaRepository.findByEstadoCitaIgnoreCase("Recibida" )).thenReturn(Optional.empty());
        when(etapaRepository.save(any(EtapaFlujoRecepcion.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaFlujoRecepcion resultado = service.crear(new EtapaFlujoRecepcionRequest(
                "Recepcion", "Recibida", "Paciente llego", "blue", null, true));

        assertEquals("Recibida", resultado.getEstadoCita());
        assertEquals(2, resultado.getPosicion());
        assertEquals("blue", resultado.getColor());
    }

    @Test
    void crearEtapaConPosicionFueraDeRangoLaColocaAlFinalSinDejarHuecos() {
        EtapaFlujoRecepcion existente = etapa("Agenda", "Programada", 1);
        when(etapaRepository.count()).thenReturn(1L);
        when(etapaRepository.findAllByOrderByPosicionAscIdAsc()).thenReturn(List.of(existente));
        when(etapaRepository.findByNombreIgnoreCase("Hospitalizacion")).thenReturn(Optional.empty());
        when(etapaRepository.findByEstadoCitaIgnoreCase("Internada")).thenReturn(Optional.empty());
        when(etapaRepository.save(any(EtapaFlujoRecepcion.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaFlujoRecepcion resultado = service.crear(new EtapaFlujoRecepcionRequest(
                "Hospitalizacion", "Internada", "Ingreso a hospitalizacion", "blue", 50, true));

        assertEquals(2, resultado.getPosicion());
    }

    @Test
    void actualizarElEstadoDeEtapaActualizaLasCitasQueYaLoUsan() {
        EtapaFlujoRecepcion etapa = etapa("Triage", "En triage", 2);
        Cita cita = new Cita();
        cita.setEstado("En triage");
        when(etapaRepository.count()).thenReturn(1L);
        when(etapaRepository.findAllByOrderByPosicionAscIdAsc()).thenReturn(List.of(etapa));
        when(etapaRepository.findById(4L)).thenReturn(Optional.of(etapa));
        when(etapaRepository.findByNombreIgnoreCase("Evaluacion" )).thenReturn(Optional.empty());
        when(etapaRepository.findByEstadoCitaIgnoreCase("Evaluacion inicial" )).thenReturn(Optional.empty());
        when(citaRepository.findAll()).thenReturn(List.of(cita));
        when(etapaRepository.save(any(EtapaFlujoRecepcion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(4L, new EtapaFlujoRecepcionRequest(
                "Evaluacion", "Evaluacion inicial", "Evaluar prioridad", "violet", 2, true));

        assertEquals("Evaluacion inicial", cita.getEstado());
        verify(etapaRepository).save(etapa);
    }

    @Test
    void etapaConCitasSeDesactivaEnLugarDeEliminarse() {
        EtapaFlujoRecepcion etapa = etapa("Atencion", "En curso", 1);
        when(etapaRepository.findById(4L)).thenReturn(Optional.of(etapa));
        when(etapaRepository.findByActivaTrueOrderByPosicionAscIdAsc()).thenReturn(List.of(etapa, etapa("Agenda", "Programada", 2)));
        when(citaRepository.existsByEstadoIgnoreCase("En curso")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> service.eliminar(4L));

        verify(etapaRepository, never()).delete(any(EtapaFlujoRecepcion.class));
    }

    @Test
    void noPermiteDesactivarLaUltimaEtapaActiva() {
        EtapaFlujoRecepcion etapa = etapa("Agenda", "Programada", 1);
        when(etapaRepository.count()).thenReturn(1L);
        when(etapaRepository.findAllByOrderByPosicionAscIdAsc()).thenReturn(List.of(etapa));
        when(etapaRepository.findById(4L)).thenReturn(Optional.of(etapa));
        when(etapaRepository.findByActivaTrueOrderByPosicionAscIdAsc()).thenReturn(List.of(etapa));

        assertThrows(RuntimeException.class, () -> service.actualizar(4L, new EtapaFlujoRecepcionRequest(
                "Agenda", "Programada", "Cita reservada", "cyan", 1, false)));

        verify(etapaRepository, never()).save(any(EtapaFlujoRecepcion.class));
    }

    private EtapaFlujoRecepcion etapa(String nombre, String estado, int posicion) {
        return EtapaFlujoRecepcion.builder().id(4L).nombre(nombre).estadoCita(estado).descripcion("")
                .color("cyan").posicion(posicion).activa(true).build();
    }
}
