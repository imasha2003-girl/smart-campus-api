package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {
    private final String resourceType;
    private final String resourceId;

    public LinkedResourceNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " with ID '" + resourceId + "' not found.");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
}
