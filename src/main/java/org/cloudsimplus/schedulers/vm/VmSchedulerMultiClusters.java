/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudsimplus.schedulers.vm;

import lombok.Getter;
import lombok.NonNull;

import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.schedulers.MipsShare;
import org.cloudsimplus.vms.*;
import org.cloudsimplus.vms.VmOversubscribable;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicyFirstFit;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * VmSchedulerTimeShared is a Virtual Machine Monitor (VMM), also called Hypervisor,
 * that defines a policy to allocate one or more PEs from a PM to a VM, and allows sharing of PEs
 * by multiple VMs. <b>This class also implements 10% performance degradation due
 * to VM migration. It does not support over-subscription.</b>
 *
 * <p>Each host has to use is own instance of a VmScheduler that will so
 * schedule the allocation of host's PEs for VMs running on it.</p>
 *
 * <p>
 * It does not perform a preemption process in order to move running
 * VMs to the waiting list in order to make room for other already waiting
 * VMs to run. It just imposes there is not waiting VMs,
 * <b>oversimplifying</b> the scheduling, considering that for a given simulation
 * second <i>t</i>, the total processing capacity of the processor cores (in
 * MIPS) is equally divided by the VMs that are using them.
 * </p>
 *
 * <p>In processors enabled with <a href="https://en.wikipedia.org/wiki/Hyper-threading">Hyper-threading technology (HT)</a>,
 * it is possible to run up to 2 processes at the same physical CPU core.
 * However, this scheduler implementation
 * oversimplifies a possible HT feature by allowing several VMs to use a fraction of the MIPS capacity from
 * physical PEs, until that the total capacity of the virtual PE is allocated.
 * Consider that a virtual PE is requiring 1000 MIPS but there is no physical PE
 * with such a capacity. The scheduler will allocate these 1000 MIPS across several physical PEs,
 * for instance, by allocating 500 MIPS from PE 0, 300 from PE 1 and 200 from PE 2, totaling the 1000 MIPS required
 * by the virtual PE.
 * </p>
 *
 * <p>In a real hypervisor in a Host that has Hyper-threading CPU cores, two virtual PEs can be
 * allocated to the same physical PE, but a single virtual PE must be allocated to just one physical PE.</p>
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 1.0
 */
public class VmSchedulerMultiClusters extends VmSchedulerAbstract {

    protected Map<Float, List<VmOversubscribable>> consumerPerOversubscription;
    protected Map<Float, Long> resourceCountPerOversubscription;
    protected List<Pe> peList;
    protected Integer criticalSize;

    /**
     * Creates a time-shared VM scheduler.
     *
     */
    public VmSchedulerMultiClusters(final List<Pe> peList, final List<Float> oversubscriptionLevels) {
        this(peList, oversubscriptionLevels, DEF_VM_MIGRATION_CPU_OVERHEAD);
    }

    /**
     * Creates a time-shared VM scheduler, defining a CPU overhead for VM migration.
     *
     * @param vmMigrationCpuOverhead the percentage of Host's CPU usage increase when a
     * VM is migrating in or out of the Host. The value is in scale from 0 to 1 (where 1 is 100%).
     */
    public VmSchedulerMultiClusters(final List<Pe> peList, final List<Float> oversubscriptionLevels, final double vmMigrationCpuOverhead){
        super(vmMigrationCpuOverhead);
        this.peList = peList;
        this.consumerPerOversubscription = new HashMap<Float, List<VmOversubscribable>>();
        this.resourceCountPerOversubscription = new HashMap<Float, Long>();
        this.criticalSize = 1;
        for(Float oversubscription : oversubscriptionLevels){
            this.consumerPerOversubscription.put(oversubscription, new ArrayList<VmOversubscribable>());
            this.resourceCountPerOversubscription.put(oversubscription, new Long(0));
        }
    }

    @Override
    public boolean allocatePesForVmInternal(final Vm vm, final MipsShare requestedMips) {
        boolean success = allocateMipsShareForVmInternal(vm, requestedMips);
        if(success){
            Float oversubscriptionLevel = ((VmOversubscribable)vm).getOversubscriptionLevel();
            consumerPerOversubscription.get(oversubscriptionLevel).add((VmOversubscribable)vm);
            resourceCountPerOversubscription.put(oversubscriptionLevel, requestedMips.pes() + resourceCountPerOversubscription.get(oversubscriptionLevel));
        }
        return success;
    }

    /**
     * Try to allocate the MIPS requested by a VM
     * and update the allocated MIPS share.
     *
     * @param vm the VM
     * @param requestedMips the list of mips share requested by the vm
     * @return true if successful, false otherwise
     */
    private boolean allocateMipsShareForVmInternal(final Vm vm, final MipsShare requestedMips) {
        if (!isSuitableForVm(vm, requestedMips)) {
            return false;
        }

        allocateMipsShareForVm(vm, requestedMips);
        return true;
    }

    /**
     * Performs the allocation of a MIPS List to a given VM.
     * The actual MIPS to be allocated to the VM may be reduced
     * if the VM is in migration, due to migration overhead.
     *
     * @param vm the VM to allocate MIPS to
     * @param requestedMipsReduced the list of MIPS to allocate to the VM,
     * after it being adjusted by the {@link VmSchedulerAbstract#getMipsShareRequestedReduced(Vm, MipsShare)} method.
     * @see VmSchedulerAbstract#getMipsShareRequestedReduced(Vm, MipsShare)
     */
    protected void allocateMipsShareForVm(final Vm vm, final MipsShare requestedMipsReduced) {
        final var mipsShare = getMipsShareToAllocate(vm, requestedMipsReduced);
        //System.out.println("allocateMipsShareForVm " + mipsShare);
        ((VmOversubscribable)vm).setAllocatedMips(mipsShare);
    }

