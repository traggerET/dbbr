package generator;

import operation.Operation;
import util.Pair;

public class GBranched implements IGenerator {

    private final IGenerator gen;
    private final IGenerator fTrueGen;

    private final IGenerator otherwise;
    private final Branched branch;

    /**
     * Branching generator:
     * if operation returned from gen satisfies @branch@ condition, go to fTrueGen,
     * if doesn't satisfy, go to otherwise
     * if gen must be continued, then Null must be returned from branch.
     */
    public GBranched(Branched branch, IGenerator gen, IGenerator fTrueGen, IGenerator otherwise) {
        this.gen = gen;
        this.fTrueGen = fTrueGen;
        this.otherwise = otherwise;
        this.branch = branch;
    }

    @Override
    public Pair<Operation, IGenerator> nextState(Test test, Context ctx) {
        Pair<Operation, IGenerator> result = gen.nextState(test, ctx);
        if (result != null) {
            return new Pair<>(result.getFirst(), new GBranched(branch, gen, fTrueGen, otherwise));
        }
        return null;
    }

    @Override
    public IGenerator update(Test test, Context ctx, Operation event) {
        Boolean res = branch.test(test, ctx, event);
        if (res == null) {
            return gen.update(test, ctx, event);
        } else if (res) {
            return fTrueGen;
        }
        else {
            return otherwise;
        }
    }
}
