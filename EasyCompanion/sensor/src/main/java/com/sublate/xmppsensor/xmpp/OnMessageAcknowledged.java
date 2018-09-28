package com.sublate.xmppsensor.xmpp;


import com.sublate.xmppsensor.entities.Account;

public interface OnMessageAcknowledged {
	public void onMessageAcknowledged(Account account, String id);
}
