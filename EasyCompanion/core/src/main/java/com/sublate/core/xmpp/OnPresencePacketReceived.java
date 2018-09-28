package com.sublate.core.xmpp;


import com.sublate.core.entities.Account;
import com.sublate.core.xmpp.stanzas.PresencePacket;

public interface OnPresencePacketReceived extends PacketReceived {
	public void onPresencePacketReceived(Account account, PresencePacket packet);
}
