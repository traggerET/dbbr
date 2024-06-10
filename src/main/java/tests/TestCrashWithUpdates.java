package tests;

import generator.IGenerator;
import generator.GFabric;
import generator.Test;
import interpreter.ISession;
import interpreter.Interpreter;
import operation.Operation;
import org.apache.commons.text.StringSubstitutor;
import util.Pair;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCrashWithUpdates {
    public static List<String> tableNames = List.of("customer", "film", "address");

    public static List<Pair<String, List<String>>> descrs = List.of(new Pair<>("email", List.of("asdsad@phystech.edu", "svdvmfdv@phystech.edu", "kdvkkv@phystech.edu")),
            new Pair<>("fulltext", List.of("some long and useful description1",
                    "another short and useless description2",
                    "some long and useful description3")),
                    new Pair<>("district", List.of("Holland", "Mexico", "Russia")));
    public static class CheckIntegritySession implements ISession {
        private final AtomicInteger tableToCheckCorruption = new AtomicInteger(0);

        @Override
        public void open(Operation operation) {
        }

        @Override
        public void invoke(Operation operation) {
            String queryStr = (String) operation.getExtMap().get("query");

            Map<String, String> values = new HashMap<>();
            ;
            values.put("table", tableNames.get(tableToCheckCorruption.getAndAdd(1)));

            String query = StringSubstitutor.replace(queryStr, values, "{", "}");

            String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
            String username = "tihon";
            String password = "31313541";
            Connection connection = null;
            try {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement();
                statement.executeQuery(query);
            } catch (SQLException e) {
                operation.setRes(Operation.Result.FAIL);
                operation.setValue("DB was not restored correctly");
            }
        }

        @Override
        public void close(Operation operation) {
        }

        @Override
        public String getId() {
            return "TablesIntegrity";
        }
    }

    public static class SpamReqsSession implements ISession {
        private final AtomicInteger tableToCheckCorruption = new AtomicInteger(0);

        @Override
        public void open(Operation operation) {
        }

        @Override
        public void invoke(Operation operation) {
            String queryStr = (String) operation.getExtMap().get("query");

            Map<String, String> values = new HashMap<>();
            ;
            values.put("table", tableNames.get(tableToCheckCorruption.getAndAdd(1)));

            String query = StringSubstitutor.replace(queryStr, values, "{", "}");

            String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
            String username = "tihon";
            String password = "31313541";
            Connection connection = null;
            try {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement();
                statement.executeQuery(query);
            } catch (SQLException e) {
                operation.setRes(Operation.Result.FAIL);
                operation.setValue("DB was not restored correctly");
            }
        }

        @Override
        public void close(Operation operation) {
        }

        @Override
        public String getId() {
            return "TablesIntegrity";
        }
    }

    public static void test() throws InterruptedException {
        Operation spamRequestsOperation = createSpamRequestsOperation();
        IGenerator genSpam = GFabric.timeLimit(6, GFabric.cycle(GFabric.fromOp(spamRequestsOperation)));

        Operation killProcessOperation = createKillProcessOperation();
        IGenerator genKill = GFabric.fromOp(killProcessOperation);

        Operation restoreFromWAL = createRestoreFromWal();
        IGenerator genRestore = GFabric.fromOp(restoreFromWAL);

        Operation checkIntegrity = createCheckTablesIntegrity();
        IGenerator genCheckIntegrity = GFabric.repeat(tableNames.size(), GFabric.fromOp(checkIntegrity));

        IGenerator testGen = GFabric.then(GFabric.fromGens(genKill, genSpam), genRestore, genCheckIntegrity);

        Interpreter.run(new Test(testGen));
    }

    private static Operation createSpamRequestsOperation() {
        Operation op = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        op.setInvokable((op1) -> {
            String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
            String username = "tihon";
            String password = "31313541";
            Connection connection;
            try {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement();
                int tblId = ThreadLocalRandom.current().nextInt(0, tableNames.size());
                int dId = ThreadLocalRandom.current().nextInt(0, descrs.get(tblId).getSecond().size());
                String q = "UPDATE " + tableNames.get(tblId) + " SET + " + descrs.get(tblId).getFirst() + " = '" +
                        descrs.get(tblId).getSecond().get(dId) + "';";
                statement.executeQuery(q);
                connection.close();
                op1.setValue("Finished");
            } catch (SQLException e) {
                op1.setValue("Process is already dead");
            }
        });
        return op;
    }

    private static Operation createKillProcessOperation() {
        Operation kill = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        kill.setInvokable((op) -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String commandSB = "echo 31313541 | sudo -S " +
                    "ps aux | grep postgres | awk '{print $2}' | xargs kill -9";
            String[] cmd = {"/bin/bash","-c", commandSB};
            Process pb = null;
            try {
                pb = Runtime.getRuntime().exec(cmd);
                pb.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return kill;
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

    private static Operation createCheckTablesIntegrity() {
        Operation op = new Operation(Operation.Type.INVOKE_WITH_CLIENT);
        op.setClient(new CheckIntegritySession());
        op.setClientId("TablesIntegrity");
        op.getExtMap().put("query", "select * from {table};");
        return op;
    }
}
