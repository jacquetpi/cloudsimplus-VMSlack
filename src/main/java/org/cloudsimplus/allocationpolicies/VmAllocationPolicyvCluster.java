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

    private boolean firstFit;

    public VmAllocationPolicyvCluster(){
        this.firstFit = false;
    }

    public VmAllocationPolicyvCluster(boolean firstFit){
        this.firstFit = firstFit;
    }

    /**
     * Gets a suitable host from the {@link #getHostList()}
     * Priority is given to the Availability (number of resources in vcluster available, without extending it)
     * If no availability is possible, we leverage the smallest vCluster to spread workloads homogeneously
     */
    @Override
    protected Optional<Host> defaultFindHostForVm(final Vm vm) {
        Float oversubscription = ((VmOversubscribable)vm).getOversubscriptionLevel();
        HostMultiClusters hostMaxCpuAvailability = null;
        HostMultiClusters hostMaxCpuSize = null;
        HostMultiClusters hostMaxProgress = null;
        
        System.out.println(">>New VM! Host selection for vm with " + vm.getPesNumber() + "vCPU" + " oc:" + ((VmOversubscribable)vm).getOversubscriptionLevel() + " mem " + vm.getCurrentRequestedRam() + "MB");
        for(Host host : getHostList()){
            
            HostMultiClusters hostMultiClusters = (HostMultiClusters) host;
            String memData = " mem " + (hostMultiClusters.getRam().getAllocatedResource()/1024) + "/" + (hostMultiClusters.getRam().getCapacity()/1024) + "GB";
            System.out.println(">>Host selection : candidate " + host.getId() + " state | cpu " + hostMultiClusters.debug((VmOversubscribable)vm) + "/" + hostMultiClusters.getPesNumber() + memData);

            if(!hostMultiClusters.isActive() || !hostMultiClusters.isSuitableForVm(vm)){
                continue;
            }

            long availability = hostMultiClusters.getAvailabilityFor(oversubscription);
            if(vm.getPesNumber() <= availability && (hostMaxCpuAvailability == null || availability > hostMaxCpuAvailability.getAvailabilityFor(oversubscription)))
                hostMaxCpuAvailability = hostMultiClusters;

            long size = hostMultiClusters.getSizeFor(oversubscription);
            if(hostMaxCpuSize == null || size > hostMaxCpuSize.getSizeFor(oversubscription))
                hostMaxCpuSize = hostMultiClusters;

            float progress = hostMultiClusters.getProgresstoToOptimalCpuMemRatio((VmOversubscribable)vm);
            if(hostMaxProgress == null || progress > hostMultiClusters.getProgresstoToOptimalCpuMemRatio((VmOversubscribable)vm))
                hostMaxProgress = hostMultiClusters;

            if(firstFit)
                break;
        }

        // if(hostMaxCpuAvailability != null){
        //     System.out.println(">>Host selection : Selection of " + hostMaxCpuAvailability.getId() + " for availability");
        //     return Optional.of(hostMaxCpuAvailability);
        // }

        // if(hostMaxCpuSize != null){
        //     System.out.println(">>Host selection : Selection of " + hostMaxCpuSize.getId() + " for max size");
        //     return Optional.of(hostMaxCpuSize);
        // }
        
        if(hostMaxProgress != null){
            System.out.println(">>Host selection : Selection of " + hostMaxProgress.getId() + " for progress to optimal ratio");
            return Optional.of(hostMaxProgress);
        }

        return Optional.empty();
    }

    public void debug(){
        for(Host host : getHostList()){
            HostMultiClusters hostMultiClusters = (HostMultiClusters) host;
            System.out.println(">>Wip " + hostMultiClusters.getId() + " " + hostMultiClusters.getCurrentCpuMemRatio() + "/" + hostMultiClusters.getIdealCpuMemRatio());
        }
    }



}
