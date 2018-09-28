package com.sublate.core.xmpp;


import com.sublate.core.entities.Account;
import com.sublate.core.xmpp.stanzas.MessagePacket;

public interface OnMessagePacketReceived extends PacketReceived {
	public void onMessagePacketReceived(Account account, MessagePacket packet);
}
