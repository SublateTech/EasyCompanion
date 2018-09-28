package com.sublate.core.xmpp;


import com.sublate.core.entities.Account;
import com.sublate.core.xmpp.stanzas.IqPacket;

public interface OnIqCommandPacketReceived extends PacketReceived {
	public void onIqCommandPacketReceived(Account account, IqPacket packet);
}
