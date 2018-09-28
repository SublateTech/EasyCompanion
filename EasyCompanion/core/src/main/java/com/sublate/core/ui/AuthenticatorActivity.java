package com.sublate.core.ui;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.sublate.core.Config;
import com.sublate.core.R;
import com.sublate.core.entities.Account;
import com.sublate.core.xmpp.XmppConnection;
import com.sublate.core.xmpp.jid.InvalidJidException;
import com.sublate.core.xmpp.jid.Jid;


public class AuthenticatorActivity extends XmppActivity {
	public static final String ADD_ACCOUNT = "org.xmpp.android.ADD_ACCOUNT";
	private static final String KEY_ADDRESS = AccountManager.KEY_ACCOUNT_NAME;
	private static final String KEY_PASSWORD = AccountManager.KEY_PASSWORD;
	private static final String KEY_RESOURCE = "teste"; //AccountHelper.KEY_RESOURCE;
	private EditText address;
	private EditText password;
	private EditText resource;
	private View loginForm;
	private View loginProgress;
	private Jid jid;
	private LoginTask task;

	private EditText mHostname;
	private EditText mPort;
	private AlertDialog mCaptchaDialog = null;

	private AutoCompleteTextView mAccountJid;
	private Jid jidToEdit;
	private boolean mInitMode = false;
	private boolean mUsernameMode = Config.DOMAIN_LOCK != null;
	private boolean mShowOptions = false;
	private Account mAccount;

	@Override
	protected void refreshUiReal() {
		if (mAccount != null
				&& mAccount.getStatus() != Account.State.ONLINE
				) {
			//startActivity(new Intent(getApplicationContext(),
			//ManageAccountActivity.class));
			finish();
		}
		if (mAccount != null) {
		//	updateAccountInformation(false);
		}
	}


	@Override
	void onBackendConnected() {
		String s="";

	}

	private void attemptLogin() {
		if (task != null) {
			return;
		}
		String address = this.address.getText().toString();
		String password = this.password.getText().toString();
		String resource = this.resource.getText().toString();

		EditText todo = null;

		if (password.isEmpty()) {
			this.password.setError(getString(R.string.error_field_required));
			todo = this.password;
		}

		if (address.isEmpty()) {
			this.address.setError(getString(R.string.error_field_required));
			todo = this.address;
		} else if (!address.contains("@")) {
			this.address.setError(getString(R.string.error_invalid_address));
			todo = this.address;
		}

		if (todo != null) {
			todo.requestFocus();
			return;
		}

		final Jid jid;
		try {
			if (mUsernameMode) {
				jid = Jid.fromParts(this.address.getText().toString(), getUserModeDomain(), null);
			} else {
				jid = Jid.fromString(this.address.getText().toString());
			}
		} catch (final InvalidJidException e) {
			if (mUsernameMode) {
				this.address.setError(getString(R.string.invalid_username));
			} else {
				this.address.setError(getString(R.string.invalid_jid));
			}
			this.address.requestFocus();
			return;
		}
		String hostname = null;
		int numericPort = 5222;
		if (mShowOptions) {
			hostname = mHostname.getText().toString().replaceAll("\\s","");
			final String port = mPort.getText().toString().replaceAll("\\s","");
			if (hostname.contains(" ")) {
				mHostname.setError(getString(R.string.not_valid_hostname));
				mHostname.requestFocus();
				return;
			}
			try {
				numericPort = Integer.parseInt(port);
				if (numericPort < 0 || numericPort > 65535) {
					mPort.setError(getString(R.string.not_a_valid_port));
					mPort.requestFocus();
					return;
				}

			} catch (NumberFormatException e) {
				mPort.setError(getString(R.string.not_a_valid_port));
				mPort.requestFocus();
				return;
			}
		}

		if (jid.isDomainJid()) {
			if (mUsernameMode) {
				mAccountJid.setError(getString(R.string.invalid_username));
			} else {
				mAccountJid.setError(getString(R.string.invalid_jid));
			}
			mAccountJid.requestFocus();
			return;
		}
		/*
		if (registerNewAccount) {
			if (!password.equals(passwordConfirm)) {
				mPasswordConfirm.setError(getString(R.string.passwords_do_not_match));
				mPasswordConfirm.requestFocus();
				return;
			}
		}
		*/
		/*
		if (xmppConnectionService.findAccountByJid(jid) != null) {
			mAccountJid.setError(getString(R.string.account_already_exists));
			mAccountJid.requestFocus();
			return;
		}
		*/
		mAccount = new Account(jid.toBareJid(), password);
		mAccount.setPort(numericPort);
		mAccount.setHostname(jid.getDomainpart());
		mAccount.setOption(Account.OPTION_USETLS, true);
		mAccount.setOption(Account.OPTION_USECOMPRESSION, true);
		mAccount.setOption(Account.OPTION_REGISTER, false);
		xmppConnectionService.createAccount(mAccount);


		/*
		if (!mAccount.isOptionSet(Account.OPTION_DISABLED)
				//&& !registerNewAccount
				&& !mInitMode) {
			finish();
		} else {
			//updateSaveButton();
			//updateAccountInformation(true);
		}
		*/
	}

	private void disconnect() {
		xmppConnectionService.logoutAndSave(true);

	}

	private String getUserModeDomain() {
		if (mAccount != null) {
			return mAccount.getJid().getDomainpart();
		} else {
			return Config.DOMAIN_LOCK;
		}
	}
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_account);
		address = (EditText) findViewById(R.id.edit_address);
		password = (EditText) findViewById(R.id.edit_password);
		resource = (EditText) findViewById(R.id.edit_resource);
		loginForm = findViewById(R.id.view_login_form);
		loginProgress = findViewById(R.id.view_login_progress);
		if (savedInstanceState != null) {
			address.setText(savedInstanceState.getString(KEY_ADDRESS));
			password.setText(savedInstanceState.getString(KEY_PASSWORD));
			resource.setText(savedInstanceState.getString(KEY_RESOURCE));
		}
		findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				attemptLogin();
			}
		});
		findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				disconnect();
			}
		});
	}

	private void showForm() {
		loginProgress.setVisibility(View.GONE);
		loginForm.setVisibility(View.VISIBLE);
	}

	private void showProgress() {
		loginProgress.setVisibility(View.VISIBLE);
		loginForm.setVisibility(View.GONE);
	}

	private class LoginTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			return true; //XmppConnection.testJid(jid);
		}

		@Override
		protected void onCancelled() {
			showForm();
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				//Bundle b = .createAccount(AuthenticatorActivity.this, jid);
				//setAccountAuthenticatorResult(b);
				setResult(RESULT_OK);
				finish();
			} else {
				password.setError(getString(R.string.error_login_invalid));
				password.requestFocus();
				showForm();
				task = null;
			}
		}
	}

}