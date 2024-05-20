package tests;

import generator.IGenerator;
import generator.GFabric;
import generator.Test;
import interpreter.Interpreter;
import operation.Operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class TestCannotModifyBackUpFiles {
    public static void test() throws InterruptedException {
        Operation op = createMakeBackupOperation();
        IGenerator backupGen = GFabric.fromOp(op);

        op = createCheckCannotModifyOperation();
        IGenerator checkModification = GFabric.fromOp(op);

        IGenerator testGen = GFabric.phases(Arrays.asList(backupGen, checkModification));

        Interpreter.run(new Test(testGen));
    }

    private static Operation createCheckCannotModifyOperation() {
        Operation op = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        op.setInvokable((op1) -> {
            String commandSB = "echo 31313541 | sudo -S " +
                    "/home/tihon/IdeaProjects/DBBreaker/scripts/checkPermissions.sh";

            String[] cmd = {"/bin/bash","-c", commandSB};
            Process pb;
            int res = 0;
            try {
                pb = Runtime.getRuntime().exec(cmd);
                res = pb.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (res != 0) {
                String line;
                BufferedReader input = new BufferedReader(new InputStreamReader(pb.getInputStream()));
                StringBuilder sb = new StringBuilder();
                try {
                    while ((line = input.readLine()) != null) {
                        sb.append(line);
                    }
                    input.close();
                    op1.setRes(Operation.Result.FAIL);
                    op1.setValue(sb.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return op;
    }

    private static Operation createMakeBackupOperation() {
        Operation op = new Operation(Operation.Type.INVOKE_WITHOUT_CLIENT);
        op.setInvokable((op1) -> {
            String commandSB = "echo 31313541 | sudo -S " +
                    "/home/tihon/IdeaProjects/DBBreaker/scripts/make_backup.sh /tmp";
            String[] cmd = {"/bin/bash","-c", commandSB};
            Process pb;
            try {
                pb = Runtime.getRuntime().exec(cmd);
                if (pb.waitFor() != 0) {
                    op.setRes(Operation.Result.FAIL);
                    op.setValue("Could not create backup of DB");
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return op;
    }
}
