package AllocateAlgorithms;

import Process.Page;
import Process.Process;
import java.util.*;

public abstract class FrameAllocationAlgorithm {
    protected int totalFrames;
    protected ArrayList<Process> processes;
    protected ArrayList<Page> globalReferences;
    protected Map<Integer, Integer> framesSizes;
    protected Map<Integer, Integer> processPageFaults;
    private Map<Integer, List<Page>> processFrames;
    protected int totalPageFaults;

    // Pola do zarządzania wstrzymywaniem procesów
    protected List<Process> activeProcesses;
    protected final LinkedList<Process> suspendedProcessesQueue;
    protected final Map<Integer, List<Page>> suspendedProcessReferences;
    protected final Map<Integer, Integer> lastFrameCountBeforeSuspension;
    protected final Map<Integer, Integer> remainingReferences;
    protected int totalProcessesSuspendedEvents;

    // Pola do zarządzania szamotaniem
    protected Map<Integer, Integer> thrashingIncidentsPerProcess;
    protected Map<Integer, Integer> pageFaultsInThrashingWindowPerProcess;
    protected Map<Integer, LinkedList<Boolean>> referenceFaultHistoryPerProcess;
    protected final int thrashingWindow_w;
    protected final double thrashingThreshold_e_ratio;

    public FrameAllocationAlgorithm(int totalFrames, ArrayList<Process> processes, ArrayList<Page> globalReferences, int thrashingWindow_w, double thrashingThreshold_e_ratio) {
        this.totalFrames = totalFrames;
        this.processes = processes;
        this.globalReferences = new ArrayList<>(globalReferences);
        this.framesSizes = new HashMap<>();
        this.processPageFaults = new HashMap<>();
        this.processFrames = new HashMap<>();
        this.totalPageFaults = 0;

        this.activeProcesses = new ArrayList<>(processes);
        this.suspendedProcessesQueue = new LinkedList<>();
        this.suspendedProcessReferences = new HashMap<>();
        this.lastFrameCountBeforeSuspension = new HashMap<>();
        this.remainingReferences = new HashMap<>();
        this.totalProcessesSuspendedEvents = 0;

        this.thrashingIncidentsPerProcess = new HashMap<>();
        this.pageFaultsInThrashingWindowPerProcess = new HashMap<>();
        this.referenceFaultHistoryPerProcess = new HashMap<>();
        this.thrashingWindow_w = thrashingWindow_w;
        this.thrashingThreshold_e_ratio = thrashingThreshold_e_ratio;

        initialize();
    }

    protected void initialize() {
        for (Process process : processes) {
            framesSizes.put(process.getProcessID(), 0);
            processPageFaults.put(process.getProcessID(), 0);
            referenceFaultHistoryPerProcess.put(process.getProcessID(), new LinkedList<>());
            remainingReferences.put(process.getProcessID(), process.getTotalReferences());
        }
    }

    protected Process getProcessById(int processId) {
        return processes.stream()
                .filter(p -> p.getProcessID() == processId)
                .findFirst()
                .orElse(null);
    }

    protected void suspendProcess(Process procToSuspend, LinkedList<Page> currentGlobalReferencesQueue) {
        if (!activeProcesses.contains(procToSuspend)) return;

        activeProcesses.remove(procToSuspend);
        suspendedProcessesQueue.add(procToSuspend);

        List<Page> remainingRefsForSuspended = new LinkedList<>();
        LinkedList<Page> nextGlobalReferences = new LinkedList<>();
        
        for(Page page : currentGlobalReferencesQueue) {
            if(page.getProcessID() == procToSuspend.getProcessID()){
                remainingRefsForSuspended.add(page);
            } else {
                nextGlobalReferences.add(page);
            }
        }
        
        currentGlobalReferencesQueue.clear();
        currentGlobalReferencesQueue.addAll(nextGlobalReferences);
        suspendedProcessReferences.put(procToSuspend.getProcessID(), remainingRefsForSuspended);

        int framesFreed = getFramesSizes().getOrDefault(procToSuspend.getProcessID(), 0);
        lastFrameCountBeforeSuspension.put(procToSuspend.getProcessID(), framesFreed);

        getFramesSizes().put(procToSuspend.getProcessID(), 0);
        if (getProcessFrames().containsKey(procToSuspend.getProcessID())) {
            getProcessFrames().get(procToSuspend.getProcessID()).clear();
        }

        setTotalProcessesSuspendedEvents(getTotalProcessesSuspendedEvents() + 1);
    }

