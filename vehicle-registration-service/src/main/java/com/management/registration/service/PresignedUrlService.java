package com.management.registration.service;

import com.management.registration.dto.response.PresignedUrlResponse;
import com.management.registration.exception.S3ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class PresignedUrlService {


    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:fleet-documents}")
    private String bucketName;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    private static final Duration URL_EXPIRATION = Duration.ofMinutes(15);

    /**
     * Genera una URL prefirmada para subir un documento
     */
    public PresignedUrlResponse generarUrlParaSubida(UUID solicitudId, String tipoDocumento) {
        String fileKey = generarFileKey(solicitudId, tipoDocumento);

        if (s3Enabled) {
            return generarUrlReal(fileKey);
        } else {
            return generarUrlSimulada(fileKey);
        }
    }

    /**
     * Genera la URL prefirmada real usando AWS S3
     */
    private PresignedUrlResponse generarUrlReal(String fileKey) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType("application/pdf")
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(URL_EXPIRATION)
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

            log.info("URL prefirmada generada para: {}", fileKey);

            return PresignedUrlResponse.builder()
                    .uploadUrl(presignedRequest.url().toString())
                    .fileKey(fileKey)
                    .expiresAt(LocalDateTime.now().plus(URL_EXPIRATION))
                    .message("URL generada exitosamente. Válida por 15 minutos.")
                    .build();

        } catch (Exception e) {
            log.error("Error al generar URL prefirmada: {}", e.getMessage(), e);
            throw new S3ServiceException("Error al generar URL de carga", e);
        }
    }

    /**
     * Simula la generación de URL prefirmada (para desarrollo local)
     */
    private PresignedUrlResponse generarUrlSimulada(String fileKey) {
        String simulatedUrl = String.format(
                "https://%s.s3.amazonaws.com/%s?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=900",
                bucketName, fileKey
        );

        log.info("URL prefirmada SIMULADA generada para: {}", fileKey);

        return PresignedUrlResponse.builder()
                .uploadUrl(simulatedUrl)
                .fileKey(fileKey)
                .expiresAt(LocalDateTime.now().plus(URL_EXPIRATION))
                .message("URL simulada generada. En producción, esta será una URL real de S3.")
                .build();
    }

    /**
     * Genera la clave del archivo en S3
     */
    private String generarFileKey(UUID solicitudId, String tipoDocumento) {
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        return String.format("solicitudes/%s/%s_%s.pdf", solicitudId, tipoDocumento, timestamp);
    }
}
