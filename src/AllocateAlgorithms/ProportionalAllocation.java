package AllocateAlgorithms;

import java.util.ArrayList;
import Algorithms.Algorithm;
import Algorithms.LRU;
import Process.Process;
import Process.Page;

public class ProportionalAllocation extends FrameAllocationAlgorithm {

    public ProportionalAllocation(int totalFrames, ArrayList<Process> processes, ArrayList<Page> globalReferences, int thrashingWindow_w, double thrashingThreshold_e_ratio) {
        super(totalFrames, processes, globalReferences, thrashingWindow_w, thrashingThreshold_e_ratio);
    }

    @Override
    public void allocateFrames() {
        int remainingFrames = totalFrames;
        int totalUsedPages = processes.stream().mapToInt(Process::getTotalPages).sum();

        for (int i = 0; i < processes.size(); i++) {
            Process process = processes.get(i);
            double proportion = (double) process.getTotalPages() / totalUsedPages;
            int framesSizes = (int) Math.round(proportion * totalFrames);

            if (i == processes.size() - 1) {
                framesSizes = remainingFrames;
            } else {
                remainingFrames -= framesSizes;
            }

            getFramesSizes().put(process.getProcessID(), Math.max(framesSizes, 1));
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