    protected void tryReactivateProcess(LinkedList<Page> currentGlobalReferencesQueue) {
        if (suspendedProcessesQueue.isEmpty()) return;

        int totalAllocatedFrames = activeProcesses.stream()
                .mapToInt(p -> getFramesSizes().getOrDefault(p.getProcessID(), 0))
                .sum();
        int availableSystemFrames = this.totalFrames - totalAllocatedFrames;

        if (availableSystemFrames <= 0) return;

        Process procToReactivate = suspendedProcessesQueue.peekFirst();
        List<Page> refsToRestore = suspendedProcessReferences.getOrDefault(procToReactivate.getProcessID(), new LinkedList<>());

        int lastFrameCount = lastFrameCountBeforeSuspension.getOrDefault(procToReactivate.getProcessID(), 1);
        int desiredFrames = Math.max(1, lastFrameCount);
        int framesToAllocate = Math.min(desiredFrames, availableSystemFrames);

        if (framesToAllocate >= lastFrameCount && availableSystemFrames >= lastFrameCount) {
            procToReactivate = suspendedProcessesQueue.removeFirst();
            suspendedProcessReferences.remove(procToReactivate.getProcessID());

            getFramesSizes().put(procToReactivate.getProcessID(), framesToAllocate);
            activeProcesses.add(procToReactivate);
            getProcessFrames().get(procToReactivate.getProcessID()).clear();

            if (!refsToRestore.isEmpty()) {
                LinkedList<Page> nextGlobalReferences = new LinkedList<>();
                Random rand = new Random();
                
                while (!currentGlobalReferencesQueue.isEmpty() || !refsToRestore.isEmpty()) {
                    if (!currentGlobalReferencesQueue.isEmpty() && rand.nextDouble() < 0.7) {
                        nextGlobalReferences.add(currentGlobalReferencesQueue.removeFirst());
                    }
                    if (!refsToRestore.isEmpty() && rand.nextDouble() < 0.7) {
                        nextGlobalReferences.add(refsToRestore.removeFirst());
                    }
                }
                currentGlobalReferencesQueue.addAll(nextGlobalReferences);
            }
        }
    }

    protected void releaseFramesAndMarkAsFinished(Process process) {
        int framesReleased = getFramesSizes().getOrDefault(process.getProcessID(), 0);

        if (activeProcesses.contains(process)) {
            activeProcesses.remove(process);

            if (framesReleased > 0) {
                getFramesSizes().put(process.getProcessID(), 0);
                if (getProcessFrames().containsKey(process.getProcessID())) {
                    getProcessFrames().get(process.getProcessID()).clear();
                }
            }
        }
    }

    protected void handleThrashing(Page page) {
        LinkedList<Boolean> processHistory = referenceFaultHistoryPerProcess.get(page.getProcessID());
        
        if (processHistory.size() > thrashingWindow_w) {
            if (processHistory.removeFirst()) {
                pageFaultsInThrashingWindowPerProcess.merge(page.getProcessID(), -1, Integer::sum);
            }
        }

        if (processHistory.size() == thrashingWindow_w && thrashingWindow_w > 0) {
            double currentFaultRateInWindow = (double) pageFaultsInThrashingWindowPerProcess.get(page.getProcessID()) / thrashingWindow_w;
            
            if (currentFaultRateInWindow > thrashingThreshold_e_ratio) {
                thrashingIncidentsPerProcess.merge(page.getProcessID(), 1, Integer::sum);
            }

            pageFaultsInThrashingWindowPerProcess.put(page.getProcessID(), 0);
            referenceFaultHistoryPerProcess.put(page.getProcessID(), new LinkedList<>());
        }
    }

    protected void handlePageFault(Page page) {
        totalPageFaults++;
        processPageFaults.merge(page.getProcessID(), 1, Integer::sum);
    }

    public Map<Integer, Integer> getFramesSizes() { return framesSizes; }
    public void setFramesSizes(Map<Integer, Integer> framesSizes) { this.framesSizes = framesSizes; }
    
    public Map<Integer, Integer> getProcessPageFaults() { return processPageFaults; }
    public int getTotalPageFaults() { return totalPageFaults; }
    
    public Map<Integer, List<Page>> getProcessFrames() { return processFrames; }
    
    public int getTotalProcessesSuspendedEvents() { return totalProcessesSuspendedEvents; }
    public void setTotalProcessesSuspendedEvents(int events) { this.totalProcessesSuspendedEvents = events; }
    
    public Map<Integer, Integer> getThrashingIncidentsPerProcess() { return thrashingIncidentsPerProcess; }
    public int getTotalThrashingIncidents() {
        return thrashingIncidentsPerProcess.values().stream().mapToInt(Integer::intValue).sum();
    }

    public abstract void allocateFrames();
    public abstract void simulate();
}