package generator;

import operation.Operation;
import util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class GReserve implements IGenerator {
    private final List<Set<Integer>> ranges;
    private final List<Function<Context, Context>> contextFilters;
    private final List<IGenerator> gens;
    private static final String KEY_OP = "op";
    private static final String KEY_GEN = "gen";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_ID = "id";

    public GReserve(List<Set<Integer>> ranges, List<Function<Context, Context>> contextFilters, List<IGenerator> gens) {
        this.ranges = ranges;
        this.contextFilters = contextFilters;
        this.gens = gens;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        List<Map<String, Object>> opMapList = IntStream.range(0, ranges.size())
                .mapToObj(i -> {
                    IGenerator gen = gens.get(i);
                    Context filteredCtx = contextFilters.get(i).apply(ctx);
                    Pair<Operation, IGenerator> opGen = gen.nextState(test, filteredCtx);
                    if (opGen != null) {
                        Map<String, Object> opMap = new HashMap<String, Object>();
                        opMap.put(KEY_OP, opGen.getFirst());
                        opMap.put(KEY_GEN, opGen.getSecond());
                        opMap.put(KEY_WEIGHT, ranges.get(i).size());
                        opMap.put(KEY_ID, i);
                        return opMap;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        Map<String, Object> defaultOpMap = null;
        Context filteredCtx = contextFilters.get(contextFilters.size() - 1).apply(ctx);
        Pair<Operation, IGenerator> defaultOpGen = gens.get(gens.size() - 1).nextState(test, filteredCtx);
        if (defaultOpGen != null) {
            defaultOpMap = new HashMap<>();
            defaultOpMap.put(KEY_OP, defaultOpGen.getFirst());
            defaultOpMap.put(KEY_GEN, defaultOpGen.getSecond());
            defaultOpMap.put(KEY_WEIGHT, ctx.allThreadCount() - ranges.stream().map(Set::size).reduce(0, Integer::sum));  
            defaultOpMap.put(KEY_ID, gens.size() - 1);
        }

        Map<String, Object> soonest = opMapList.stream().reduce(defaultOpMap, GFabric::soonestOpMap);

        if (soonest != null) {
            List<IGenerator> updatedGens = new ArrayList<>(gens);
            updatedGens.set((Integer) (soonest.get(KEY_ID)), (IGenerator) (soonest.get(KEY_GEN)));
            return new Pair<> (
                    (Operation) (soonest.get(KEY_OP)),
                    new GReserve(ranges, contextFilters, updatedGens)
            );
        }
        return null;
    }

    @Override
    public IGenerator update(Test test, Context ctx, Operation event) {
        int process = event.getProcess();
        int thread = ctx.processToThread(process);
        int i = IntStream.range(0, ranges.size())
                .filter(j -> ranges.get(j).contains(thread))
                .findFirst()
                .orElse(ranges.size());

        List<IGenerator> updatedGens = new ArrayList<>(gens);
        updatedGens.set(i, updatedGens.get(i).update(test, ctx, event));
        return new GReserve(ranges, contextFilters, updatedGens);
    }
}
