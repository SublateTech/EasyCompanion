package com.sublate.xmppsensor.crypto.sasl;

import android.util.Base64;

import com.sublate.xmppsensor.entities.Account;
import com.sublate.xmppsensor.xml.TagWriter;

import java.security.SecureRandom;



public class External extends SaslMechanism {

	public External(TagWriter tagWriter, Account account, SecureRandom rng) {
		super(tagWriter, account, rng);
	}

	@Override
	public int getPriority() {
		return 25;
	}

	@Override
	public String getMechanism() {
		return "EXTERNAL";
	}

	@Override
	public String getClientFirstMessage() {
		return Base64.encodeToString(account.getJid().toBareJid().toString().getBytes(),Base64.NO_WRAP);
	}
}
