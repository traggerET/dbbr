package generator;

import operation.Operation;
import util.Pair;

import java.util.function.Predicate;

public class GFilter implements IGenerator {
    private final Predicate<Object> filter;
    private final IGenerator gen;

    public GFilter(Predicate<Object> filter, IGenerator gen) {
        this.filter = filter;
        this.gen = gen;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        IGenerator currentGen = gen;
        while (true) {
            Pair<Operation, IGenerator> result = currentGen.nextState(test, ctx);
            if (result != null) {
                Operation op = result.getFirst();
                currentGen = result.getSecond();
                if (op.isPending() || filter.test(op)) {
                    return new Pair<>(op, new GFilter(filter, currentGen));
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public IGenerator update(Test test, Context ctx, Operation event) {
        return new GFilter(filter, gen.update(test, ctx, event));
    }
}