package com.sublate.core.crypto.sasl;

import com.sublate.core.entities.Account;
import com.sublate.core.xml.TagWriter;

import java.security.SecureRandom;



public class Anonymous extends SaslMechanism {

	public Anonymous(TagWriter tagWriter, Account account, SecureRandom rng) {
		super(tagWriter, account, rng);
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public String getMechanism() {
		return "ANONYMOUS";
	}

	@Override
	public String getClientFirstMessage() {
		return "";
	}
}
