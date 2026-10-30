package com.enterprise.mongodb.web.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(String.format("%s with identifier '%s' was not found", resourceName, identifier), "RESOURCE_NOT_FOUND");
    }
}