    private long getAvailableMipsFromHostPe(final Pe hostPe) {
        return hostPe.getPeProvisioner().getAvailableResource();
    }

    /**
     * The non-emptiness of the list is ensured by the {@link VmScheduler#isSuitableForVm(Vm, MipsShare)} method.
     */
    @Override
    protected boolean isSuitableForVmInternal(final Vm vm, final MipsShare requestedMips) {
        VmOversubscribable vmOversubscribable = (VmOversubscribable) vm;
        final double totalRequestedMips = requestedMips.totalMips();
        // Consult Oversubscription policy
        long possibleNewHostAllocation = getUsedResources(vmOversubscribable);
        //System.out.println("> #Debug id" + vmOversubscribable.getId() + " : " + vmOversubscribable.getOversubscriptionLevel() + " " + requestedMips.pes() + "vcpu" + " " + requestedMips.totalMips() + "mips");
        //System.out.println("> #Debug AvailableMips " + getTotalAvailableMips() + " requested mips" + totalRequestedMips);

        return possibleNewHostAllocation <= getHost().getWorkingPesNumber();
        //return getHost().getWorkingPesNumber() >= requestedMips.pes() && getTotalAvailableMips() >= totalRequestedMips;
    }

    /**
     * Gets the actual MIPS share that will be allocated to VM's PEs,
     * considering the VM migration status.
     * If the VM is in migration, this will cause overhead, reducing
     * the amount of MIPS allocated to the VM.
     *
     * @param vm the VM requesting allocation of MIPS
     * @param requestedMips the list of MIPS requested for each vPE
     * @return the allocated MIPS share to the VM
     */
    protected MipsShare getMipsShareToAllocate(final Vm vm, final MipsShare requestedMips) {
        return getMipsShareToAllocate(requestedMips, mipsPercentToRequest(vm));
    }

    /**
     * Gets the actual MIPS share that will be allocated to VM's PEs,
     * considering the VM migration status.
     * If the VM is in migration, this will cause overhead, reducing
     * the amount of MIPS allocated to the VM.
     *
     * @param requestedMips the list of MIPS requested for each vPE
     * @param scalingFactor the factor that will be used to reduce the amount of MIPS
     * allocated to each vPE (which is a percentage value between [0 .. 1]) in case the VM is in migration
     * @return the MIPS share allocated to the VM
     */
    protected MipsShare getMipsShareToAllocate(final MipsShare requestedMips, final double scalingFactor) {
        if(scalingFactor == 1){
            return requestedMips;
        }

        return new MipsShare(requestedMips.pes(), requestedMips.mips()*scalingFactor);
    }

    @Override
    protected long deallocatePesFromVmInternal(final Vm vm, final int pesToRemove) {
        VmOversubscribable vmOversubscribable = (VmOversubscribable) vm;
        // long removedPes = Math.max(
        //     removePesFromVm(vmOversubscribable, vmOversubscribable.getRequestedMips(), pesToRemove),
        //     removePesFromVm(vmOversubscribable, vmOversubscribable.getAllocatedMips(), pesToRemove));

        long beforePes = getUsedResources();
        Float oversubscriptionLevel = vmOversubscribable.getOversubscriptionLevel();
        consumerPerOversubscription.get(oversubscriptionLevel).remove(vmOversubscribable);
        resourceCountPerOversubscription.put(oversubscriptionLevel, resourceCountPerOversubscription.get(oversubscriptionLevel) - vmOversubscribable.getPesNumber());
        long afterPes = getUsedResources();

        long removedPes = afterPes - beforePes;
        //System.out.println("> #VM leaving id" + vmOversubscribable.getId() + " : " + vmOversubscribable.getOversubscriptionLevel() + " " + removedPes + "pes to remove");
        //System.out.println("> #VM host " + getHost().getWorkingPesNumber() + "/" + getHost().getPesNumber());
        return removedPes;
    }

    private long getUsedResources(){
        return getUsedResources(null);
    }

    private long getUsedResources(VmOversubscribable additionalVm){
        Long hostPesAllocation = new Long(0);
        for (Float oversubscriptionLevel : consumerPerOversubscription.keySet()) {
            Integer currentSize = consumerPerOversubscription.get(oversubscriptionLevel).size();
            Long hostPesAllocationForOversubscriptionLevel = resourceCountPerOversubscription.get(oversubscriptionLevel);
            if((additionalVm != null) && additionalVm.getOversubscriptionLevel().equals(oversubscriptionLevel)){
                currentSize+=1;
                hostPesAllocationForOversubscriptionLevel+=additionalVm.getPesNumber();
            }
            if(currentSize>= this.criticalSize){
                hostPesAllocationForOversubscriptionLevel = (long) Math.ceil(hostPesAllocationForOversubscriptionLevel/oversubscriptionLevel);
            }
            hostPesAllocation += hostPesAllocationForOversubscriptionLevel;
        }
        return hostPesAllocation;
    }

}
