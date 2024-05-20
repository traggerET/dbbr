package generator;

import operation.Operation;
import util.Pair;

public class GRepeat implements IGenerator {
    private final long remaining;
    private final IGenerator gen;

    public GRepeat(long remaining, IGenerator gen) {
        this.remaining = remaining;
        this.gen = gen;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        if (remaining != 0) {
            Pair<Operation, IGenerator> result = gen.nextState(test, ctx);
            if (result != null) {
                return new Pair<>(result.getFirst(), new GRepeat(Math.max(-1, remaining - 1), gen));
            }
        }
        return null;
    }

    public GRepeat update(Test test, Context ctx, Operation operation) {
        return new GRepeat(remaining, gen.update(test, ctx, operation));
    }
}
