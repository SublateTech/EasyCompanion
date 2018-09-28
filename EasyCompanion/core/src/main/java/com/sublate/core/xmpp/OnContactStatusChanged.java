package com.sublate.core.xmpp;


import com.sublate.core.entities.Contact;

public interface OnContactStatusChanged {
	public void onContactStatusChanged(final Contact contact, final boolean online);
}
