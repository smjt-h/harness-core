package software.wings.beans.command;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class GitOpsDummyCommandUnit extends AbstractCommandUnit{

    public static final String FetchFiles = "Fetch Files";
    public static final String UpdateFiles = "Update Files";
    public static final String Commit = "Commit";

    public GitOpsDummyCommandUnit(String name) {
        this.setName(name);
    }

    @Override
    public CommandExecutionStatus execute(CommandExecutionContext context) {
        return null;
    }
}
