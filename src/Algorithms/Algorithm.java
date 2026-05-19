package Algorithms;

import Process.Page;
import java.util.ArrayList;
import java.util.List;

public abstract class Algorithm {
    private int frameSize;
    private List<Page> frame;
    private Page currentPage;
    private int pageErrors;

    public Algorithm(int frameSize) {
        this.frameSize = frameSize;
        this.frame = new ArrayList<>();
        this.currentPage = null;
        this.pageErrors = 0;
    }

    public Algorithm() {
        this(0);
    }

    public abstract boolean runAlgorithm();

    public boolean containsPage() {
        return frame.stream().anyMatch(page -> 
            page.getPageNumber() == currentPage.getPageNumber() && 
            page.getProcessID() == currentPage.getProcessID()
        );
    }

    public void incrementPageErrors() { pageErrors++; }
    public Page getCurrentPage() { return currentPage; }
    public void setCurrentPage(Page currentPage) { this.currentPage = currentPage; }
    
    public int getFrameSize() { return frameSize; }
    public void setFrameSize(int frameSize) { this.frameSize = frameSize; }
    
    public List<Page> getFrame() { return frame; }
    public void setFrame(List<Page> frame) { this.frame = frame; }
    
    public int getPageErrors() { return pageErrors; }
    public void setPageErrors(int pageErrors) { this.pageErrors = pageErrors; }
}