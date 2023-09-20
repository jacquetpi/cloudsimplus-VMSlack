/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudsimplus.vms;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.SimpleStorage;
import org.cloudsimplus.schedulers.cloudlet.CloudletScheduler;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;

/**
 * Implements the basic features of a Virtual Machine (VM), which runs inside a
 * {@link Host} that may be shared among other VMs. It processes
 * {@link Cloudlet cloudlets}. This processing happens according to a policy,
 * defined by the {@link CloudletScheduler}. Each VM has an owner (user), which
 * can submit cloudlets to the VM to execute them.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 1.0
 */
public class VmOversubscribable extends VmSimple {

    protected Float oversubscriptionLevel;

    public VmOversubscribable(final long id, final double mipsCapacity, final long pesNumber, final Float oversubscriptionLevel) {
        super(id, (long) mipsCapacity, pesNumber);
        this.oversubscriptionLevel = oversubscriptionLevel;
    }

    public VmOversubscribable(final double mipsCapacity, final long pesNumber, final Float oversubscriptionLevel) {
        super(mipsCapacity, pesNumber);
        this.oversubscriptionLevel = oversubscriptionLevel;
    }

    public Float getOversubscriptionLevel(){
        return this.oversubscriptionLevel;
    }

    public void setOversubscriptionLevel(final Float oversubscriptionLevel){
        this.oversubscriptionLevel = oversubscriptionLevel;
    }

}