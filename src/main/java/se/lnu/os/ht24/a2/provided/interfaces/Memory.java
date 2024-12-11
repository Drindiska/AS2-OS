package se.lnu.os.ht24.a2.provided.interfaces;

import se.lnu.os.ht24.a2.provided.data.ProcessInterval;

import java.util.List;
import java.util.Set;

public interface Memory {
    boolean containsProcess(int processId);
    List<Integer> processes();
    int processSize(int processId);
    ProcessInterval getProcessInterval(int processId);
    Set<Integer> neighboringProcesses(int processId);
    double fragmentation();
    Set<ProcessInterval> freeSlots();
}
