package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1.2 - Discovery / HATEOAS root endpoint.
 * GET /api/v1 returns API metadata and navigable resource links.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        response.put("contact", Map.of(
            "name", "Campus Infrastructure Team",
            "email", "admin@smartcampus.ac.uk"
        ));

        // HATEOAS: resource links allow clients to navigate without hardcoding paths
        Map<String, String> links = new LinkedHashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("resources", links);

        return Response.ok(response).build();
    }
}
