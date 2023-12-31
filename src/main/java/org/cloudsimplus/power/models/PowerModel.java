package org.cloudsimplus.power.models;

import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.power.PowerMeasurement;

/**
 * A common interface for implementing models of power consumption
 * for entities such as {@link Datacenter}s and {@link Host}s.
 *
 * A PowerModel computes the current power usage (in Watts) of an entity during the simulation.
 * @since CloudSim Plus 6.0.0
 */
public interface PowerModel {

    /**
     * Returns the entity's current power usage as a {@link PowerMeasurement},
     * which can hold additional information like static and dynamic fraction
     * of power usage.
     */
    PowerMeasurement getPowerMeasurement();

    /**
     * Returns the entity's current total power usage as a double value,
     * representing the Watts consumed.
     */
    default double getPower() {
        return getPowerMeasurement().getTotalPower();
    }
}

