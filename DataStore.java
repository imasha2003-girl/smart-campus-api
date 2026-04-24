package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store.
 *
 * Because JAX-RS resource classes are request-scoped (new instance per request),
 * shared state must live outside them. This singleton uses ConcurrentHashMap
 * to ensure thread-safety when multiple requests access data simultaneously,
 * preventing race conditions and data corruption.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // Thread-safe maps for all entities
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private void seedData() {
        // Seed some rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Lab A", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        // Seed some sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 450.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LAB-101");

        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        // Link sensors to rooms
        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());

        // Seed some readings
        sensorReadings.put(s1.getId(), new ArrayList<>(List.of(
            new SensorReading(21.0),
            new SensorReading(22.5)
        )));
        sensorReadings.put(s2.getId(), new ArrayList<>(List.of(
            new SensorReading(440.0)
        )));
        sensorReadings.put(s3.getId(), new ArrayList<>());
    }

    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, Sensor> getSensors() { return sensors; }
    public Map<String, List<SensorReading>> getSensorReadings() { return sensorReadings; }
}
