package eu.siacs.conversations.cmd;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.siacs.conversations.services.XmppConnectionService;

public class CommandActivationCmd extends CommandHandlerBase {

    private Map<String, Cmd> mListCommands = new HashMap<String, Cmd>();

    public CommandActivationCmd(XmppConnectionService mainService) {
            super(mainService, CommandHandlerBase.TYPE_INTERNAL, "Command Activator", new Cmd("activate"), new Cmd("deactivate"));
    }

    @Override
    protected void onCommandActivated() {
        Set<CommandHandlerBase> commands = null; //XmppConnectionService.getCommandHandlersSet();
        mListCommands.clear();
        if (commands != null) {
            for (CommandHandlerBase cmdBase : commands) {
                if (cmdBase.getType() != CommandHandlerBase.TYPE_INTERNAL) {
                    for (Cmd c : cmdBase.getCommands()) {
                        mListCommands.put(c.getName().toLowerCase(), c);
                    }
                }
            }
        }
    }

    @Override
    protected void onCommandDeactivated() {
        mListCommands.clear();
    }

    @Override
    protected void execute(Command cmd) {
        String arg1 = cmd.getArg1();
        if (isMatchingCmd(cmd, "activate")) {
            if (arg1.equals("")) {
                XmppMsg msg = new XmppMsg("List of activated commands", true);
                for (Cmd c : mListCommands.values()) {
                    if (c.isActive()) {
                        //msg.appendBold(c.getName());
                        //msg.appendLine(": Activated");
                    }
                }
                send(msg);
            } else {
                Cmd c = mListCommands.get(arg1.toLowerCase());
                if (c != null) {
                    updateCommand(c, true);
                    send(c.getName() + ": " + (c.isActive() ? "Activated" : "Deactivated"));
                } else {
                    send("Command not found");
                }
            }
        } if (isMatchingCmd(cmd, "deactivate")) {
            if (arg1.equals("")) {
                XmppMsg msg = new XmppMsg("List of deactivated commands", true);
                for (Cmd c : mListCommands.values()) {
                    if (!c.isActive()) {
                        //msg.appendBold(c.getName());
                        //msg.appendLine(": Deactivated");
                    }
                }
                send(msg);
            } else {
                Cmd c = mListCommands.get(arg1.toLowerCase());
                if (c != null) {
                    updateCommand(c, false);
                    send(c.getName() + ": " + (c.isActive() ? "Activated" : "Deactivated"));
                } else {
                    send("Command not found");
                }
            }
        }
    }

    @Override
    protected void initializeSubCommands() {
    }

    private void updateCommand(Cmd cmd, boolean isActive) {
        cmd.setActive(isActive);
     //   XmppConnectionService.updateCommandState();
    }
}
