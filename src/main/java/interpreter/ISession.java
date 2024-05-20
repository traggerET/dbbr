package interpreter;

import operation.Operation;

public interface ISession {
    void open(Operation operation);
    void invoke(Operation operation);
    void close(Operation operation);
    String getId();
}
