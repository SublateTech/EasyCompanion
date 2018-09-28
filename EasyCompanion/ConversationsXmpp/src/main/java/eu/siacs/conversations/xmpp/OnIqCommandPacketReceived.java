package eu.siacs.conversations.xmpp;

import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.xmpp.stanzas.IqPacket;

public interface OnIqCommandPacketReceived extends PacketReceived {
	public void onIqCommandPacketReceived(Account account, IqPacket packet);
}
