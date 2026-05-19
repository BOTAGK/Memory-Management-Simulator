# Memory-Management-Simulator

A Java-based OS simulator modeling virtual memory management, page replacement (LRU), and dynamic frame allocation strategies.

## Overview
This project simulates concurrent processes competing for physical memory frames. It generates memory access sequences based on the locality of reference principle and evaluates different frame allocation algorithms under heavy system load. A key focus is on detecting and handling system thrashing through dynamic process suspension.

## Features
* **Concurrent Execution Simulation:** Models multiple processes running in parallel with overlapping memory requests.
* **Thrashing Prevention:** Monitors page fault frequencies in sliding time windows. Automatically suspends and reactivates processes to prevent memory overload.
* **Locality of Reference:** Custom data generator creating realistic memory access patterns using dynamic locality subsets.
* **OOP Architecture:** Built with extensibility in mind using abstract classes and polymorphism for algorithm implementations.

## Implemented Algorithms
* **Allocation Strategies:** * Equal Allocation
  * Proportional Allocation
  * Working Set Size (WSS)
  * Page Fault Frequency (PFF)
* **Page Replacement:** LRU (Least Recently Used)

## Metrics Tracked
The simulator outputs the following data per run:
* Initial and final physical frame distribution.
* Total and per-process page fault counts.
* Thrashing incident counts.
* Number of process suspension events triggered by dynamic allocators.

## How to run
1. Clone the repository.
2. Compile the Java files.
3. Run `Main.java`. Simulation parameters (total frames, thresholds, process counts) can be adjusted directly within the `main` method.
