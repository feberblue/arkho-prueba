package com.management.registration.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignedUrlResponse {

    //Url del fichecho
    private String uploadUrl;

    //key del fichero
    private String fileKey;

    // fecha de expiracion
    private LocalDateTime expiresAt;

    //Mensaje
    private String message;
}

