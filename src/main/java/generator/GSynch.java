package generator;

import operation.Operation;
import util.Pair;

public class GSynch implements IGenerator {
    private final IGenerator gen;

    public GSynch(IGenerator gen) {
        this.gen = gen;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        if (ctx.freeThreadCount() == ctx.allThreadCount()) {
            return gen.nextState(test, ctx);
        } else {
            return new Pair<>(Operation.PENDING, this);
        }
    }

    public GSynch update(Test test, Context ctx, Operation op) {
        return new GSynch(gen.update(test, ctx, op));
    }
}
