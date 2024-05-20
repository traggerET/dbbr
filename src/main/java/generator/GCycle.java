package generator;

import operation.Operation;
import util.Pair;

public class GCycle implements  IGenerator {
    private final int remaining;
    private final IGenerator originalGen;
    private final IGenerator gen;

    public GCycle(int remaining, IGenerator originalGen, IGenerator gen) {
        this.remaining = remaining;
        this.originalGen = originalGen;
        this.gen = gen;
    }
    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context context) {
        if (remaining != 0) {
            Pair<Operation, IGenerator> result = gen.nextState(test, context);
            if (result != null) {
                return new Pair<>(result.getFirst(), new GCycle(remaining - 1, originalGen, result.getSecond()));
            } else {
                return new GCycle(remaining - 1, originalGen, originalGen).nextState(test, context);
            }
        }
        return null;
    }

    @Override
    public IGenerator update(Test test, Context context, Operation event) {
        return new GCycle(remaining, originalGen, gen.update(test, context, event));
    }
}
