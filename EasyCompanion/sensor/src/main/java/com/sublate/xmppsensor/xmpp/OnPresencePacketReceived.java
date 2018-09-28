package com.sublate.xmppsensor.xmpp;


import com.sublate.xmppsensor.entities.Account;
import com.sublate.xmppsensor.xmpp.stanzas.PresencePacket;

public interface OnPresencePacketReceived extends PacketReceived {
	public void onPresencePacketReceived(Account account, PresencePacket packet);
}
