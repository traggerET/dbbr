package interpreter;

import operation.Operation;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

public interface IWorker {
    Future<?> getFuture();
    int getId();
    BlockingQueue<Operation> getInQ();
}