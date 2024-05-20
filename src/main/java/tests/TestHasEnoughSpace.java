package tests;

import generator.IGenerator;
import generator.GFabric;
import generator.Test;
import interpreter.Interpreter;
import operation.Operation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestHasEnoughSpace {
    public static void test() throws InterruptedException {
        Operation occupySpace = createOccupySpaceOperation();
        IGenerator genOccupySpace = GFabric.fromOp(occupySpace);

        Operation spamRequestsOperation = createSpamRequestsOperation();
        IGenerator updreqG = GFabric.fromOp(spamRequestsOperation);

        Operation switchWalFileOperation = createSwitchWalFileOperation();
        IGenerator switchG = GFabric.fromOp(switchWalFileOperation);

        IGenerator genSpam = GFabric.timeLimit(6000000000L, GFabric.phases(Arrays.asList(updreqG, switchG)));

        Operation releaseSpace = createReleaseSpaceOperation();
        IGenerator genReleaseSpace = GFabric.fromOp(releaseSpace);

        IGenerator testGen = GFabric.then(genOccupySpace, genSpam, genReleaseSpace);

        Interpreter.run(new Test(testGen));
    }

    private static Operation createSwitchWalFileOperation() {
        Operation op = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        op.setInvokable((op1) -> {
            String jdbcUrl = "jdbc:postgresql://localhost:5432/dvdrental";
            String username = "tihon";
            String password = "31313541";
            List<String> fulltexts = Arrays.asList(
                    "some long and useful description1",
                    "another short and useless description2",
                    "some long and useful description3",
                    "another short and useless description4",
                    "some long and useful description5");
            Connection connection;
            try {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement();
                String q = "UPDATE film SET fulltext = '" +
                        fulltexts.get(ThreadLocalRandom.current().nextInt(0, fulltexts.size())) +
                        "';";
                statement.executeQuery(q);
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return op;
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
                statement.executeQuery("SELECT pg_switch_wal();");
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return op;
    }

    private static Operation createOccupySpaceOperation() {
        Operation occupy = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        occupy.setInvokable((op) -> {
            String[] cmd = {"/bin/bash","-c", "fallocate -l 1G a.txt"};
            Process pb = null;
            try {
                pb = Runtime.getRuntime().exec(cmd);
                pb.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return occupy;
    }

    private static Operation createReleaseSpaceOperation() {
        Operation occupy = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        occupy.setInvokable((op) -> {
            String[] cmd = {"/bin/bash","-c", "rm a.txt"};
            Process pb = null;
            try {
                pb = Runtime.getRuntime().exec(cmd);
                pb.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return occupy;
    }
}
