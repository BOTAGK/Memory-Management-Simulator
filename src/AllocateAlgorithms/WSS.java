package AllocateAlgorithms;

import java.util.*;
import Algorithms.Algorithm;
import Algorithms.LRU;
import Process.Process;
import Process.Page;

public class WSS extends FrameAllocationAlgorithm {
    private final int deltaTime; 
    private final int c_wssCalculationInterval; 

    private Map<Integer, Integer> processWorkingSetSizes; 
    private Map<Integer, LinkedList<Page>> processReferencesInWindow; 
    private int globalTimeSinceLastWSSCheck; 

    public WSS(int totalFrames, ArrayList<Process> processes, ArrayList<Page> globalReferences,
               int deltaTime, int c_wssCalculationInterval, int thrashingWindow_w, double thrashingThreshold_e_ratio) {
        super(totalFrames, processes, globalReferences, thrashingWindow_w, thrashingThreshold_e_ratio);
        this.deltaTime = deltaTime;
        this.c_wssCalculationInterval = c_wssCalculationInterval;
        this.processWorkingSetSizes = new HashMap<>();
        this.processReferencesInWindow = new HashMap<>();
        this.activeProcesses = new ArrayList<>(processes);
        this.globalTimeSinceLastWSSCheck = 0;

        for (Process p : processes) {
            processReferencesInWindow.put(p.getProcessID(), new LinkedList<>());
            processWorkingSetSizes.put(p.getProcessID(), 1); 
            getProcessFrames().put(p.getProcessID(), new ArrayList<>()); 
        }
    }

    @Override
    public void allocateFrames() {
        if (processes.isEmpty()) return;

        ProportionalAllocation initialAlloc = new ProportionalAllocation(totalFrames, processes, globalReferences,
                thrashingWindow_w, thrashingThreshold_e_ratio);
        initialAlloc.allocateFrames();
        this.setFramesSizes(new HashMap<>(initialAlloc.getFramesSizes()));
    }

    @Override
    public void simulate() {
        Algorithm lru = new LRU();
        LinkedList<Page> currentGlobalReferences = new LinkedList<>(this.globalReferences);

        while (!currentGlobalReferences.isEmpty() || !suspendedProcessesQueue.isEmpty()) {
            
            if (!suspendedProcessesQueue.isEmpty()) {
                int totalAllocatedFramesBeforeReactivation = activeProcesses.stream()
                        .mapToInt(p -> getFramesSizes().getOrDefault(p.getProcessID(), 0))
                        .sum();
                if (this.totalFrames - totalAllocatedFramesBeforeReactivation > 0) {
                    tryReactivateProcess(currentGlobalReferences);
                }
            }

            if (!currentGlobalReferences.isEmpty()) {
                Page currentPage = currentGlobalReferences.removeFirst();
                Process currentProcess = getProcessById(currentPage.getProcessID());

                if (currentProcess == null || !activeProcesses.contains(currentProcess) || suspendedProcessesQueue.contains(currentProcess)) {
                    continue;
                }

                globalTimeSinceLastWSSCheck++;
                processReferencesInWindow.get(currentPage.getProcessID()).add(currentPage);
                remainingReferences.merge(currentProcess.getProcessID(), -1, Integer::sum);

                lru.setCurrentPage(currentPage);
                lru.setFrameSize(getFramesSizes().get(currentProcess.getProcessID()));
                lru.setFrame(getProcessFrames().get(currentProcess.getProcessID()));

                boolean pageFaultOccurred;
                if (getFramesSizes().get(currentProcess.getProcessID()) > 0) {
                    pageFaultOccurred = lru.runAlgorithm();
                } else {
                    pageFaultOccurred = true; 
                }

                if (pageFaultOccurred) {
                    handlePageFault(currentPage);
                    referenceFaultHistoryPerProcess.get(currentProcess.getProcessID()).add(true);
                    pageFaultsInThrashingWindowPerProcess.merge(currentProcess.getProcessID(), 1, Integer::sum);
                } else {
                    referenceFaultHistoryPerProcess.get(currentProcess.getProcessID()).add(false);
                }

                if (remainingReferences.get(currentProcess.getProcessID()) <= 0) {
                    releaseFramesAndMarkAsFinished(currentProcess);
                    remainingReferences.remove(currentProcess.getProcessID());
                    processWorkingSetSizes.remove(currentProcess.getProcessID());
                }

                handleThrashing(currentPage);
            }

            if (globalTimeSinceLastWSSCheck >= deltaTime) {
                calculateAndAdjustWSS(currentGlobalReferences);

                for (Process p : this.processes) {
                    processReferencesInWindow.put(p.getProcessID(), new LinkedList<>());
                }
                globalTimeSinceLastWSSCheck = 0; 
            }
        }
    }

    private void calculateAndAdjustWSS(LinkedList<Page> currentGlobalReferences) {
        for (Process p : activeProcesses) {
            LinkedList<Page> refsInWindow = processReferencesInWindow.get(p.getProcessID());
            long wss = refsInWindow.stream().map(Page::getPageNumber).distinct().count();
            processWorkingSetSizes.put(p.getProcessID(), (int) Math.max(1, wss));
        }

        int totalWSSNeeded = activeProcesses.stream()
                .mapToInt(p -> processWorkingSetSizes.get(p.getProcessID()))
                .sum();

        if (totalWSSNeeded <= totalFrames) {
            for (Process p : activeProcesses) {
                int wss_i = processWorkingSetSizes.get(p.getProcessID());
                getFramesSizes().put(p.getProcessID(), wss_i);
            }
        } else {
            Process procToSuspend = activeProcesses.stream()
                    .min(Comparator.comparingInt((Process p) -> processWorkingSetSizes.get(p.getProcessID()))
                            .thenComparingInt(Process::getProcessID)) 
                    .orElse(null);

            if (procToSuspend != null && activeProcesses.size() > 1) {
                suspendProcess(procToSuspend, currentGlobalReferences);
                processWorkingSetSizes.put(procToSuspend.getProcessID(), 0);
                calculateAndAdjustWSS(currentGlobalReferences);
            } else if (procToSuspend != null && activeProcesses.size() == 1) {
                getFramesSizes().put(procToSuspend.getProcessID(), totalFrames);
            }
        }
    }
}