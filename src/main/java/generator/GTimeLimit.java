package generator;

import operation.Operation;
import util.Pair;

public class GTimeLimit implements IGenerator {
    private final long limit;
    private final Long cutoff;
    private final IGenerator gen;
    public GTimeLimit(long limit, Long cutoff, IGenerator gen) {
        this.limit = limit;
        this.cutoff = cutoff;
        this.gen = gen;
    }
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        Pair<Operation, IGenerator> result = gen.nextState(test, ctx);
        if (result != null) {
            Operation operation = result.getFirst();
            if (operation.isPending()) {
                return new Pair<>(operation, new GTimeLimit(limit, cutoff, result.getSecond()));
            } else {
                long newCutoff = cutoff != null ? cutoff : operation.getTime() + limit;
                if (operation.getTime() < newCutoff) {
                    return new Pair<>(operation, new GTimeLimit(limit, newCutoff, result.getSecond()));
                }
            }
        }
        return null;
    }

    public IGenerator update(Test test, Context ctx, Operation event) {
        return new GTimeLimit(limit, cutoff, gen.update(test, ctx, event));
    }
}
