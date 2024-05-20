package generator;

import operation.Operation;
import util.Pair;

import java.util.*;
import java.util.function.Function;

public class GEachThread implements IGenerator {
    private final IGenerator freshGen;
    private Map<Integer, Function<Context, Context>> contextFilters;
    private final Map<Object, IGenerator> gens;

    private static final String KEY_OP = "op";
    private static final String KEY_GEN = "gen";
    private static final String KEY_THREAD = "thread";

    public GEachThread(IGenerator freshGen, Map<Integer, Function<Context, Context>> contextFilters, Map<Object, IGenerator> gens) {
        this.freshGen = freshGen;
        this.contextFilters = contextFilters;
        this.gens = gens;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        eachThreadEnsureContextFilters(ctx);
        Map<String, Object> soonest = ctx.freeThreads()
                .stream()
                .map(thread -> {
                    IGenerator gen = gens.getOrDefault(thread, freshGen);
                    Context threadCtx = contextFilters.get(thread).apply(ctx);
                    // That's why we can't modify state of generator in operation: we perform freshGen.op multiple (for each thread)
                    // times without invoking operation!
                    Pair<Operation, IGenerator> res = gen.nextState(test, threadCtx);
                    if (res != null) {
                        return Map.of(KEY_OP, res.getFirst(), KEY_GEN, res.getSecond(), KEY_THREAD, thread);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .reduce(GFabric::soonestOpMap)
                .orElse(null);
        if (soonest != null) {
            Operation op = (Operation)soonest.get(KEY_OP);
            IGenerator gen = (IGenerator)soonest.get(KEY_GEN);
            Integer thread = (Integer)soonest.get(KEY_THREAD);
            gens.put(thread, gen);
            return new Pair<>(op, new GEachThread(freshGen, contextFilters, gens));
        } else if (ctx.freeThreadCount() != ctx.allThreadCount()) {
            return new Pair<>(Operation.PENDING, this);
        } else {
            return null;
        }
    }

    @Override
    public IGenerator update(Test test, Context ctx, Operation event) {
        eachThreadEnsureContextFilters(ctx);
        int process = event.getProcess();
        int thread = ctx.processToThread(process);
        IGenerator gen = gens.getOrDefault(thread, freshGen);
        Context context = contextFilters.get(thread).apply(ctx);
        IGenerator genPrime = gen.update(test, context, event);
        gens.put(thread, genPrime);
        return new GEachThread(freshGen, contextFilters, gens);
    }

    public void eachThreadEnsureContextFilters(Context ctx) {
        if (contextFilters == null) {
            contextFilters = new HashMap<>();
            Map<Integer, Function<Context, Context>> newContextFilters = new HashMap<>();
            Set<Integer> allThreads = ctx.allThreads();

            for (Integer thread : allThreads) {
                Set<Integer> hs = new HashSet<>(Collections.singleton(thread));
                newContextFilters.put(thread, Context.makeThreadFilter(hs::contains, ctx));
            }

            contextFilters.putAll(newContextFilters);
        }
    }
}
