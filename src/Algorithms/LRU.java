package Algorithms;

import Process.Page;
import java.util.Comparator;

public class LRU extends Algorithm {
    public LRU(int frameSize) {
        super(frameSize);
    }

    public LRU() {
        super();
    }

    @Override
    public boolean runAlgorithm() {
        boolean pageFault = false;
        setPageErrors(0);

        if (getFrame().size() < getFrameSize()) {
            if (!containsPage()) {
                getFrame().add(getCurrentPage());
                pageFault = true;
            }
        } else {
            if (!containsPage()) {
                getFrame().remove(pageToDelete());
                getFrame().add(getCurrentPage());
                pageFault = true;
            }
        }
        
        for (Page p : getFrame()) {
            if (p.getPageNumber() == getCurrentPage().getPageNumber() && p.getProcessID() == getCurrentPage().getProcessID()) {
                p.setSinceReference(0);
            }
            p.incrementSinceReference();
        }

        return pageFault;
    }

    private Page pageToDelete() {
        return getFrame().stream()
                .max(Comparator.comparingInt(Page::getSinceReference))
                .orElse(null);
    }
}