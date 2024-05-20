package generator;

import operation.Operation;

public interface Updatable {
    IGenerator update(IGenerator gen, Test test, Context ctx, Operation event);
}
