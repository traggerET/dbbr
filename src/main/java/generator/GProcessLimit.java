package generator;

import operation.Operation;
import util.Pair;

import java.util.HashSet;
import java.util.Set;

// TODO: remove this class? Current implementation seems useless
public class GProcessLimit implements IGenerator {
    private final Set<Integer> procs;
    private final IGenerator gen;
    private final int n;

    // It's important that this limitation will work only for contexts supporting no more than N processes in total!
    public GProcessLimit(int n, Set<Integer> procs, IGenerator gen) {
        this.n = n;
        this.procs = procs;
        this.gen = gen;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        Pair<Operation, IGenerator> result = gen.nextState(test, ctx);
        if (result == null) {
            return null;
        }
        Operation op = result.getFirst();
        IGenerator gen = result.getSecond();

        if (op.isPending()) {
            return new Pair<>(op, new GProcessLimit(n, procs, gen));
        } else {
            Set<Integer> procsNew = new HashSet<>(procs);
            procsNew.addAll(ctx.allProcesses());

            if (procsNew.size() <= n) {
                return new Pair<>(op, new GProcessLimit(n, procsNew, gen));
            }
        }
        return null;
    }

    @Override
    public IGenerator update(Test test, Context ctx, Operation event) {
        return new GProcessLimit(n, procs, gen.update(test, ctx, event));
    }
}
