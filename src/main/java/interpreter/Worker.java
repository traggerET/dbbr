package interpreter;

import operation.Operation;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

public class Worker implements IWorker {
    private Future<?> future;
    private int id;
    private BlockingQueue<Operation> inQ;

    public Worker(Future<?> future, int id, BlockingQueue<Operation> inQ) {
        this.future = future;
        this.id = id;
        this.inQ = inQ;
    }


    public Future<?> getFuture() {
        return future;
    }
    public int getId() {
        return id;
    }
    public BlockingQueue<Operation> getInQ() {
        return inQ;
    }
}
