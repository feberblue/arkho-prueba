package com.management.registration.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.registration.entity.Solicitud;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Slf4j
public class EventPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    public EventPublisher(@Autowired(required = false) SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @Value("${aws.sqs.queue-url:}")
    private String queueUrl;

    @Value("${aws.sqs.enabled:false}")
    private boolean sqsEnabled;

    @Async
    public void publicarSolicitudCreada(Solicitud solicitud) {
        SolicitudCreadaEvent event = SolicitudCreadaEvent.fromSolicitud(
                solicitud.getId(),
                solicitud.getPatente(),
                solicitud.getNombrePropietario(),
                solicitud.getRut(),
                solicitud.getEmail()
        );

        if (sqsEnabled && queueUrl != null && !queueUrl.isEmpty()) {
            enviarASQS(event);
        } else {
            simularEnvio(event);
        }
    }

    private void enviarASQS(SolicitudCreadaEvent event) {
        if (sqsClient == null) {
            log.warn("SqsClient no disponible, simulando envío en su lugar");
            simularEnvio(event);
            return;
        }
        
        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest sendRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendRequest);

            log.info("Evento enviado a SQS - MessageId: {}, SolicitudId: {}",
                    response.messageId(), event.getSolicitudId());

        } catch (JsonProcessingException e) {
            log.error("Error al serializar evento: {}", event, e);
        } catch (Exception e) {
            log.error("Error al enviar mensaje a SQS: {}", event, e);
        }
    }

    private void simularEnvio(SolicitudCreadaEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("EVENTO_SIMULADO - SolicitudCreada: {}", eventJson);
            log.info("Evento SolicitudCreada simulado - ID: {}, Patente: {}",
                    event.getSolicitudId(), event.getPatente());
        } catch (JsonProcessingException e) {
            log.error("Error al serializar evento para simulación: {}", event, e);
        }
    }
}
