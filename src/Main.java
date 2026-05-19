import java.util.ArrayList;
import Process.Process;
import Simulation.Simulation;

public class Main {
    public static void main(String[] args) {

        int totalFrames = 40;          
        int totalReferences = 1000;    
        int minPages = 100;            
        int maxPages = 400;            
        int localityWindow = 30;       
        int maxLocalitySize = 40;      
        int numProcesses = 4;          

        double lowerThreshold = 0.1;                   
        double upperThreshold = 0.3;                   
        int deltaTime = 50;                            
        double highThreshold_h = 0.9;                  
        boolean useHighThresholdForSuspension = true;  
        int thrashingWindow_w = 10;                    
        double thrashingThreshold_e_ratio = 0.5;       

        ArrayList<Process> processes = new ArrayList<>();
        for (int i = 0; i < numProcesses; i++) {
            processes.add(new Process(i, totalReferences, minPages, maxPages, localityWindow, maxLocalitySize));
        }

        System.out.println();
        int totalPageReferences = 0;
        
        for (Process process : processes) {
            System.out.println("Proces " + process.getProcessID() + " ma " + process.getTotalPages() + " różnych stron.");
            totalPageReferences += process.getPageReferences().size();
        }

        System.out.println("\nCałkowita liczba odwołań do stron: " + totalPageReferences);
        System.out.println("\nCałkowita liczba ramek (stron fizycznych): " + totalFrames);
        System.out.println("\n--------- Symulacja Algorytmów ------------------");

        Simulation simulation = new Simulation(totalFrames, processes, lowerThreshold, upperThreshold, deltaTime,
                highThreshold_h, useHighThresholdForSuspension, thrashingWindow_w, thrashingThreshold_e_ratio);
        simulation.runSimulation();
    }
}