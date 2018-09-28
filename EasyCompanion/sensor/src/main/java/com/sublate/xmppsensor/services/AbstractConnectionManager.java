package com.sublate.xmppsensor.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.util.Pair;

import com.sublate.xmppsensor.Config;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;




public class AbstractConnectionManager {
	protected XmppConnectionService mXmppConnectionService;

	public AbstractConnectionManager(XmppConnectionService service) {
		this.mXmppConnectionService = service;
	}

	public XmppConnectionService getXmppConnectionService() {
		return this.mXmppConnectionService;
	}

	public long getAutoAcceptFileSize() {
		String config = this.mXmppConnectionService.getPreferences().getString(
				"auto_accept_file_size", "524288");
		try {
			return Long.parseLong(config);
		} catch (NumberFormatException e) {
			return 524288;
		}
	}

	public boolean hasStoragePermission() {
		if (!Config.ONLY_INTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return mXmppConnectionService.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		} else {
			return true;
		}
	}


	public PowerManager.WakeLock createWakeLock(String name) {
		PowerManager powerManager = (PowerManager) mXmppConnectionService.getSystemService(Context.POWER_SERVICE);
		return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,name);
	}
}
