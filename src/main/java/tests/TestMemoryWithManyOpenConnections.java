package tests;

import generator.IGenerator;
import generator.GFabric;
import generator.Test;
import interpreter.Interpreter;
import operation.Operation;

import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class TestMemoryWithManyOpenConnections {
    private static final String EXPENSIVE_QUERY = "SET work_mem to '32MB';\n" +
        "select * \n" +
        "from pg_class a, pg_class b, pg_class c, pg_class d, pg_class e \n" +
        "order by random();\n";

    public static void test() throws InterruptedException {
        Operation op = createExpensiveQueryOperation();
        IGenerator genExpensiveConnections = GFabric.repeat(20, GFabric.fromOp(op));

        op = createCheckAliveOperation();
        IGenerator genCheckAliveOperation = GFabric.fromOp(op);
        IGenerator testGen = GFabric.then(genExpensiveConnections, genCheckAliveOperation);

        Interpreter.run(new Test(testGen));
    }

    private static Operation createExpensiveQueryOperation() {
        Operation op = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        op.setInvokable((op1) -> {
            CompletableFuture.runAsync(() -> {
                String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
                String username = "tihon";
                String password = "31313541";
                Connection connection = null;
                try {
                    connection = DriverManager.getConnection(jdbcUrl, username, password);
                    Statement statement = connection.createStatement();
                    statement.setQueryTimeout(100);
                    statement.executeQuery(EXPENSIVE_QUERY);
                    connection.close();

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        return op;
    }
    private static Operation createCheckAliveOperation() {
        Operation op = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        op.setInvokable((op1) -> {
            String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
            String username = "tihon";
            String password = "31313541";
            Connection connection = null;
            try {
                // Check server is responding and did not crash
                connection = DriverManager.getConnection(jdbcUrl, username, password);
                connection.close();
            } catch (SQLException e) {
                op1.setRes(Operation.Result.FAIL);
                op1.setValue(e);
                return;
            }
            op1.setRes(Operation.Result.OK);
        });
        return op;
    }
}
