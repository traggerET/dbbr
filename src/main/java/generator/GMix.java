package generator;

import operation.Operation;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GMix implements IGenerator {
    private final int i;
    private final List<IGenerator> gens;

    public GMix(int i, List<IGenerator> gens) {
        this.i = i;
        this.gens = gens;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        if (!gens.isEmpty()) {
            Pair<Operation, IGenerator> opGen = gens.get(i).nextState(test, ctx);
            if (opGen != null) {
                return new Pair<>(opGen.getFirst(), new GMix(new Random().nextInt(gens.size()), updateList(gens, i, opGen.getSecond())));
            } else {
                return new GMix(new Random().nextInt(gens.size() - 1), removeGenerator(gens, i)).nextState(test, ctx);
            }
        }
        return null;
    }

    @Override
    public IGenerator update(Test test, Context ctx, Operation operation) {
        return this;
    }

    private List<IGenerator> updateList(List<IGenerator> list, int index, IGenerator gen) {
        List<IGenerator> updatedList = new ArrayList<>(list);
        updatedList.set(index, gen);
        return updatedList;
    }

    private List<IGenerator> removeGenerator(List<IGenerator> list, int index) {
        List<IGenerator> updatedList = new ArrayList<>(list);
        updatedList.remove(index);
        return updatedList;
    }
}
