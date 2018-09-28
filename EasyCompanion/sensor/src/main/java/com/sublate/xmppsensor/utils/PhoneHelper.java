package com.sublate.xmppsensor.utils;

import android.Manifest;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.Loader.OnLoadCompleteListener;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

public class PhoneHelper {


	private static class NotThrowCursorLoader extends CursorLoader {

		public NotThrowCursorLoader(Context c, Uri u, String[] p, String s, String[] sa, String so) {
			super(c, u, p, s, sa, so);
		}

		@Override
		public Cursor loadInBackground() {

			try {
				return (super.loadInBackground());
			} catch (Throwable e) {
				return(null);
			}
		}

	}

	public static Uri getSelfiUri(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				&& context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
			return null;
		}
		String[] mProjection = new String[]{Profile._ID, Profile.PHOTO_URI};
		Cursor mProfileCursor = context.getContentResolver().query(
				Profile.CONTENT_URI, mProjection, null, null, null);

		if (mProfileCursor == null || mProfileCursor.getCount() == 0) {
			return null;
		} else {
			mProfileCursor.moveToFirst();
			String uri = mProfileCursor.getString(1);
			mProfileCursor.close();
			if (uri == null) {
				return null;
			} else {
				return Uri.parse(uri);
			}
		}
	}

	public static String getVersionName(Context context) {
		final String packageName = context == null ? null : context.getPackageName();
		if (packageName != null) {
			try {
				return context.getPackageManager().getPackageInfo(packageName, 0).versionName;
			} catch (final PackageManager.NameNotFoundException | RuntimeException e) {
				return "unknown";
			}
		} else {
			return "unknown";
		}
	}
}
