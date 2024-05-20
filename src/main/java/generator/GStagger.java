package generator;

import operation.Operation;
import util.Pair;

import java.util.concurrent.ThreadLocalRandom;

public class GStagger implements IGenerator {
    private final long dt;
    private final Long nextTime;
    private final IGenerator gen;

    public GStagger(long dt, Long nextTime, IGenerator gen) {
        this.dt = dt;
        this.nextTime = nextTime;
        this.gen = gen;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        Pair<Operation, IGenerator> opGen = gen.nextState(test, ctx);
        if (opGen != null) {
            long now = ctx.getTime();
            long nextTime = (this.nextTime != null) ? this.nextTime : now;
            Operation op = opGen.getFirst();
            if (op.isPending()) {
                return new Pair<>(op, this);
            } else if (nextTime <= op.getTime()) {
                return new Pair<>(op, new GStagger(dt, op.getTime() + ThreadLocalRandom.current().nextLong(dt), opGen.getSecond()));
            } else {
                op.setTime(nextTime);
                return new Pair<>(op, new GStagger(dt, nextTime + ThreadLocalRandom.current().nextLong(dt), opGen.getSecond()));
            }
        }
        return null;
    }

    @Override
    public GStagger update(Test test, Context ctx, Operation event) {
        return new GStagger(dt, nextTime, gen.update(test, ctx, event));
    }
}
