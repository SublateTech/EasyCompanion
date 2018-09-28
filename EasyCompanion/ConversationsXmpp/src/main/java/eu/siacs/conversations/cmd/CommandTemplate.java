package eu.siacs.conversations.cmd;


import eu.siacs.conversations.services.XmppConnectionService;

@SuppressWarnings("unused")
public class CommandTemplate extends CommandHandlerBase {

    public CommandTemplate(XmppConnectionService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "Command Name", new Cmd("cmd1", "cmd1alias"), new Cmd("cms2","cmd2alias"));
        // If your command needs references, do not init them here
    }

    @Override
    protected void onCommandActivated() {
        // Allocate resources
    }

    @Override
    protected void onCommandDeactivated() {
        // Deallocate resources
    }
    
    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "cmd1")) {
            // do something useful
            if (cmd.getArg1().equals("firstArgument")) {
                // do something useful
            }
        } else {
            send("Unknown argument \"" + cmd.getAllArg1() + "\" for command \"" + cmd.getCommand() + "\"");
        }
    }

    @Override
    protected void initializeSubCommands() {
        Cmd cmd = mCommandMap.get("cmd");
        cmd.setHelp(0, null);
        cmd.AddSubCmd("subCmd", 0, null);
    }
}
