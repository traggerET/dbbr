package generator;

import operation.Operation;
import util.Pair;

public class GFriendlyExc implements IGenerator {
    private final IGenerator gen;

    public GFriendlyExc(IGenerator gen) {
        this.gen = gen;
    }

    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        try {
            Pair<Operation, IGenerator> opGen = gen.nextState(test, ctx);
            if (opGen != null) {
                return new Pair<>(opGen.getFirst(), new GFriendlyExc(opGen.getSecond()));
            }
        } catch (Throwable t) {
            throw new RuntimeException("Generator threw " + t.getClass() + "-" + t.getMessage() + " when asked for an operation. Generator:\n" + gen + "\nContext:\n" + ctx);
        }
        return null;
    }

    public IGenerator update(Test test, Context ctx, Operation ope) {
        try {
            return new GFriendlyExc(gen.update(test, ctx, ope));
        } catch (Throwable t) {
            throw new RuntimeException("Generator threw " + t + " when updated with an opertaion update. Generator:\n" + gen + "\nContext:\n" + ctx + "\nOperation:\n" + ope);
        }
    }
}