package com.sublate.core.xmpp;


import com.sublate.core.entities.Account;

public interface OnStatusChanged {
	public void onStatusChanged(Account account);
}
