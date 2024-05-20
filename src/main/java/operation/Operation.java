package operation;

import interpreter.ISession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Operation {
    public static final Operation PENDING = new Operation(Type.PENDING);
    private Type type;
    private int process;
    private long time;
    private Object value;

    /*
    (Optional) The id of the client to invoke this operation.
    */
    private String clientId;

    /*
    The client must be provided when OPEN_CLIENT operation is needed.
    Should it be forbidden to provide client for CLOSE_CLIENT and INVOKE-OPERATIONS? in these cases clientId will be enough
    */
    private ISession client;

    private Consumer<Operation> invokable;

    private Result res;

    private String id;

    LocalDateTime startTime;
    LocalDateTime endTime;

    // For communication with client, additional data/settings and other user needs.
    public Map<String, Object> extMap = new HashMap<>();

    public Operation(Type type) {
        this.type = type;
    }

    public Operation(Operation operation) {
        this.type = operation.type;
        this.id = operation.id;
        this.res = operation.res;
        this.client = operation.client;
        this.clientId = operation.clientId;
        this.value = operation.value;
        this.time = operation.time;
        this.process = operation.process;
        this.invokable = operation.invokable;
        this.extMap = operation.extMap;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Result getRes() {
        return res;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getExtMap() {
        return extMap;
    }

    public void setInvokable(Consumer<Operation> invokable) {
        this.invokable = invokable;
    }

    public boolean isPending() {
        return type == Type.PENDING;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getProcess() {
        return process;
    }

    public void setProcess(int process) {
        this.process = process;
    }

    public Type getType() {
        return type;
    }

    public String getSessionId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public ISession getSession() {
        return client;
    }

    public void setClient(ISession client) {
        this.client = client;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void invoke() {
        // we want to be able to copy operations, including invokables, and also we want to be able to modify Operation
        // we invoke, that's why invokable is a field and "this" is passed to it.
        invokable.accept(this);
    }

    public Result getResult() {
        return res;
    }

    public void setRes(Result res) {
        this.res = res;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        sb.append(dtf.format(startTime)).append(" - ").append(dtf.format(endTime)).append("\n").
                append("Result: ").append(getRes()).append("\n").
                append("Type: ").append(getType()).append("\n");
        if (id != null) {
            sb.append("Operation ID: ").append(id).append("\n");
        }
        if (clientId != null) {
            sb.append("Client ID: ").append(clientId).append("\n");
        }
        if (value != null) {
            sb.append("Value: ").append(value).append("\n");
        }
                ;
        return sb.toString();
    }

    public enum Type {
        OPEN_CLIENT("OPEN_CLIENT"),
        CLOSE_CLIENT("CLOSE_CLIENT"),
        INVOKE_WITH_CLIENT("INVOKE_WITH_CLIENT"),
        INVOKE_WITHOUT_CLIENT("INVOKE_WITHOUT_CLIENT"),
        PENDING("PENDING"),
        SLEEP("SLEEP"),
        EXIT("EXIT");

        private final String type;

        Type(String res) {
            this.type = res;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    public enum Result {
        OK("OK"),
        FAIL("FAILED"),
        UNKNOWN("UNKNOWN");

        private final String res;

        Result(String res) {
            this.res = res;
        }

        @Override
        public String toString() {
            return res;
        }
    }
}
