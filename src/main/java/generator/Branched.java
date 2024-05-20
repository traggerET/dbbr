package generator;

import operation.Operation;

public interface Branched {
    Boolean test(Test test, Context ctx, Operation op);
}
