package generator;

import operation.Operation;
import util.Pair;

import java.util.ArrayList;
import java.util.List;

public class GCycleTimes implements IGenerator {
    private final long period; // Total period of the cycle, in nanos
    private final Long t0; // Starting time, in nanos (initially null)
    private final List<Long> intervals; // Array of durations in nanos that each generator should last for
    private final List<Long> cutoffs; // Array of times in nanos below which that particular generator starts. Omits the last generator
    private final List<IGenerator> gens; // Array of generators

    public GCycleTimes(long period, Long t0, List<Long> intervals, List<Long> cutoffs, List<IGenerator> gens) {
        this.period = period;
        this.t0 = t0;
        this.intervals = intervals;
        this.cutoffs = cutoffs;
        this.gens = gens;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        long now = ctx.getTime();
        long t0 = this.t0 != null ? this.t0 : ctx.getTime();
        long inPeriod = (now - t0) % period;
        long cycleStart = now - inPeriod;
        int i = 0;
        while (i != cutoffs.size() && inPeriod >= cutoffs.get(i)) {
            i++;
        }

        int j = i;
        long t = cycleStart;
        for (int k = 0; k < i; k++) {
            t += this.intervals.get(k);
        }

        while (true) {
            IGenerator gen = gens.get(j);
            long interval = intervals.get(j);
            long tPrime = t + interval;
            ctx.setTime(Math.max(now, t));
            Pair<Operation, IGenerator> opGen = gen.nextState(test, ctx);
            if (opGen == null) {
                return null;
            } else if (opGen.getFirst().isPending()) {
                List<IGenerator> newGens = new ArrayList<>(gens);
                newGens.set(j, gen);
                return new Pair<>(Operation.PENDING, new GCycleTimes(period, t0, intervals, cutoffs, newGens));
            } else if ((opGen.getFirst()).getTime() < tPrime) {
                List<IGenerator> newGens = new ArrayList<>(gens);
                newGens.set(j, gen);
                return new Pair<>(opGen.getFirst(), new GCycleTimes(period, t0, intervals, cutoffs, newGens));
            } else {
                j = (j + 1) % gens.size();
                t = tPrime;
            }
        }
    }

    @Override
    public GCycleTimes update(Test test, Context ctx, Operation event) {
        return new GCycleTimes(period, t0, intervals, cutoffs, gens.stream().map(gen -> gen.update(test, ctx, event)).toList());
    }
}
