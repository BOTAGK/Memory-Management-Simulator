package Simulation;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import AllocateAlgorithms.*;
import Process.Process;
import Process.Page;

public class Simulation {
    private int totalFrames;
    private ArrayList<Process> processes;
    private ArrayList<Page> globalReferences;
    private ArrayList<FrameAllocationAlgorithm> algorithms;

    double lowerThreshold;
    double upperThreshold;
    int deltaTime;
    double highThreshold_h;
    boolean useHighThresholdForSuspension;
    int thrashingWindow_w;
    double thrashingThreshold_e_ratio;

    public Simulation(int totalFrames, ArrayList<Process> processes, double lowerThreshold, double upperThreshold, int deltaTime,
                      double highThreshold_h, boolean useHighThresholdForSuspension,
                      int thrashingWindow_w, double thrashingThreshold_e_ratio) {
        this.totalFrames = totalFrames;
        this.processes = processes;
        this.globalReferences = generateGlobalReferences(processes);
        this.algorithms = new ArrayList<>();
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        this.deltaTime = deltaTime;
        this.highThreshold_h = highThreshold_h;
        this.useHighThresholdForSuspension = useHighThresholdForSuspension;
        this.thrashingWindow_w = thrashingWindow_w;
        this.thrashingThreshold_e_ratio = thrashingThreshold_e_ratio;

        initializeAlgorithms();
    }

    private void initializeAlgorithms() {
        algorithms.add(new EqualAllocation(totalFrames, processes, globalReferences, thrashingWindow_w, thrashingThreshold_e_ratio));
        algorithms.add(new ProportionalAllocation(totalFrames, processes, globalReferences, thrashingWindow_w, thrashingThreshold_e_ratio));
        // Uwaga: Klasa PageFaultFrequency musi być dostępna w Twoim projekcie, nie dołączyłeś jej do snippetu, więc zakładam, że istnieje.
        algorithms.add(new PageFaultFrequency(totalFrames, processes, globalReferences, lowerThreshold, upperThreshold, deltaTime,
                highThreshold_h, useHighThresholdForSuspension, thrashingWindow_w, thrashingThreshold_e_ratio));
        algorithms.add(new WSS(totalFrames, processes, globalReferences, deltaTime, deltaTime/2, thrashingWindow_w, thrashingThreshold_e_ratio));
    }

    public void runSimulation() {
        for (FrameAllocationAlgorithm algorithm : algorithms) {
            algorithm.allocateFrames();

            System.out.println("\nAlgorytm: " + algorithm.getClass().getSimpleName());
            Map<Integer, Integer> algorithmBefore = new java.util.HashMap<>(algorithm.getFramesSizes());
            
            algorithm.simulate();
            
            System.out.println("Całkowita liczba błędów stron: " + algorithm.getTotalPageFaults());
            System.out.println("Całkowita liczba szamotań: " + algorithm.getTotalThrashingIncidents());
            System.out.println("Rozdzielenie ramek na starcie: " + algorithmBefore);
            System.out.println("Rozdzielenie ramek na końcu: " + algorithm.getFramesSizes());
            System.out.println("Błędy stron na proces: " + algorithm.getProcessPageFaults());
            System.out.println("Ilość szamotań na proces: " + algorithm.getThrashingIncidentsPerProcess());

            if (algorithm.getClass() == PageFaultFrequency.class || algorithm.getClass() == WSS.class) {
                System.out.println("Całkowita liczba zastopowanych procesów: " + algorithm.getTotalProcessesSuspendedEvents());
            }
            System.out.println("*****************************************************");
        }
    }

    public static ArrayList<Page> generateGlobalReferences(ArrayList<Process> processes) {
        ArrayList<Page> globalReferences = new ArrayList<>();
        ArrayList<ArrayList<Page>> processReferences = new ArrayList<>();
        Random rand = new Random();

        for (Process process : processes) {
            processReferences.add(new ArrayList<>(process.getPageReferences()));
        }

        boolean allEmpty;
        do {
            allEmpty = true;
            for (int i = 0; i < processReferences.size(); i++) {
                if (!processReferences.get(i).isEmpty()) {
                    allEmpty = false;
                    if (rand.nextDouble() < 0.7) { 
                        globalReferences.add(processReferences.get(i).removeFirst());
                    }
                }
            }
        } while (!allEmpty);

        return globalReferences;
    }
}