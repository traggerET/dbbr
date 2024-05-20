package generator;

import operation.Operation;
import util.Pair;

import java.util.List;

public class GFlipFlop implements IGenerator {
    private final List<IGenerator> gens;
    private final int i;

    public GFlipFlop(List<IGenerator> gens, int i) {
        this.gens = gens;
        this.i = i;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        Pair<Operation, IGenerator> opGen = gens.get(i).nextState(test, ctx);
        if (opGen != null) {
            return new Pair<>(opGen.getFirst(), new GFlipFlop(gens, (i + 1) % gens.size()));
        }
        return null;
    }

    @Override
    public GFlipFlop update(Test test, Context ctx, Operation event) {
        return this;
    }
}