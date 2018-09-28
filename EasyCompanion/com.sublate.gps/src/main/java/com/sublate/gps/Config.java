package com.sublate.gps;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.sublate.gps.model.LocationDataHelper;
import com.sublate.gps.services.BootReceiver;
import com.sublate.gps.services.SchedulerService;


public class Config {
	public static boolean DEBUG = false;
	public static final int LOCATION_SERVICE_WAKE_LOCK_TIMEOUT = 60000;
	public static final int BOOT_RECEIVER_WAKE_LOCK_TIMEOUT = 60000;


	/**
	 * Called throughout the application when the number of accounts has changed. This method
	 * enables or disables the Compose activity, the boot receiver and the service based on
	 * whether any accounts are configured.
	 */
	public static void setServicesEnabled(Context context) {
		setServicesEnabled(context, true, null);

	}

	private static void setServicesEnabled(Context context, boolean enabled, Integer wakeLockId) {

		PackageManager pm = context.getPackageManager();

		if (!enabled && pm.getComponentEnabledSetting(new ComponentName(context, SchedulerService.class)) ==
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * If no accounts now exist but the service is still enabled we're about to disable it
             * so we'll reschedule to kill off any existing alarms.
             */
			SchedulerService.actionReset(context, wakeLockId);
		}
		Class<?>[] classes = { BootReceiver.class, SchedulerService.class };

		for (Class<?> clazz : classes) {

			boolean alreadyEnabled = pm.getComponentEnabledSetting(new ComponentName(context, clazz)) ==
					PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

			if (enabled != alreadyEnabled) {
				pm.setComponentEnabledSetting(
						new ComponentName(context, clazz),
						enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
								PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP);
			}
		}

		if (enabled && pm.getComponentEnabledSetting(new ComponentName(context, SchedulerService.class)) ==
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * And now if accounts do exist then we've just enabled the service and we want to
             * schedule alarms for the new accounts.
             */
			SchedulerService.actionReset(context, wakeLockId);
		}

	}

	public static final String LOGTAG = "com.sublate.gps";

	public static final String BUG_REPORTS = "bugs@sublate.org";

	/**
	 * Max time (in millis) the wake lock will be held for when background sync is happening
	 */
	public static final int WAKE_LOCK_TIMEOUT = 600000;

	public static final int MANUAL_WAKE_LOCK_TIMEOUT = 120000;


	public static final boolean DEBUG_LOGGING = false; //log all stanzas that were received while the app is in background


	private Config() {
	}

	private static String gpsLoggerFolder = "/SublateGps";
	public static String getGpsLoggerFolder() {
		return gpsLoggerFolder;
	}

	public static LocationDataHelper GPSDataBase;

}
