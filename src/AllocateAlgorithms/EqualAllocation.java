package AllocateAlgorithms;

import java.util.ArrayList;
import Algorithms.Algorithm;
import Algorithms.LRU;
import Process.Process;
import Process.Page;

public class EqualAllocation extends FrameAllocationAlgorithm {
    public EqualAllocation(int totalFrames, ArrayList<Process> processes, ArrayList<Page> globalReferences, int thrashingWindow_w, double thrashingThreshold_e_ratio) {
        super(totalFrames, processes, globalReferences, thrashingWindow_w, thrashingThreshold_e_ratio);
    }

    @Override
    public void allocateFrames() {
        int framesSizes = totalFrames / processes.size();
        for (Process process : processes) {
            getFramesSizes().put(process.getProcessID(), framesSizes);
            getProcessFrames().put(process.getProcessID(), new ArrayList<>());
        }
    }

    @Override
    public void simulate() {
        Algorithm lru = new LRU();
        ArrayList<Page> globalReferences = new ArrayList<>(this.globalReferences);

        while (!globalReferences.isEmpty()) {
            Page page = globalReferences.removeFirst();
            int processID = page.getProcessID();
            int frameSize = getFramesSizes().get(processID);
            
            lru.setCurrentPage(page);
            lru.setFrameSize(frameSize);
            lru.setFrame(getProcessFrames().get(processID));

            if(lru.runAlgorithm()) {
                handlePageFault(page);
                referenceFaultHistoryPerProcess.get(processID).add(true);
                pageFaultsInThrashingWindowPerProcess.merge(processID, 1, Integer::sum);
            } else {
                referenceFaultHistoryPerProcess.get(processID).add(false);
            }

            handleThrashing(page);
        }
    }
}