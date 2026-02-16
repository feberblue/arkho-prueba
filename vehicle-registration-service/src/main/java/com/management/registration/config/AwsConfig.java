package com.management.registration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${aws.sqs.enabled:false}")
    private boolean sqsEnabled;

    /**
     * Bean para S3 Client
     */
    @Bean
    public S3Client s3Client() {
        if (!s3Enabled) {
            return null; // No crear cliente si S3 está deshabilitado
        }

        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Bean para S3 Presigner (generación de URLs prefirmadas)
     */
    @Bean
    public S3Presigner s3Presigner() {
        if (!s3Enabled) {
            // Crear un presigner mock para desarrollo local
            return S3Presigner.builder()
                    .region(Region.of(awsRegion))
                    .build();
        }

        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Bean para SQS Client
     */
    @Bean
    public SqsClient sqsClient() {
        if (!sqsEnabled) {
            return null; // No crear cliente si SQS está deshabilitado
        }

        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
