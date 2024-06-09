package generator;

import operation.Operation;
import util.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GFabric {

    public static IGenerator synchronize(IGenerator gen) {
        return new GSynch(gen);
    }
    // TODO: rename class to factory/fabric

    public static IGenerator then(IGenerator ... gens) {
        ArrayList<IGenerator> arr = new ArrayList<>();
        for (int i = 0; i < gens.length; i++) {
            arr.add(gens[i]);
            if (i < gens.length - 1) {
                Operation op = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
                op.setInvokable((op1) -> {});
                arr.add(synchronize(fromOp(op)));
            }
        }
        return new GFromList(arr);
    }

    public static IGenerator phases(List<IGenerator> generators) {
        return new GFromList(generators.stream().map(GFabric::synchronize).collect(Collectors.toList()));
    }

    public static IGenerator limit(int remaining, IGenerator gen) {
        return new GLimit(remaining, gen);
    }

    public static IGenerator once(int remaining, IGenerator gen) {
        return new GLimit(1, gen);
    }

    public static IGenerator timeLimit(long dt, IGenerator gen) {
        return new GTimeLimit(TimeUnit.SECONDS.toNanos(dt), null, gen);
    }

    public static IGenerator stagger(long dt, IGenerator gen) {
        long newDt = 2 * dt * 1000000000; // Convert seconds to nanoseconds
        return new GStagger(newDt, null, gen);
    }

    public static IGenerator repeat(long limit, IGenerator gen) {
        assert limit >= 0;
        return new GRepeat(limit, gen);
    }

    public static IGenerator mix(List<IGenerator> gens) {
        if (!gens.isEmpty()) {
            return new GMix(new Random().nextInt(gens.size()), new ArrayList<>(gens));
        }
        return null;
    }

    public static IGenerator reserve(List<Pair<Integer, IGenerator>> gensWithThreads, IGenerator defGen) {
        List<Pair<Set<Integer>, IGenerator>> genArgs = new ArrayList<>();
        int n = 0;
        for (Pair<Integer, IGenerator> genWithThread : gensWithThreads) {
            int threadCount = genWithThread.getFirst();
            IGenerator gen = genWithThread.getSecond();
            n += threadCount;
            Set<Integer> genThreads = new HashSet<>();
            for (int j = n - threadCount; j < n; j++) {
                genThreads.add(j);
            }
            genArgs.add(new Pair<>(genThreads, gen));
        }

        List<Set<Integer>> ranges = genArgs.stream().map(Pair::getFirst).collect(Collectors.toList());
        Set<Integer> allRanges = ranges.stream().flatMap(Set::stream).collect(Collectors.toSet());
        List<Function<Context, Context>> contextFilters = new ArrayList<>(ranges.stream()
                .map(r -> Context.makeThreadFilter(r::contains))
                .toList());
        contextFilters.add(Context.makeThreadFilter(i -> !allRanges.contains(i)));

        List<IGenerator> gens = genArgs.stream().map(Pair::getSecond).collect(Collectors.toList());
        gens.add(defGen);

        return new GReserve(ranges, contextFilters, gens);
    }

    public static IGenerator flipFlop(List<IGenerator> gens) {
        return new GFlipFlop(gens, 0);
    }

    public static IGenerator cycleTimes(List<Pair<Integer, IGenerator>> gensWithTimes) {
        List<Long> intervals = new ArrayList<>();
        List<IGenerator> gens = new ArrayList<>();
        long period = 0;
        List<Long> cutoffs = new ArrayList<>();
        for (Pair<Integer, IGenerator> gensWithTime : gensWithTimes) {
            long t = (long) gensWithTime.getFirst();
            intervals.add(TimeUnit.SECONDS.toNanos(t));
            gens.add(gensWithTime.getSecond());
            period += t;
            cutoffs.add(period);
        }
        return new GCycleTimes(period, null, intervals, cutoffs, gens);
    }

    public static IGenerator untilOK(IGenerator gen) {
        return new GUntilOk(gen, false, new HashSet<>());
    }

    public static IGenerator cycle(IGenerator gen) {
        return new GCycle(-1, gen, gen);
    }

    public static IGenerator cycle(int limit, IGenerator gen) {
        return new GCycle(limit, gen, gen);
    }

    public static IGenerator onUpdate(Updatable f, IGenerator gen) {
        return new GOnUpdate(f, gen);
    }

    public static IGenerator filter(Predicate<Object> filter, IGenerator gen) {
        return new GFilter(filter, gen);
    }

    public static IGenerator map(Function<Operation, Operation> f, IGenerator gen) {
        return new GMap(f, gen);
    }

    public static IGenerator delay(double dt, IGenerator gen) {
        return new GDelay((long) (dt * 1e9), null, gen);
    }

    public static IGenerator processLimit(int n, IGenerator gen) {
        return new GProcessLimit(n, new HashSet<>(), gen);
    }

    public static IGenerator eachThread(IGenerator gen) {
        return new GEachThread(gen, null, new HashMap<>());
    }

    public static IGenerator fromFunc(Supplier<IGenerator> supplier) {
        return new GFromFunc(supplier);
    }

    public static IGenerator fromGens(IGenerator ... gens) {
        return new GFromList(Arrays.asList(gens));
    }

    public static IGenerator fromList(List<IGenerator> lst) {
        return new GFromList(lst);
    }

    public static IGenerator fromOp(Operation op) {
        return new GFromOp(op);
    }

    public static IGenerator sleep(long dt) {
        Operation sleepOp = new Operation(Operation.Type.SLEEP);
        sleepOp.setValue(dt);
        return new GFromOp(sleepOp);
    }

    public static IGenerator friendly(IGenerator generator) {
        return new GFriendlyExc(generator);
    }

    // TODO: replace maps with some precompiled type containing necessary fields...
    public static Map<String, Object> soonestOpMap(Map<String, Object> m1, Map<String, Object> m2) {
        if (m1 == null) {
            return m2;
        } else if (m2 == null) {
            return m1;
        } else {
            Operation op1 = (Operation) m1.get("op");
            Operation op2 = (Operation) m2.get("op");

            if (op1 == null) {
                return m2;
            } else if (op2 == null) {
                return m1;
            } else {
                int t1 = (int) op1.getTime();
                int t2 = (int) op2.getTime();

                if (t1 == t2) {
                    int w1 = (int) m1.get("weight");
                    int w2 = (int) m2.get("weight");
                    int w = w1 + w2;

                    if (new Random().nextInt(w) < w1) {
                        return m1;
                    } else {
                        return m2;
                    }
                } else {
                    if (t1 < t2) {
                        return m1;
                    } else {
                        return m2;
                    }
                }
            }
        }
    }

    public static Operation fillInOp(Operation op, Context ctx) {
        int p = ctx.someFreeProcess();

        if (p != -1) {
            Operation newOp = new Operation(op);
            newOp.setProcess(p);
            newOp.setTime(ctx.getTime());
            return newOp;
        } else {
            return Operation.PENDING;
        }
    }
}
