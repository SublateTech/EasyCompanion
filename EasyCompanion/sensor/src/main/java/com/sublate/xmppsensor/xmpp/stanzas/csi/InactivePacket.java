package com.sublate.xmppsensor.xmpp.stanzas.csi;


import com.sublate.xmppsensor.xmpp.stanzas.AbstractStanza;

public class InactivePacket extends AbstractStanza {
	public InactivePacket() {
		super("inactive");
		setAttribute("xmlns", "urn:xmpp:csi:0");
	}
}
