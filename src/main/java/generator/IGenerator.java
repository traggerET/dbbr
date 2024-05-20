package generator;


import operation.Operation;
import util.Pair;

public interface IGenerator {
    Pair<Operation, IGenerator> nextState(Test test, Context context);
    IGenerator update(Test test, Context context, Operation event);
}
