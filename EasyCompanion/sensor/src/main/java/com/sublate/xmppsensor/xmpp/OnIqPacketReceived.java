package com.sublate.xmppsensor.xmpp;


import com.sublate.xmppsensor.entities.Account;
import com.sublate.xmppsensor.xmpp.stanzas.IqPacket;

public interface OnIqPacketReceived extends PacketReceived {
	public void onIqPacketReceived(Account account, IqPacket packet);
}
