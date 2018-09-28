package com.sublate.xmppsensor.xmpp;


import com.sublate.xmppsensor.entities.Account;
import com.sublate.xmppsensor.xmpp.stanzas.IqPacket;

public interface OnIqCommandPacketReceived extends PacketReceived {
	public void onIqCommandPacketReceived(Account account, IqPacket packet);
}
