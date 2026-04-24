package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 - API Request & Response Logging Filter
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter so a
 * single class handles cross-cutting logging for every inbound request and
 * every outbound response.
 *
 * Using JAX-RS filters for cross-cutting concerns (logging, auth, CORS) is
 * superior to manual Logger.info() in each resource method because:
 * - It avoids code duplication across every endpoint.
 * - Resources stay focused on business logic (Single Responsibility Principle).
 * - Filters can be applied/removed globally without touching resource classes.
 * - They execute in a consistent, predictable order in the JAX-RS pipeline.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info(String.format("--> REQUEST  | %s %s", method, uri));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        int status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info(String.format("<-- RESPONSE | %s %s | Status: %d", method, uri, status));
    }
}
