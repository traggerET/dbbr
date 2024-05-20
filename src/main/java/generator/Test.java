package generator;

public class Test {

    public IGenerator gen;
    public long maxPendingInterval = 500000;
    public int concurrency = 5;

    public Test(IGenerator gen) {
        this.gen = gen;
    }
}
