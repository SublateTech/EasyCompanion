package com.sublate.core.xmpp;


import com.sublate.core.entities.Account;
import com.sublate.core.xmpp.stanzas.IqPacket;

public interface OnIqPacketReceived extends PacketReceived {
	public void onIqPacketReceived(Account account, IqPacket packet);
}
