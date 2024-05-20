package generator;

import java.util.Collection;
import java.util.Set;

public interface IContext {
    Set<Integer> allThreads();
    int allThreadCount();
    int freeThreadCount();
    Set<Integer> allProcesses();
    int processToThread(int process);
    int threadToProcess(int thread);
    boolean isThreadFree(int thread);
    Set<Integer> freeThreads();
    Collection<Integer> freeProcesses();
    int someFreeProcess();
    Context busyThread(long time, int thread);
    Context freeThread(long time, int thread);
    Context withNextProcess(int thread);
}
