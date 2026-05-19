package AllocateAlgorithms;

import java.util.*;
import Algorithms.Algorithm;
import Algorithms.LRU;
import Process.Process;
import Process.Page;

public class PageFaultFrequency extends FrameAllocationAlgorithm {
    private final double lowerThreshold; 
    private final double upperThreshold; 
    private final double highThreshold_h; 
    private final int deltaTime; 
    private final boolean useHighThresholdForSuspension;

    private final Map<Integer, Integer> pageFaultsInDeltaTForProcess; 
    private final Map<Integer, Integer> referencesProcessedInDeltaTForProcess; 
    private int globalTimeTickSinceLastPpfCheck; 

    public PageFaultFrequency(int totalFrames, ArrayList<Process> processes, ArrayList<Page> globalReferences,
                              double lowerThreshold, double upperThreshold, int deltaTime,
                              double highThreshold_h, boolean useHighThresholdForSuspension,
                              int thrashingWindow_w, double thrashingThreshold_e_ratio) {
        super(totalFrames, processes, globalReferences, thrashingWindow_w, thrashingThreshold_e_ratio);
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        this.deltaTime = deltaTime; 
        this.highThreshold_h = highThreshold_h;
        this.useHighThresholdForSuspension = useHighThresholdForSuspension;

        this.pageFaultsInDeltaTForProcess = new HashMap<>();
        this.referencesProcessedInDeltaTForProcess = new HashMap<>();
        this.globalTimeTickSinceLastPpfCheck = 0;

        for (Process p : processes) {
            pageFaultsInDeltaTForProcess.put(p.getProcessID(), 0);
            referencesProcessedInDeltaTForProcess.put(p.getProcessID(), 0);
            getProcessFrames().put(p.getProcessID(), new ArrayList<>());
        }
    }

    @Override
    public void allocateFrames() {
        if (this.processes.isEmpty()) return;

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
            
            // Faza 1: Próba wznowienia procesów, jeśli są wolne ramki
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

                globalTimeTickSinceLastPpfCheck++;
                referencesProcessedInDeltaTForProcess.merge(currentProcess.getProcessID(), 1, Integer::sum);
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
                    pageFaultsInDeltaTForProcess.merge(currentProcess.getProcessID(), 1, Integer::sum);
                    referenceFaultHistoryPerProcess.get(currentProcess.getProcessID()).add(true);
                    pageFaultsInThrashingWindowPerProcess.merge(currentProcess.getProcessID(), 1, Integer::sum);
                } else {
                    referenceFaultHistoryPerProcess.get(currentProcess.getProcessID()).add(false);
                }

                if (remainingReferences.get(currentProcess.getProcessID()) <= 0) {
                    releaseFramesAndMarkAsFinished(currentProcess);
                    remainingReferences.remove(currentProcess.getProcessID());
                }

                handleThrashing(currentPage);
            }

            // Faza 2: Ocena PPF i dostosowanie ramek (wywoływane co deltaTime)
            if (globalTimeTickSinceLastPpfCheck >= deltaTime) {
                for (Process proc : new ArrayList<>(activeProcesses)) {
                    if (!getFramesSizes().containsKey(proc.getProcessID())) continue;

                    int faultsInWindow = pageFaultsInDeltaTForProcess.getOrDefault(proc.getProcessID(), 0);
                    int refsProcessedInWindow = referencesProcessedInDeltaTForProcess.getOrDefault(proc.getProcessID(), 0);

                    double ppf = 0.0;
                    if (refsProcessedInWindow > 0) {
                        ppf = (double) faultsInWindow / refsProcessedInWindow; 
                    }

                    int currentProcFrames = getFramesSizes().get(proc.getProcessID());
                    int totalAllocatedFrames = activeProcesses.stream()
                            .mapToInt(p -> getFramesSizes().getOrDefault(p.getProcessID(), 0))
                            .sum();
                    int availableSystemFrames = this.totalFrames - totalAllocatedFrames;

                    if (ppf > upperThreshold) { 
                        if (availableSystemFrames > 0) {
                            getFramesSizes().put(proc.getProcessID(), currentProcFrames + 1);
                        } else { 
                            boolean suspendNow = !useHighThresholdForSuspension || (ppf > highThreshold_h);
                            if (suspendNow) {
                                suspendProcess(proc, currentGlobalReferences);
                                
                                pageFaultsInDeltaTForProcess.put(proc.getProcessID(), 0);
                                referencesProcessedInDeltaTForProcess.put(proc.getProcessID(), 0);
                            }
                        }
                    } 
                    else if (refsProcessedInWindow > 0 && ppf < lowerThreshold) { 
                        if (currentProcFrames > 1) { 
                            getFramesSizes().put(proc.getProcessID(), currentProcFrames - 1);
                        }
                    }
                }

                // Reset liczników dla następnego okna Δt
                for (Process p : this.processes) { 
                    pageFaultsInDeltaTForProcess.put(p.getProcessID(), 0);
                    referencesProcessedInDeltaTForProcess.put(p.getProcessID(), 0);
                }
                globalTimeTickSinceLastPpfCheck = 0; 
            }
        }
    }

    public Map<Integer, LinkedList<Boolean>> getReferenceFaultHistoryPerProcess() {
        return referenceFaultHistoryPerProcess;
    }

    public Map<Integer, Integer> getPageFaultsInThrashingWindowPerProcess() {
        return pageFaultsInThrashingWindowPerProcess;
    }
}