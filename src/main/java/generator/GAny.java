package generator;

import operation.Operation;
import util.Pair;

import java.util.*;

public class GAny implements IGenerator {
    private final List<IGenerator> gens;

    private static final String KEY_GEN = "gen";
    private static final String KEY_ID = "id";
    private static final String KEY_OP = "op";

    public GAny(List<IGenerator> gens) {
        this.gens = gens;
    }
    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context context) {
        List<Map<String, Object>> gMaps = new ArrayList<>();
        for (int i = 0; i < gens.size(); i++) {
            IGenerator gen = gens.get(i);
            Pair<Operation, IGenerator> result = gen.nextState(test, context);
            if (result != null) {
                Map<String, Object> gMap = new HashMap<>();
                gMap.put(KEY_GEN, result.getSecond());
                gMap.put(KEY_OP, result.getFirst());
                gMap.put(KEY_ID, i);
                gMaps.add(gMap);
            }
        }
        Map<String, Object> choice = gMaps.stream().reduce(null, GFabric::soonestOpMap);
        if (choice == null) {
            return null;
        }
        IGenerator gen1 = (IGenerator) choice.get(KEY_GEN);
        int id = (Integer) choice.get(KEY_ID);
        ArrayList<IGenerator> gens1 = new ArrayList<>(gens);
        gens1.set(id, gen1);
        return new Pair<>((Operation) choice.get(KEY_OP), new GAny(gens1));
    }

    @Override
    public IGenerator update(Test test, Context context, Operation event) {
        List<IGenerator> updatedGens = new ArrayList<>();
        for (IGenerator gen : gens) {
            updatedGens.add(gen.update(test, context, event));
        }
        return new GAny(updatedGens);
    }
}
