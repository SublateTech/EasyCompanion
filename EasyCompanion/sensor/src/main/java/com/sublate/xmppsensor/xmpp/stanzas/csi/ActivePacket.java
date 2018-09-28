package com.sublate.xmppsensor.xmpp.stanzas.csi;


import com.sublate.xmppsensor.xmpp.stanzas.AbstractStanza;

public class ActivePacket extends AbstractStanza {
	public ActivePacket() {
		super("active");
		setAttribute("xmlns", "urn:xmpp:csi:0");
	}
}
