package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Part 2 - Room Management
 * Manages /api/v1/rooms
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/rooms
     * Returns all rooms.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(rooms).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room ID is required."))
                    .build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        store.getRooms().put(room.getId(), room);
        return Response
                .created(URI.create("/api/v1/rooms/" + room.getId()))
                .entity(room)
                .build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns a specific room by ID.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Deletes a room. Blocked if sensors are still assigned.
     *
     * Idempotency: DELETE is idempotent — deleting a non-existent room still returns
     * a successful 2xx response (or 404), and re-sending the same request does not
     * change server state. In this implementation we return 404 on the second call
     * since the room no longer exists, which is the most transparent behaviour.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found: " + roomId))
                    .build();
        }
        if (!room.getSensorIds().isEmpty()) {
            // Business constraint: cannot delete a room with active sensors
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }
        store.getRooms().remove(roomId);
        return Response.ok(Map.of("message", "Room '" + roomId + "' has been deleted.")).build();
    }
}
