package com.sublate.xmppsensor.xmpp;


import com.sublate.xmppsensor.entities.Account;
import com.sublate.xmppsensor.xmpp.stanzas.MessagePacket;

public interface OnMessagePacketReceived extends PacketReceived {
	public void onMessagePacketReceived(Account account, MessagePacket packet);
}
