package com.sublate.gps.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sublate.gps.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class Preferences {

    private static final String TAG = "Preferences";
    public static final String RESET_PREF = "reset_preference";
    private static final String TICK_CHECK_SERVICES = "TickToCheck_preference";
    private static final String START_STOP_SERVICE_PREF = "startStopService";


    private static Preferences mPreferences;
    private static SharedPreferences mSharedPreferences;
    private Context mContext;

    public static synchronized Preferences getPreferences(Context context) {
        Context appContext = context.getApplicationContext();

        if (mPreferences == null) {
            mPreferences = new Preferences(appContext);
        }
        return mPreferences;
    }
    private Preferences(Context context) {
        mContext = context;
        if (mSharedPreferences==null)
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }
    public static SharedPreferences getPreferences() {
        return mSharedPreferences;
    }


    private static boolean autoSendEnabled = false;
    public static boolean isAutoSendEnabled() {
        return autoSendEnabled;
    }


    public boolean getResetStatus() {
        return getPreferences().getBoolean(RESET_PREF, false);
    }
    public void resetPreferences()
    {
        Editor editor = getPreferences().edit();
        editor.clear();
        editor.commit();
    }
    public void setStartStopService(boolean enabled) {
        putBoolean(START_STOP_SERVICE_PREF, enabled);
    }
    public boolean getStartStopService() {
        return getPreferences().getBoolean(START_STOP_SERVICE_PREF, false);
    }

    public void put(String key, String value) {//Log.v("Keystore","PUT "+key+" "+value);
        Editor editor;

        editor = getPreferences().edit();
        editor.putString(key, value);
        editor.commit(); // Stop everything and do an immediate save!
        // editor.apply();//Keep going and save when you are not busy - Available only in APIs 9 and above.  This is the preferred way of saving.
    }
    public String get(String key) {//Log.v("Keystore","GET from "+key);
        return getPreferences().getString(key, null);

    }
    public Boolean getBoolean(String key, Boolean value) {//Log.v("Keystore","GET from "+key);
        return getPreferences().getBoolean(key, value);

    }
    public void putBoolean(String key, Boolean value) {//Log.v("Keystore","PUT "+key+" "+value);
        Editor editor;

        editor = getPreferences().edit();
        editor.putBoolean(key, value);
        editor.commit(); // Stop everything and do an immediate save!
        // editor.apply();//Keep going and save when you are not busy - Available only in APIs 9 and above.  This is the preferred way of saving.
    }
    public String getString(String key) {//Log.v("Keystore","GET from "+key);
        return getPreferences().getString(key, null);

    }
    public static void putString(String key, String value) {//Log.v("Keystore","PUT "+key+" "+value);
        Editor editor;

        editor = getPreferences().edit();
        editor.putString(key, value);
        editor.commit(); // Stop everything and do an immediate save!
        // editor.apply();//Keep going and save when you are not busy - Available only in APIs 9 and above.  This is the preferred way of saving.
    }
    public int getInt(String key) {//Log.v("Keystore","GET INT from "+key);
        return getPreferences().getInt(key, 0);
    }
    public void putInt(String key, int num) {//Log.v("Keystore","PUT INT "+key+" "+String.valueOf(num));
        Editor editor;
        editor = getPreferences().edit();

        editor.putInt(key, num);
        editor.commit();
    }
    public void clear(){
        Editor editor;
        editor = getPreferences().edit();

        editor.clear();
        editor.commit();
    }
    public void remove(){
        Editor editor;
        editor = getPreferences().edit();

        //editor.remove(filename);
        editor.commit();
    }


    public static class Gps {

        // preference constants
        private static final String MIN_TIME_PREF = "mintime_preference";
        private static final String MIN_DIS_PREF = "mindistance_preference";
        private static final String GPS_PREF = "gps_preference";
        private static final String NETWORK_PREF = "network_preference";
        private static final String SIGNAL_PREF = "signal_preference";
        private static final String DEBUG_PREF = "advanced_log_preference";
        private static final String GPS_UPDATES_PREF = "gps_updates_preference";

        public boolean trackNetwork() {
            return getPreferences().getBoolean(NETWORK_PREF, true);
        }

        public boolean trackGPS() {
            return getPreferences().getBoolean(GPS_PREF, true);
        }

        public boolean doDebugLogging() {
            return getPreferences().getBoolean(DEBUG_PREF, false);
        }

        public boolean trackSignalStrength() {
            return getPreferences().getBoolean(SIGNAL_PREF, false);
        }


        public float getLocationMinDistance() {
            try {
                String disString = getPreferences().getString(MIN_DIS_PREF, "0");
                return Float.parseFloat(disString);
            } catch (NumberFormatException e) {
                Log.e("", "Invalid preference for location min distance", e);
            }
            return 0;
        }

        public static int getGpsUpdates() {
            try {
                String disString = getPreferences().getString(GPS_UPDATES_PREF, "10");
                return Integer.parseInt(disString) * 1000 * 60;
            } catch (NumberFormatException e) {
                Log.e("", "Invalid preference for GPS Updates", e);
            }
            return 0;
        }

        public int getTickCheckService() {
            try {
                String disString = getPreferences().getString(TICK_CHECK_SERVICES, "5");
                return Integer.parseInt(disString) * 1000 * 60;
            } catch (NumberFormatException e) {
                Log.e("", "Invalid preference tick check service", e);
            }
            return 0;
        }

        public long getLocationUpdateTime() {
            try {
                String timeString = getPreferences().getString(MIN_TIME_PREF, "0");
                long secondsTime = Long.valueOf(timeString);
                return secondsTime * 1000;
            } catch (NumberFormatException e) {
                Log.e("", "Invalid preference for location min time", e);
            }
            return 0;
        }
    }

    public static class User {

        public static final String USER = "user";
        public static final String USER_NAME = "user_name";
        public static final String USER_PHONE = "user_phone";
        public static final String USER_EMAIL = "user_email";
        public static final String USER_TYPE = "user_type";

        public static  String getUserName() {
            return getPreferences().getString(USER_NAME, "");
        }
    }

    public void loadPresetProperties() {

        //Either look for /sdcard/GPSLogger/gpslogger.properties or /sdcard/gpslogger.properties
        //Environment.getExternalStorageDirectory() +

        File file =  new File( Config.getGpsLoggerFolder() + "/gps.properties");
        if(!file.exists()){
          //  file = new File(Environment.getExternalStorageDirectory() + "/gps.properties");
          //  if(!file.exists()){
                return;
           // }
        }

        try {
            Properties props = new Properties();
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            props.load(reader);

            for(Object key : props.keySet()){

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                Editor editor = prefs.edit();

                String value = props.getProperty(key.toString());
                Log.d("Preferences","Setting preset property: " + key.toString() + " to " + value.toString());

                if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")){
                    editor.putBoolean(key.toString(), Boolean.parseBoolean(value));
                }
                else {
                    editor.putString(key.toString(), value);
                }
                editor.commit();
            }

        } catch (Exception e) {
            //tracer.error("Could not load preset properties", e);
        }

    }

    public static class eMail {
        public static boolean IsEmailSetup() {
            return isAutoEmailEnabled()
                    && getAutoEmailTargets().length() > 0
                    && getSmtpServer().length() > 0
                    && getSmtpPort().length() > 0
                    && getSmtpUsername().length() > 0;

        }

        public static String getAutoEmailTargets() {
            return Preferences.mSharedPreferences.getString("autoemail_target", "");
        }

        /**
         * @return the autoEmailEnabled
         */
        public static boolean isAutoEmailEnabled() {
            return Preferences.mSharedPreferences.getBoolean("autoemail_enabled", false);
        }

        public static String getSmtpServer() {
            return Preferences.mSharedPreferences.getString("smtp_server", "");
        }

        public static String getSmtpPort() {
            return Preferences.mSharedPreferences.getString("smtp_port", "");
        }

        public static String getSmtpUsername() {
            return Preferences.mSharedPreferences.getString("smtp_username", "");
        }

        public static String getSmtpPassword() {
            return Preferences.mSharedPreferences.getString("smtp_password", "");
        }

        /**
         * Returns the from value to use when sending an email
         *
         * @return
         */
        public static String getSenderAddress() {
            if (getSmtpFrom() != null && getSmtpFrom().length() > 0) {
                return getSmtpFrom();
            }

            return getSmtpUsername();
        }

        private static String getSmtpFrom() {
            return Preferences.mSharedPreferences.getString("smtp_from", "");
        }

        public static boolean isSmtpSsl() {
            return Preferences.mSharedPreferences.getBoolean("smtp_ssl", false);
        }
    }

    public static class Ftp {

        private static boolean autoFtpEnabled;
        private static int ftpPort;

        public static void setAutoFtpEnabled(boolean m_autoFtpEnabled) {
            autoFtpEnabled = m_autoFtpEnabled;

        }
        public static boolean isAutoFtpEnabled() {
            return autoFtpEnabled;
        }


        public static int getFtpPort() {
            ftpPort = 21; // Preferences.mSharedPreferences.getInt("autoftp_port", 21);
            return ftpPort;
        }

        public static String getFtpUsername() {
            return Preferences.mSharedPreferences.getString("autoftp_username", "");
        }

        public static String getFtpDirectory() {
            return Preferences.mSharedPreferences.getString("autoftp_directory", "");
        }

        public static String getFtpPassword() {
            return Preferences.mSharedPreferences.getString("autoftp_password", "");
        }

        public static String getFtpProtocol() {
            return Preferences.mSharedPreferences.getString("autoftp_protocol", "POP3");
        }

        public static boolean FtpImplicit() {
            return Preferences.mSharedPreferences.getBoolean("autoftp_implicit", false);
        }

        public static String getFtpServerName() {
            return Preferences.mSharedPreferences.getString("autoftp_server", "");
        }

        public static boolean FtpUseFtps() {
            return Preferences.mSharedPreferences.getBoolean("autoftp_useftps", false);
        }

    }

    public static class Export {
        public static double getPrefAltitudeCorrection() {
            return prefAltitudeCorrection;
        }

        public static boolean getPrefEGM96AltitudeCorrection() {
            return prefEGM96AltitudeCorrection;
        }

        public static int getPrefKMLAltitudeMode() {
            return prefKMLAltitudeMode;
        }

        public static boolean getPrefShowDecimalCoordinates() {
            return prefShowDecimalCoordinates;
        }

        public static int getPrefUM() {
            return prefUM;
        }

        public static int getPrefShowDirections() {
            return prefShowDirections;
        }

        private static boolean prefEGM96AltitudeCorrection = false;
        private static double prefAltitudeCorrection = 0d;
        private static int prefKMLAltitudeMode = 0;
        private static boolean prefShowDecimalCoordinates = false;
        private static int prefUM = 1; //UM_METRIC_KMH;
        private static int UM_METRIC_KMH = 1;
        private static int prefShowDirections = 0;
    }

    public static class OpenGTS {
        public static boolean isOpenGTSEnabled() {
            return getPreferences().getBoolean("opengts_enabled", false);
        }

        public static boolean isAutoOpenGTSEnabled() {
            return getPreferences().getBoolean("autoopengts_enabled", false);
        }

        public static String getOpenGTSServer() {
            return getPreferences().getString("opengts_server", null);
        }

        public static String getOpenGTSServerPort() {
            return getPreferences().getString("opengts_server_port", null);
        }

        public static String getOpenGTSServerCommunicationMethod() {
            return getPreferences().getString("opengts_server_communication_method", null);
        }

        public static String getOpenGTSServerPath() {
            return getPreferences().getString("autoopengts_server_path", null);

        }

        public static String getOpenGTSDeviceId() {
            return getPreferences().getString("opengts_device_id", null);
        }

        public static String getOpenGTSAccountName() {
            return getPreferences().getString("opengts_accountname", null);
        }
    }
}
