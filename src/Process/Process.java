package Process;

import java.util.ArrayList;
import java.util.Random;

public class Process {
    private int processID;
    private ArrayList<Page> pageReferences;
    private int localityWindow;
    private int maxLocalitySize;
    private int totalPages;
    private int localityStart;
    private int totalReferences;
    private int maxPages;
    private int minPages;
    private Random rand = new Random();

    public Process(int processID, int totalReferences, int minPages, int maxPages,
                   int localityWindow, int maxLocalitySize) {
        this.processID = processID;
        this.localityWindow = localityWindow;
        this.maxLocalitySize = maxLocalitySize;
        this.maxPages = maxPages;
        this.minPages = minPages;
        this.totalPages = minPages + rand.nextInt(maxPages - minPages + 1);
        this.localityStart = processID * maxPages;
        this.totalReferences = totalReferences;
        this.pageReferences = generateReferences();
    }

    private ArrayList<Page> generateReferences() {
        ArrayList<Page> references = new ArrayList<>();
        int localitySize = rand.nextInt(maxLocalitySize) + 1;
        
        for (int i = 0; i < totalReferences; i++) {
            if (i != 0 && i % localityWindow == 0) {
                localityStart += localitySize;
                localitySize = rand.nextInt(maxLocalitySize) + 1;
                if (localityStart + localitySize > (maxPages * processID) + totalPages) {
                    localityStart = processID * maxPages;
                }
            }
            int currentPage = localityStart + rand.nextInt(maxLocalitySize) + 1;
            references.add(new Page(currentPage, processID));
        }
        return references;
    }

    public ArrayList<Page> getPageReferences() { return pageReferences; }
    public int getProcessID() { return processID; }
    public int getTotalReferences() { return totalReferences; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}