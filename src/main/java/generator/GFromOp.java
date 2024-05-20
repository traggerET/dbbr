package generator;

import operation.Operation;
import util.Pair;

public class GFromOp implements IGenerator {
    private final Operation operation;

    public GFromOp(Operation operation) {
        this.operation = operation;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context context) {
        if (operation == null) {
            return null;
        }
        Operation op = GFabric.fillInOp(operation, context);
                                                        // close next time
        return new Pair<>(op, op.isPending() ? this : new GFromOp(null));
    }

    @Override
    public IGenerator update(Test test, Context context, Operation event) {
        return this;
    }
}
