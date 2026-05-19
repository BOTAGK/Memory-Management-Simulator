package Process;

public class Page {
    private int pageNumber;
    private boolean secondChance;
    private int sinceReference;
    private int processID;

    public Page(int pageNumber, int processID) {
        this.pageNumber = pageNumber;
        this.secondChance = true;
        this.sinceReference = 0;
        this.processID = processID;
    }

    public int getProcessID() { return processID; }
    public void setProcessID(int processID) { this.processID = processID; }
    
    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    
    public boolean isSecondChance() { return secondChance; }
    public void setSecondChance(boolean secondChance) { this.secondChance = secondChance; }
    
    public int getSinceReference() { return sinceReference; }
    public void setSinceReference(int sinceReference) { this.sinceReference = sinceReference; }
    public void incrementSinceReference() { this.sinceReference++; }

    @Override
    public String toString() {
        return "Page: " + pageNumber;
    }
}