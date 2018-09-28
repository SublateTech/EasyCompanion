package com.sublate.xmppsensor.xmpp;


import com.sublate.xmppsensor.entities.Account;

public interface OnStatusChanged {
	public void onStatusChanged(Account account);
}
