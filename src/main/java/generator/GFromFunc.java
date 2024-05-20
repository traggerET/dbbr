package generator;

import operation.Operation;
import util.Pair;

import java.util.function.Supplier;

public class GFromFunc implements IGenerator {

    private final Supplier<IGenerator> supplier;

    public GFromFunc(Supplier<IGenerator> supplier) {
        this.supplier = supplier;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context context) {
        return supplier.get().nextState(test, context);
    }

    @Override
    public IGenerator update(Test test, Context context, Operation event) {
        return this;
    }
}
