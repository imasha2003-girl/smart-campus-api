package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * Part 4.2 - Historical Data Management Sub-Resource
 * Handles /api/v1/sensors/{sensorId}/readings
 *
 * This class is instantiated by SensorResource's sub-resource locator.
 * It focuses solely on reading history for a specific sensor, keeping
 * responsibilities separated and the codebase maintainable.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full reading history for the sensor.
     */
    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getSensorReadings().getOrDefault(sensorId, List.of());
        return Response.ok(readings).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading and updates the parent sensor's currentValue.
     *
     * State Constraint: Sensors with status "MAINTENANCE" cannot accept new readings.
     * They are physically disconnected — throwing SensorUnavailableException returns 403 Forbidden.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        // Part 5.3 - Block readings for sensors under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Reading body is required."))
                    .build();
        }

        // Assign ID and timestamp if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(java.util.UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Append to history
        store.getSensorReadings()
             .computeIfAbsent(sensorId, k -> new java.util.ArrayList<>())
             .add(reading);

        // Side effect: update parent sensor's currentValue for data consistency
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings/{readingId}
     * Returns a specific reading by ID.
     */
    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        List<SensorReading> readings = store.getSensorReadings().getOrDefault(sensorId, List.of());
        return readings.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Reading not found: " + readingId))
                        .build());
    }
}
