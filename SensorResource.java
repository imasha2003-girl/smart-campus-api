package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 3 - Sensor Operations
 * Manages /api/v1/sensors
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/sensors
     * Returns all sensors. Supports optional ?type= filter.
     *
     * @QueryParam is used for filtering because query parameters are optional
     * and don't affect the resource identity — the collection /sensors always
     * refers to the same resource; the type param just narrows the view.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(store.getSensors().values());
        if (type != null && !type.isBlank()) {
            result.removeIf(s -> !s.getType().equalsIgnoreCase(type));
        }
        return Response.ok(result).build();
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor. Validates that the referenced roomId exists.
     *
     * @Consumes(APPLICATION_JSON): If a client sends text/plain or application/xml,
     * JAX-RS returns HTTP 415 Unsupported Media Type automatically, before the method is called.
     */
    @POST
    public Response registerSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor ID is required."))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Validate foreign key: roomId must exist
        String roomId = sensor.getRoomId();
        if (roomId == null || !store.getRooms().containsKey(roomId)) {
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        // Default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);
        store.getSensorReadings().put(sensor.getId(), new ArrayList<>());

        // Link sensor to the room
        Room room = store.getRooms().get(roomId);
        room.getSensorIds().add(sensor.getId());

        return Response
                .created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(sensor)
                .build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found: " + sensorId))
                    .build();
        }
        // Unlink from room
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }
        store.getSensors().remove(sensorId);
        store.getSensorReadings().remove(sensorId);

        return Response.ok(Map.of("message", "Sensor '" + sensorId + "' deleted.")).build();
    }

    /**
     * Part 4.1 - Sub-Resource Locator Pattern
     * Delegates /api/v1/sensors/{sensorId}/readings to SensorReadingResource.
     *
     * This pattern keeps the SensorResource clean. Rather than handling every nested
     * path here, we delegate to a dedicated class. The JAX-RS runtime injects context
     * and resolves the sub-resource at request time.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found: " + sensorId))
                    .build()
            );
        }
        return new SensorReadingResource(sensorId);
    }
}
