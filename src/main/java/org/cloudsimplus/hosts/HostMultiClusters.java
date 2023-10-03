/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudsimplus.hosts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.cloudsimplus.core.*;
import org.cloudsimplus.resources.*;
import org.cloudsimplus.schedulers.MipsShare;
import org.cloudsimplus.vms.*;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.provisioners.ResourceProvisioner;
import org.cloudsimplus.provisioners.ResourceProvisionerSimple;
import org.cloudsimplus.resources.HarddriveStorage;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.schedulers.vm.VmScheduler;
import org.cloudsimplus.schedulers.vm.VmSchedulerMultiClusters;
import org.cloudsimplus.vms.VmOversubscribable;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A Host class that implements the most basic features of a Physical Machine
 * (PM) inside a {@link Datacenter}. It executes actions related to management
 * of virtual machines (e.g., creation and destruction). A host has a defined
 * policy for provisioning memory and bw, as well as an allocation policy for
 * PEs to {@link Vm Virtual Machines}. A host is associated to a Datacenter and
 * can host virtual machines.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 1.0
 */
public class HostMultiClusters extends HostSimple {

    /**
     * Creates and powers on a Host without a pre-defined ID.
     * It uses a {@link ResourceProvisionerSimple}
     * for RAM and Bandwidth and also sets a {@link VmSchedulerSpaceShared} as default.
     * The ID is automatically set when a List of Hosts is attached
     * to a {@link Datacenter}.
     *
     * @param ram the RAM capacity in Megabytes
     * @param bw the Bandwidth (BW) capacity in Megabits/s
     * @param storage the storage capacity in Megabytes
     * @param peList the host's {@link Pe} list
     *
     * @see HostSimple#HostSimple(long, long, HarddriveStorage, List)
     * @see ChangeableId#setId(long)
     * @see #setRamProvisioner(ResourceProvisioner)
     * @see #setBwProvisioner(ResourceProvisioner)
     * @see #setVmScheduler(VmScheduler)
     */
    public HostMultiClusters(final long ram, final long bw, final long storage, final List<Pe> peList, final List<Float> oversubscriptionLevels) {
        super(ram, bw, storage, peList);
        setVmScheduler(new VmSchedulerMultiClusters(peList, oversubscriptionLevels));
    }

    @Override
    protected HostSuitability isSuitableForVm(final Vm vm, final boolean inMigration, final boolean showFailureLog) {
        final var suitability = new HostSuitability(this, vm);
        suitability.setForStorage(true);
        suitability.setForRam(true);
        suitability.setForBw(true);

        //suitability.setForStorage(disk.isAmountAvailable(vm.getStorage()));
        // if (!suitability.forStorage()) {
        //     logAllocationError(showFailureLog, vm, inMigration, "MB", this.getStorage(), vm.getStorage());
        //     if (lazySuitabilityEvaluation)
        //         return suitability;
        // }

        // suitability.setForRam(ramProvisioner.isSuitableForVm(vm, vm.getRam()));
        // if (!suitability.forRam()) {
        //     logAllocationError(showFailureLog, vm, inMigration, "MB", this.getRam(), vm.getRam());
        //     if (lazySuitabilityEvaluation)
        //         return suitability;
        // }

        // suitability.setForBw(bwProvisioner.isSuitableForVm(vm, vm.getBw()));
        // if (!suitability.forBw()) {
        //     logAllocationError(showFailureLog, vm, inMigration, "Mbps", this.getBw(), vm.getBw());
        //     if (lazySuitabilityEvaluation)
        //         return suitability;
        // }

        suitability.setForPes(vmScheduler.isSuitableForVm(vm));
        return suitability;
    }

    /* getAvailabilityFor(oversubscriptionLevel)
    *  Availability is defined as the number of resources in vcluster available, without having to extend it
    */
    public long getAvailabilityFor(Float oversubscription){
        return ((VmSchedulerMultiClusters)vmScheduler).getAvailabilityFor(oversubscription);
    }

    /* getSizeFor(oversubscriptionLevel)
    *  Allocation size of oversubscriptionLevel
    */
    public long getSizeFor(Float oversubscription){
        return ((VmSchedulerMultiClusters)vmScheduler).getSizeFor(oversubscription);
    }

    public long debug(VmOversubscribable additionalVm){
        return ((VmSchedulerMultiClusters)vmScheduler).debug(additionalVm);
    }

    @Override
    public String toString() {
        final char dist = datacenter.getCharacteristics().getDistribution().symbol();
        final String dc =
                datacenter == null || Datacenter.NULL.equals(datacenter) ? "" :
                "/%cDC %d".formatted(dist, datacenter.getId());
        return "Host %d%s".formatted(getId(), dc);
    }

    @Override
    public int compareTo(final Host other) {
        if(this.equals(requireNonNull(other))) {
            return 0;
        }

        return Long.compare(this.id, other.getId());
    }
}
