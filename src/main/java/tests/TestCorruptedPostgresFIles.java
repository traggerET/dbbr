package tests;

import generator.IGenerator;
import generator.GFabric;
import generator.Test;
import interpreter.ISession;
import interpreter.Interpreter;
import operation.Operation;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCorruptedPostgresFIles {
    public static List<String> tableNames = List.of("actor", "film", "city");

    public static class CheckCorruptionSession implements ISession {
        private AtomicInteger corruptId = new AtomicInteger(0);
        private String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
        private String username = "tihon";
        private String password = "31313541";

        @Override
        public void open(Operation operation) {
        }

        @Override
        public void invoke(Operation operation) {
            String queryStr = (String) operation.getExtMap().get("query");

            Map<String, String> values = new HashMap<>();
            values.put("table", tableNames.get(corruptId.getAndAdd(1)));

            String query = StringSubstitutor.replace(queryStr, values, "{", "}");

            boolean currentCorruptionDetected = false;
            try {
                Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement();
                statement.executeQuery(query);
                connection.close();
            } catch (SQLException e) {
                currentCorruptionDetected = true;
            }
            if (!currentCorruptionDetected) {
                operation.setRes(Operation.Result.FAIL);
                operation.setValue("Corruption was not detected");
            }
        }

        @Override
        public void close(Operation operation) {
        }

        @Override
        public String getId() {
            return "CorruptionClient";
        }
    }

    public static class CheckRepairedSession implements ISession {
        private AtomicInteger  tableToCheckRepaired = new AtomicInteger(0);
        String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
        String username = "tihon";
        String password = "31313541";
        @Override
        public void open(Operation operation) {
        }

        @Override
        public void invoke(Operation operation) {
            String queryStr = (String) operation.getExtMap().get("query");

            Map<String, String> values = new HashMap<>();
            values.put("table", tableNames.get(tableToCheckRepaired.getAndAdd(1)));

            String query = StringSubstitutor.replace(queryStr, values, "{", "}");

            try {
                Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement();
                statement.executeQuery(query);
                connection.close();
            } catch (SQLException e) {
                operation.setRes(Operation.Result.FAIL);
                operation.setValue("Corruption was not correctly repaired");
            }
        }

        @Override
        public void close(Operation operation) {
        }

        @Override
        public String getId() {
            return "RepairedClient";
        }
    }

    private static final List<String> filesToCorrupt = List.of("/var/lib/postgresql/12/main/base/16643/16679",
            "/var/lib/postgresql/12/main/base/16643/16720",
            "/var/lib/postgresql/12/main/base/16643/16740");

    public static void test() throws InterruptedException {
        Operation corruptFiles = createCorruptOperation();
        IGenerator genCorrupt = GFabric.fromOp(corruptFiles);

        Operation checkCorrupt = createCheckCorruptOperation();
        IGenerator genCheckCorrupt = GFabric.repeat(filesToCorrupt.size(), GFabric.fromOp(checkCorrupt));

        Operation restoreFromWAL = createRestoreFromWal();
        IGenerator genRestore = GFabric.fromOp(restoreFromWAL);

        Operation checkRepaired = createCheckRepairedOperation();
        IGenerator genCheckRepaired = GFabric.repeat(filesToCorrupt.size(), GFabric.fromOp(checkRepaired));

        IGenerator testGen = GFabric.phases(Arrays.asList(genCorrupt, genCheckCorrupt, genRestore, genCheckRepaired));

        Interpreter.run(new Test(testGen));
    }

    private static Operation createCheckRepairedOperation() {
        Operation op = new Operation(Operation.Type.INVOKE_WITH_CLIENT);
        op.setClient(new CheckRepairedSession());
        op.setClientId("RepairedClient");
        op.getExtMap().put("query", "select * from {table};");
        return op;
    }

    private static Operation createRestoreFromWal() {
        Operation restore = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        restore.setInvokable((op) -> {
            String commandSB = "echo 31313541 | sudo -S " +
                    "/home/tihon/IdeaProjects/DBBreaker/scripts/recoverLocal.sh";
            String[] cmd = {"/bin/bash","-c", commandSB};
            Process pb = null;
            try {
                pb = Runtime.getRuntime().exec(cmd);
                if (pb.waitFor() != 0) {
                    op.setRes(Operation.Result.FAIL);
                    op.setValue("Failed to restore db");
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        });
        return restore;
    }

    private static Operation createCorruptOperation() {
        Operation corruptFiles = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        corruptFiles.setInvokable((op) -> {
            for (String fileToCorrupt: filesToCorrupt) {
                String commandSB = "echo 31313541 | sudo -S " +
                        "/home/tihon/IdeaProjects/DBBreaker/scripts/corruptFile.sh " +
                        fileToCorrupt;
                String[] cmd = {"/bin/bash","-c", commandSB};
                Process pb = null;
                try {
                    pb = Runtime.getRuntime().exec(cmd);
                    pb.waitFor();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return corruptFiles;
    }

    private static Operation createCheckCorruptOperation() {
        Operation op = new Operation(Operation.Type.INVOKE_WITH_CLIENT);
        op.setClient(new CheckCorruptionSession());
        op.setClientId("CorruptionClient");
        op.getExtMap().put("query", "select * from {table};");
        return op;
    }
}
