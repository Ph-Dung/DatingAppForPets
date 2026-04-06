package com.petmatch.backend.config;

/**
 * Thrown when a requested entity is not found in the database.
 * Maps to HTTP 404 Not Found via GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entity, Long id) {
        super(entity + " not found with id: " + id);
    }
}
