package interpreter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import generator.GFabric;
import operation.Operation;
import generator.IGenerator;
import generator.Test;
import generator.Context;
import util.Pair;

public class Interpreter {
    private static final ConcurrentHashMap<String, ISession> activeClients = new ConcurrentHashMap<>();

    public static boolean run(Test test) throws InterruptedException {
        long startTime = System.nanoTime();
        IGenerator gen = test.gen;
        Context ctx = Context.createContext(test);
        Set<Integer> workerIds = ctx.allThreads();
        ArrayBlockingQueue<Operation> completions = new ArrayBlockingQueue<>(workerIds.size());
        List<IWorker> workers = new ArrayList<>();
        Map<Integer, BlockingQueue<Operation>> invocations = new HashMap<>();
        for (int workerId : workerIds) {
            IWorker worker = spawnWorker(test, completions, workerId);
            invocations.put(workerId, worker.getInQ());
            workers.add(worker);
        }
        gen = GFabric.friendly(gen);

        try {
            int outstanding = 0;
            long pollTimeout = 0;
            long maxPendingInterval = test.maxPendingInterval;
            while (true) {
                Operation op = completions.poll(pollTimeout, TimeUnit.MICROSECONDS);
                if (op != null) {
                    int thread = ctx.processToThread(op.getProcess());
                    long time = startTime - System.nanoTime();
                    op.setTime(time);
                    ctx = ctx.freeThread(time, thread);
                    gen = gen.update(test, ctx, op);
                    if (op.getRes() == Operation.Result.FAIL) {
                        for (Map.Entry<Integer, BlockingQueue<Operation>> entry : invocations.entrySet()) {
                            entry.getValue().put(new Operation(Operation.Type.EXIT));
                        }
                        for (IWorker worker : workers) {
                            worker.getFuture().get();
                        }
                        return false;
                    }
                    outstanding--;
                    pollTimeout = 0;
                } else {
                    long time = startTime - System.nanoTime();
                    ctx.setTime(time);
                    Pair<Operation, IGenerator> pair = gen.nextState(test, ctx);
                    if (pair == null) {
                        if (outstanding > 0) {
                            pollTimeout = Math.max(maxPendingInterval, pollTimeout);
                        } else {
                            for (Map.Entry<Integer, BlockingQueue<Operation>> entry : invocations.entrySet()) {
                                entry.getValue().put(new Operation(Operation.Type.EXIT));
                            }
                            for (IWorker worker : workers) {
                                worker.getFuture().get();
                            }
                            return true;
                        }
                    } else {
                        op = pair.getFirst();
                        IGenerator gen1 = pair.getSecond();
                        if (op.isPending()) {
                            pollTimeout = Math.max(maxPendingInterval, pollTimeout);
                        } else if (time < op.getTime()) {
                            pollTimeout = (op.getTime() - time) / 1000;
                        } else {
                            int thread = ctx.processToThread(op.getProcess());
                            invocations.get(thread).put(op);
                            ctx = ctx.busyThread(op.getTime(), thread);
                            gen = gen1.update(test, ctx, op);
                            outstanding++;
                            pollTimeout = 0;
                        }
                    }
                }
            }
        } catch (ExecutionException t) {
            for (IWorker IWorker : workers) {
                IWorker.getFuture().cancel(true);
            }
            for (IWorker IWorker : workers) {
                if (!IWorker.getFuture().isDone()) {
                    IWorker.getInQ().offer(new Operation(Operation.Type.EXIT));
                }
            }
            System.err.println("Exception happened during execution");
            System.err.println(t.getMessage());
            System.out.println("Terminating");
            throw new RuntimeException(t);
        }
        return true;
    }

    private static IWorker spawnWorker(Test test, BlockingQueue<Operation> out, int id) {
        BlockingQueue<Operation> in = new ArrayBlockingQueue<Operation>(1);
        Future<?> fut = Executors.newSingleThreadExecutor().submit(() -> {
            Thread.currentThread().setName("worker " + id);
            try {
                while (true) {
                    Operation op = in.take();
                    try {
                        op.setStartTime(LocalDateTime.now());
                        ISession session;
                        switch (op.getType()) {
                            case OPEN_CLIENT:
                                session = op.getSession();
                                ISession prev = activeClients.putIfAbsent(op.getSessionId(), session);
                                // If same id provided for two different clients:
                                if (prev != null && prev != session) {
                                    throw new RuntimeException("Can't use same id for two different sessions");
                                }
                                try {
                                    session.open(op);
                                    setOkIfNull(op);
                                } catch (Exception e) {
                                    activeClients.remove(op.getSessionId());
                                    throw e;
                                }
                                out.put(op);
                                break;
                            case CLOSE_CLIENT:
                                String sId = op.getSessionId();
                                session = activeClients.get(sId);
                                session.close(op);
                                setOkIfNull(op);
                                activeClients.remove(op.getSessionId());
                                out.put(op);
                                break;
                            case INVOKE_WITH_CLIENT:
                                sId = op.getSessionId();
                                session = activeClients.get(sId);
                                session.invoke(op);
                                setOkIfNull(op);
                                out.put(op);
                                break;
                            case INVOKE_WITHOUT_CLIENT:
                                op.invoke();
                                setOkIfNull(op);
                                out.put(op);
                                break;
                            case SLEEP:
                                Thread.sleep((long)op.getValue() * 1000);
                                setOkIfNull(op);
                                out.put(op);
                                break;
                            case PENDING:
                                break;
                            case EXIT:
                                return;
                            default:
                                throw new RuntimeException("Unknown operation type");
                        }
                    } catch (Throwable e) {
                        System.out.println("Process " + op.getProcess() + " crashed");
                        op.setRes(Operation.Result.UNKNOWN);
                        op.setValue("indeterminate: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                        out.put(op);
                    }
                    finally {
                        op.setEndTime(LocalDateTime.now());
                    }
                }
            } catch (Exception e) {
            }
        });
        return new Worker(fut, id, in);
    }

    private static void setOkIfNull(Operation op) {
        if (op.getRes() == null) {
            op.setRes(Operation.Result.OK);
        }
    }
}

