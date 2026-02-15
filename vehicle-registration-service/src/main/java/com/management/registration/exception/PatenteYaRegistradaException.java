package com.management.registration.exception;

public class PatenteYaRegistradaException extends RuntimeException{

    private final String patente;

    public PatenteYaRegistradaException(String patente) {
        super(String.format("La patente '%s' ya se encuentra registrada en el sistema", patente));
        this.patente = patente;
    }

    public String getPatente() {
        return patente;
    }
}
