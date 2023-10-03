/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2021 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.allocationpolicies;

import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostMultiClusters;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmOversubscribable;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class VmAllocationPolicyvCluster extends VmAllocationPolicyAbstract {

    /**
     * Gets a suitable host from the {@link #getHostList()}
     * Priority is given to the Availability (number of resources in vcluster available, without extending it)
     * If no availability is possible, we leverage the smallest vCluster to spread workloads homogeneously
     */
    @Override
    protected Optional<Host> defaultFindHostForVm(final Vm vm) {
        Float oversubscription = ((VmOversubscribable)vm).getOversubscriptionLevel();
        HostMultiClusters hostMaxAvailability = null;
        HostMultiClusters hostMaxSize = null;
        
        System.out.println(">>Host selection for vm with " + vm.getPesNumber() + "vCPU" + " oc:" + ((VmOversubscribable)vm).getOversubscriptionLevel());
        for(Host host : getHostList()){

            HostMultiClusters hostMultiClusters = (HostMultiClusters) host;
            if(!hostMultiClusters.isActive() || !hostMultiClusters.isSuitableForVm(vm)){
                System.out.println(">>Host selection : " + host.getId() + " is full or inactive " + hostMultiClusters.debug((VmOversubscribable)vm));
                continue;
            }

            long availability = hostMultiClusters.getAvailabilityFor(oversubscription);
            if(vm.getPesNumber() <= availability && (hostMaxAvailability == null || availability > hostMaxAvailability.getAvailabilityFor(oversubscription)))
                hostMaxAvailability = hostMultiClusters;

            long size = hostMultiClusters.getSizeFor(oversubscription);
            if(hostMaxSize == null || size > hostMaxSize.getSizeFor(oversubscription))
                hostMaxSize = hostMultiClusters;

            System.out.println(">>Host selection : " + host.getId() + " available:" + availability + " size:" + size);
        }

        if(hostMaxAvailability != null){
            System.out.println(">>Host selection : Selection of " + hostMaxAvailability.getId() + " for availability");
            return Optional.of(hostMaxAvailability);
        }
        if(hostMaxSize != null){
            System.out.println(">>Host selection : Selection of " + hostMaxSize.getId() + " for min size");
            return Optional.of(hostMaxSize);
        }
        return Optional.empty();
    }

}
