package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.1 - 409 Conflict: Room has sensors assigned.
 */
@Provider
class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "status", 409,
                    "error", "Conflict",
                    "message", "Room '" + ex.getRoomId() + "' cannot be deleted. It still has "
                               + ex.getSensorCount() + " sensor(s) assigned. Decommission all sensors first."
                ))
                .build();
    }
}

/**
 * Part 5.2 - 422 Unprocessable Entity: Referenced roomId does not exist.
 *
 * 422 is more accurate than 404 here because the request itself is valid HTTP,
 * but the payload contains a reference (roomId) that points to a non-existent resource.
 * 404 means the endpoint itself was not found; 422 communicates a semantic data error.
 */
@Provider
class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "status", 422,
                    "error", "Unprocessable Entity",
                    "message", "Referenced " + ex.getResourceType() + " with ID '"
                               + ex.getResourceId() + "' does not exist. Ensure the linked resource is created first."
                ))
                .build();
    }
}

/**
 * Part 5.3 - 403 Forbidden: Sensor is under MAINTENANCE.
 */
@Provider
class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException ex) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "status", 403,
                    "error", "Forbidden",
                    "message", "Sensor '" + ex.getSensorId() + "' is currently under MAINTENANCE. "
                               + "It is physically disconnected and cannot accept new readings."
                ))
                .build();
    }
}

/**
 * Part 5.4 - 500 Global Safety Net.
 * Catches ALL unexpected Throwables so raw stack traces are NEVER exposed.
 *
 * Security rationale: Stack traces reveal internal class names, library versions,
 * file paths, and logic flow — all useful for attackers performing reconnaissance.
 */
@Provider
class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper", ex);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "status", 500,
                    "error", "Internal Server Error",
                    "message", "An unexpected error occurred. Please contact the API administrator."
                ))
                .build();
    }
}
