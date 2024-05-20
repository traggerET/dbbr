package generator;

import operation.Operation;
import util.Pair;

import static java.lang.Long.max;

public class GDelay implements IGenerator {
    private final long dt;
    private final Long nextTime;
    private final IGenerator gen;

    public GDelay(long dt, Long nextTime, IGenerator gen) {
        this.dt = dt;
        this.nextTime = nextTime;
        this.gen = gen;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        Pair<Operation, IGenerator> opGen = gen.nextState(test, ctx);
        if (opGen == null) {
            return null;
        }
        Operation op = opGen.getFirst();
        if (op.isPending()) {
            return new Pair<>(op, new GDelay(dt, nextTime, opGen.getSecond()));
        }
        long newTime = nextTime != null ? nextTime : op.getTime();
        // TODO: COPY OR MODIFICATION ALLOWED?A
        op.setTime(max(newTime, op.getTime()));
        return new Pair<>(op, new GDelay(dt, dt + op.getTime(), opGen.getSecond()));
    }

    @Override
    public IGenerator update(Test test, Context ctx, Operation event) {
        return new GDelay(dt, nextTime, gen.update(test, ctx, event));
    }
}
