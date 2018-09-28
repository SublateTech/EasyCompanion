package com.sublate.xmppsensor.xmpp.stanzas.streammgmt;


import com.sublate.xmppsensor.xmpp.stanzas.AbstractStanza;

public class RequestPacket extends AbstractStanza {

	public RequestPacket(int smVersion) {
		super("r");
		this.setAttribute("xmlns", "urn:xmpp:sm:" + smVersion);
	}

}
