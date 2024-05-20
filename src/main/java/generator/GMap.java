package generator;

import operation.Operation;
import util.Pair;

import java.util.function.Function;

public class GMap implements IGenerator {
    private final Function<Operation, Operation> f;
    private final IGenerator gen;

    public GMap(Function<Operation, Operation> f, IGenerator gen) {
        this.f = f;
        this.gen = gen;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        Pair<Operation, IGenerator> opGen = gen.nextState(test, ctx);
        if (opGen != null) {
            Operation oper = opGen.getFirst();
            return new Pair<>(oper.isPending() ? oper : f.apply(oper), new GMap(f, opGen.getSecond()));
        }
        return null;
    }

    public GMap update(Test test, Context ctx, Operation event) {
        return new GMap(f, gen.update(test, ctx, event));
    }
}
