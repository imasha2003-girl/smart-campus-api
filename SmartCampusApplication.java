package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 * Sets the base URI path for all REST endpoints to /api/v1
 *
 * Lifecycle Note: By default, JAX-RS resource classes are request-scoped —
 * a new instance is created per HTTP request. This means in-memory data structures
 * (like our HashMaps) must live in a shared singleton store, not in the resource class itself.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Jersey auto-discovers resources via package scanning configured in Main.java
}
