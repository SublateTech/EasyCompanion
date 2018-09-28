package com.sublate.xmppsensor.xmpp;


import com.sublate.xmppsensor.entities.Contact;

public interface OnContactStatusChanged {
	public void onContactStatusChanged(final Contact contact, final boolean online);
}
