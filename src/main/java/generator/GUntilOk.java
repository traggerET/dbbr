package generator;

import operation.Operation;
import util.Pair;

import java.util.HashSet;
import java.util.Set;

public class GUntilOk implements IGenerator {
    private final IGenerator gen;
    private final boolean done;
    private final Set<Integer> activeProcesses;

    public GUntilOk(IGenerator gen, boolean done, Set<Integer> activeProcesses) {
        this.gen = gen;
        this.done = done;
        this.activeProcesses = activeProcesses;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        if (!done) {
            Pair<Operation, IGenerator> result = gen.nextState(test, ctx);
            if (result != null) {
                Operation operation = result.getFirst();
                Set<Integer> newActiveProcesses = new HashSet<>(activeProcesses);
                if (!operation.isPending()) {
                    newActiveProcesses.add(operation.getProcess());
                }
                return new Pair<>(operation, new GUntilOk(result.getSecond(), done, newActiveProcesses));
            }
        }
        return null;
    }

    public IGenerator update(Test test, Context ctx, Operation op) {
        IGenerator genPrime = gen.update(test, ctx, op);
        int p = op.getProcess();
        if (activeProcesses.contains(p)) {
            return switch (op.getResult()) {
                case OK -> {
                    Set<Integer> newActiveProcesses = new HashSet<>(activeProcesses);
                    newActiveProcesses.remove(p);
                    yield new GUntilOk(genPrime, true, newActiveProcesses);
                }
                case UNKNOWN, FAIL -> {
                    Set<Integer> newActiveProcesses = new HashSet<>(activeProcesses);
                    newActiveProcesses.remove(p);
                    yield new GUntilOk(genPrime, done, newActiveProcesses);
                }
                default -> new GUntilOk(genPrime, done, activeProcesses);
            };
        } else {
            return new GUntilOk(genPrime, done, activeProcesses);
        }
    }
}
