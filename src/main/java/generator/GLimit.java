package generator;

import operation.Operation;
import util.Pair;

public class GLimit implements IGenerator {

    private final int remaining;
    private final IGenerator gen;

    public GLimit(int remaining, IGenerator gen) {
        this.remaining = remaining;
        this.gen = gen;
    }

    @Override
    public IGenerator update(Test test, Context ctx, Operation event) {
        return new GLimit(remaining, gen.update(test, ctx, event));
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        if (remaining > 0) {
            Pair<Operation, IGenerator> result = gen.nextState(test, ctx);
            if (result != null) {
                return new Pair<>(result.getFirst(), new GLimit(remaining - 1, result.getSecond()));
            }
        }
        return null;
    }
}
