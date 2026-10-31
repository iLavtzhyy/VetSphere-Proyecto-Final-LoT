package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.repository.ChatConversacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.ChatMensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Retira mensajes y conversaciones que superan el periodo de conservación configurado. */
@Service
@RequiredArgsConstructor
public class ChatRetencionService {
    private final ChatMensajeRepository mensajeRepository;
    private final ChatConversacionRepository conversacionRepository;

    @Value("${app.chat.retencion-dias:7}")
    private int diasRetencion;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void limpiarExpirados() {
        LocalDateTime limite = LocalDateTime.now().minusDays(Math.max(1, diasRetencion));
        mensajeRepository.eliminarAnterioresA(limite);
        conversacionRepository.eliminarAnterioresA(limite);
    }
}
