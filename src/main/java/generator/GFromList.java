package generator;

import operation.Operation;
import util.Pair;

import java.util.ArrayList;
import java.util.List;

public class GFromList implements IGenerator {
    private final List<IGenerator> gens;
    public GFromList(List<IGenerator> gens) {
        this.gens = gens;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context context) {
        return op(gens, test, context);
    }

    private Pair<Operation, IGenerator> op(List<IGenerator> gens, Test test, Context context) {
        if (gens != null && !gens.isEmpty()) {
            IGenerator gen = gens.get(0);
            Pair<Operation, IGenerator> opResult = gen.nextState(test, context);
            if (opResult != null) {
                Operation op = opResult.getFirst();
                if (gens.size() > 1) {
                    ArrayList<IGenerator> arr = new ArrayList<>();
                    arr.add(opResult.getSecond());
                    arr.addAll(gens.subList(1, gens.size()));
                    return new Pair<>(op, new GFromList(arr));
                } else {
                    return new Pair<>(op, opResult.getSecond());
                }
            } else {
                if (gens.size() > 1) {
                    return op(gens.subList(1, gens.size()), test, context);
                }
            }
        }
        return null;
    }

    @Override
    public IGenerator update(Test test, Context context, Operation event) {
        if (gens != null && !gens.isEmpty()) {
            ArrayList<IGenerator> arr = new ArrayList<>();
            arr.add(gens.get(0).update(test, context, event));
            arr.addAll(gens.subList(1, gens.size()));
            return new GFromList(arr);
        }
        // close next time
        return new GFromOp(null);
    }
}
