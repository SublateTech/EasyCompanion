package eu.siacs.conversations.cmd;


public class XmppMsg {

    private final StringBuilder mMessage = new StringBuilder();


    public XmppMsg() {

    }

    public XmppMsg(String msg) {

        mMessage.append(msg);
    }

    public XmppMsg(String msg, boolean newLine) {
        mMessage.append(msg);

    }
}