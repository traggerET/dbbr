package generator;

import operation.Operation;
import util.Pair;

public class GOnUpdate implements IGenerator {
    private final Updatable f;
    private final IGenerator gen;

    public GOnUpdate(Updatable f, IGenerator gen) {
        this.f = f;
        this.gen = gen;
    }

    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        Pair<Operation, IGenerator> result = gen.nextState(test, ctx);
        if (result != null) {
            return new Pair<>(result.getFirst(), new GOnUpdate(f, result.getSecond()));
        }
        return null;
    }
    public IGenerator update(Test test, Context ctx, Operation event) {
        return f.update(this, test, ctx, event);
    }
}
