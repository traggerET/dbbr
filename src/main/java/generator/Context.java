package generator;

import javax.sound.midi.Soundbank;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Context implements IContext {

    private long time;
    private int nextThreadIndex;
    private int threadCount;
    private BitSet allThreads;
    private BitSet freeThreads;
    private List<Integer> threadToProcess;
    private Map<Integer, Integer> processToThread;

    public Context(long time, int nextThreadIndex, int threadCount, BitSet allThreads, BitSet freeThreads, List<Integer> threadToProcess, Map<Integer, Integer> processToThread) {
        this.time = time;
        this.nextThreadIndex = nextThreadIndex;
        this.threadCount = threadCount;
        this.allThreads = allThreads;
        this.freeThreads = freeThreads;
        this.threadToProcess = threadToProcess;
        this.processToThread = processToThread;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long t) {
        time = t;
    }

    @Override
    public Set<Integer> allThreads() {
        return indicesToNames(allThreads);
    }

    @Override
    public int allThreadCount() {
        return allThreads.cardinality();
    }

    @Override
    public int freeThreadCount() {
        return freeThreads.cardinality();
    }

    @Override
    public Set<Integer> allProcesses() {
        return allThreads().stream()
                .map(this::threadToProcess)
                .collect(Collectors.toSet());
    }

    @Override
    public int processToThread(int process) {
        return processToThread.getOrDefault(process, -1);
    }

    @Override
    public int threadToProcess(int thread) {
        return threadToProcess.get(thread);
    }

    @Override
    public Set<Integer> freeThreads() {
        return indicesToNames(freeThreads);
    }

    @Override
    public Collection<Integer> freeProcesses() {
        return freeThreads().stream()
                .map(this::threadToProcess)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isThreadFree(int thread) {
        return freeThreads.get(thread);
    }

    @Override
    public int someFreeProcess() {
        int i = freeThreads.nextSetBit(nextThreadIndex);
        if (i >= 0) {
            return threadToProcess.get(i);
        } else if (nextThreadIndex == 0) {
            return -1;
        } else {
            i = freeThreads.nextSetBit(0);
            return i == -1 ? -1 : threadToProcess.get(i);
        }
    }

    @Override
    public Context freeThread(long time, int thread) {
        BitSet freeThreads = (BitSet) this.freeThreads.clone();
        freeThreads.set(thread);
        return new Context(time, nextThreadIndex, threadCount, allThreads, freeThreads, threadToProcess, processToThread);
    }

    @Override
    public Context busyThread(long time, int thread) {
        BitSet freeThreads = (BitSet) this.freeThreads.clone();
        freeThreads.clear(thread);
        return new Context(time, (nextThreadIndex + 1) % threadCount, threadCount, allThreads, freeThreads, threadToProcess, processToThread);
    }

    @Override
    public Context withNextProcess(int thread) {
        // TODO: why using process at all?
        int process = threadToProcess(thread);
        int process1 = threadCount + process;
        List<Integer> threadToProcess1 = new ArrayList<>(threadToProcess);
        threadToProcess1.set(thread, process1);
        Map<Integer, Integer> processToThread1 = new HashMap<>(processToThread);
        processToThread1.remove(process);
        processToThread1.put(process1, thread);
        return new Context(time, nextThreadIndex, threadCount, allThreads, freeThreads, threadToProcess1, processToThread1);
    }

    public static Context createContext(Test test) {
        int threadCount = test.concurrency;

        BitSet allThreadsBitset = new BitSet(threadCount);
        allThreadsBitset.set(0, threadCount);

        List<Integer> threadIndexToProcess = new ArrayList<>();
        Map<Integer, Integer> processToThread = new HashMap<>();
        for (int i = 0; i < threadCount; i += 1) {
            processToThread.put(i, i);
            threadIndexToProcess.add(i);
        }

        return new Context(0, 0, threadCount, allThreadsBitset, allThreadsBitset, threadIndexToProcess, processToThread);
    }

    public static Function<Context, Context> makeThreadFilter(Predicate<Integer> pred, Context ctx) {
        BitSet bs = (BitSet) ctx.allThreads.clone();

        for (int i = 0; i >= 0; i = bs.nextSetBit(i + 1)) {
            if (!pred.test(i)) {
                bs.clear(i);
            }
        }

        return (context) -> new Context(context.getTime(), context.nextThreadIndex, context.threadCount,
                intersectBitsets(bs, context.allThreads), intersectBitsets(bs, context.freeThreads),
                context.threadToProcess, context.processToThread);
    }

    public static Function<Context, Context> makeThreadFilter(Predicate<Integer> pred) {
        return new Function<>() {
            Function<Context, Context> threadFilter = null;
            @Override
            public Context apply(Context ctx) {
                if (threadFilter != null) {
                    return threadFilter.apply(ctx);
                } else {
                    Function<Context, Context> tf = makeThreadFilter(pred, ctx);
                    threadFilter = tf;
                    return tf.apply(ctx);
                }
            }
        };
    }

    private static BitSet intersectBitsets(BitSet bitset1, BitSet bitset2) {
        BitSet result = (BitSet) bitset1.clone();
        result.and(bitset2);
        return result;
    }

    private Set<Integer> indicesToNames(BitSet indices) {
        Set<Integer> names = new HashSet<>();
        for (int i = indices.nextSetBit(0); i >= 0; i = indices.nextSetBit(i + 1)) {
            names.add(i);
        }
        return names;
    }
}
