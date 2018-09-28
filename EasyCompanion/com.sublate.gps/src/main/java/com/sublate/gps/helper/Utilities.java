/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.sublate.gps.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.text.format.Time;

import com.sublate.gps.abstracts.IMessageBoxCallback;

import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class Utilities {

    private static final int LOGLEVEL = 5;
    private static ProgressDialog pd;


    public static void ShowProgress(Context ctx, String title, String message) {
        if (ctx != null) {
            pd = new ProgressDialog(ctx, ProgressDialog.STYLE_HORIZONTAL);
            pd.setMax(100);
            pd.setIndeterminate(true);

            pd = ProgressDialog.show(ctx, title, message, true, true);
        }
    }

    public static void HideProgress() {
        if (pd != null) {
            pd.dismiss();
        }
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param className The calling class, such as GpsMainActivity.this or
     *                  mainActivity.
     */
    public static void MsgBox(String title, String message, Context className) {
        MsgBox(title, message, className, null);
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param className   The calling class, such as GpsMainActivity.this or
     *                    mainActivity.
     * @param msgCallback An object which implements IHasACallBack so that the
     *                    click event can call the callback method.
     */
    private static void MsgBox(String title, String message, Context className,
                               final IMessageBoxCallback msgCallback) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(className);
        alertBuilder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(className.getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {

                            public void onClick(final DialogInterface dialog,
                                                final int which) {

                                if (msgCallback != null) {
                                    msgCallback.MessageBoxResult(which);
                                }
                            }
                        }
                );

        AlertDialog alertDialog = alertBuilder.create();

        if (className instanceof Activity && !((Activity) className).isFinishing()) {
            alertDialog.show();
        } else {
            alertDialog.show();
        }

    }


    /**
     * Makes string safe for writing to XML file. Removes lt and gt. Best used
     * when writing to file.
     *
     * @param desc
     * @return
     */
    public static String CleanDescription(String desc) {
        desc = desc.replace("<", "");
        desc = desc.replace(">", "");
        desc = desc.replace("&", "&amp;");
        desc = desc.replace("\"", "&quot;");

        return desc;
    }


    /**
     * Given a Date object, returns an ISO 8601 date time string in UTC.
     * Example: 2010-03-23T05:17:22Z but not 2010-03-23T05:17:22+04:00
     *
     * @param dateToFormat The Date object to format.
     * @return The ISO 8601 formatted string.
     */
    public static String GetIsoDateTime(Date dateToFormat) {
        /**
         * This function is used in gpslogger.loggers.* and for most of them the
         * default locale should be fine, but in the case of HttpUrlLogger we
         * want machine-readable output, thus  Locale.US.
         *
         * Be wary of the default locale
         * http://developer.android.com/reference/java/util/Locale.html#default_locale
         */

        // GPX specs say that time given should be in UTC, no local time.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.format(dateToFormat);
    }

    public static String GetReadableDateTime(Date dateToFormat) {
        /**
         * Similar to GetIsoDateTime(), this function is used in
         * AutoEmailHelper, and we want machine-readable output.
         */
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm",
                Locale.US);
        return sdf.format(dateToFormat);
    }

    /**
     * Converts given meters to feet.
     *
     * @param m
     * @return
     */
    public static int MetersToFeet(int m) {
        return (int) Math.round(m * 3.2808399);
    }


    /**
     * Converts given feet to meters
     *
     * @param f
     * @return
     */
    public static int FeetToMeters(int f) {
        return (int) Math.round(f / 3.2808399);
    }


    /**
     * Converts given meters to feet and rounds up.
     *
     * @param m
     * @return
     */
    public static int MetersToFeet(double m) {
        return MetersToFeet((int) m);
    }

    /**
     * Uses the Haversine formula to calculate the distnace between to lat-long coordinates
     *
     * @param latitude1  The first point's latitude
     * @param longitude1 The first point's longitude
     * @param latitude2  The second point's latitude
     * @param longitude2 The second point's longitude
     * @return The distance between the two points in meters
     */
    public static double CalculateDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        /*
            Haversine formula:
            A = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
            C = 2.atan2(√a, √(1−a))
            D = R.c
            R = radius of earth, 6371 km.
            All angles are in radians
            */

        double deltaLatitude = Math.toRadians(Math.abs(latitude1 - latitude2));
        double deltaLongitude = Math.toRadians(Math.abs(longitude1 - longitude2));
        double latitude1Rad = Math.toRadians(latitude1);
        double latitude2Rad = Math.toRadians(latitude2);

        double a = Math.pow(Math.sin(deltaLatitude / 2), 2) +
                (Math.cos(latitude1Rad) * Math.cos(latitude2Rad) * Math.pow(Math.sin(deltaLongitude / 2), 2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c * 1000; //Distance in meters

    }


    /**
     * Checks if a string is null or empty
     *
     * @param text
     * @return
     */
    public static boolean IsNullOrEmpty(String text) {
        return text == null || text.length() == 0;
    }


    public static byte[] GetByteArrayFromInputStream(InputStream is) {

        try {
            int length;
            int size = 1024;
            byte[] buffer;

            if (is instanceof ByteArrayInputStream) {
                size = is.available();
                buffer = new byte[size];
                is.read(buffer, 0, size);
            } else {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                buffer = new byte[size];
                while ((length = is.read(buffer, 0, size)) != -1) {
                    outputStream.write(buffer, 0, length);
                }

                buffer = outputStream.toByteArray();
            }
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                //tracer.warn("GetStringFromInputStream - could not close stream");
            }
        }

        return null;

    }

    /**
     * Loops through an input stream and converts it into a string, then closes the input stream
     *
     * @param is
     * @return
     */
    public static String GetStringFromInputStream(InputStream is) {
        String line;
        StringBuilder total = new StringBuilder();

        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        // Read response until the end
        try {
            while ((line = rd.readLine()) != null) {
                total.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                //tracer.warn("GetStringFromInputStream - could not close stream");
            }
        }

        // Return full string
        return total.toString();
    }


    /**
     * Converts an input stream containing an XML response into an XML Document object
     *
     * @param stream
     * @return
     */
    public static Document GetDocumentFromInputStream(InputStream stream) {
        Document doc;

        try {
            DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            DocumentBuilder builder = xmlFactory.newDocumentBuilder();
            doc = builder.parse(stream);
        } catch (Exception e) {
            doc = null;
        }

        return doc;
    }

    /**
     * Gets the GPSLogger-specific MIME type to use for a given filename/extension
     *
     * @param fileName
     * @return
     */
    public static String GetMimeTypeFromFileName(String fileName) {

        if (fileName == null || fileName.length() == 0) {
            return "";
        }


        int pos = fileName.lastIndexOf(".");
        if (pos == -1) {
            return "application/octet-stream";
        } else {

            String extension = fileName.substring(pos + 1, fileName.length());


            if (extension.equalsIgnoreCase("gpx")) {
                return "application/gpx+xml";
            } else if (extension.equalsIgnoreCase("kml")) {
                return "application/vnd.google-earth.kml+xml";
            } else if (extension.equalsIgnoreCase("zip")) {
                return "application/zip";
            }
        }

        //Unknown mime type
        return "application/octet-stream";

    }

    public static float GetBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    public static String GetAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

    }


    public static String HtmlDecode(String text) {
        if (IsNullOrEmpty(text)) {
            return text;
        }

        return text.replace("&amp;", "&").replace("&quot;", "\"");
    }

    public static String GetBuildSerial() {
        try {
            return Build.SERIAL;
        } catch (Throwable t) {
            return "";
        }
    }

    public static File[] GetFilesInFolder(File folder) {
        return GetFilesInFolder(folder, null);
    }

    public static File[] GetFilesInFolder(File folder, FilenameFilter filter) {

        if (folder == null || !folder.exists() || folder.listFiles() == null) {
            return new File[]{};
        } else {
            if (filter != null) {
                return folder.listFiles(filter);
            }
            return folder.listFiles();
        }
    }


    public static String GetFormattedCustomFileName(String baseName) {

        Time t = new Time();
        t.setToNow();

        String finalFileName = baseName;
        finalFileName = finalFileName.replaceAll("(?i)%ser", String.valueOf(Utilities.GetBuildSerial()));
        finalFileName = finalFileName.replaceAll("(?i)%hour", String.valueOf(t.hour));
        finalFileName = finalFileName.replaceAll("(?i)%min", String.valueOf(t.minute));
        finalFileName = finalFileName.replaceAll("(?i)%year", String.valueOf(t.year));
        finalFileName = finalFileName.replaceAll("(?i)%month", String.valueOf(t.month));
        finalFileName = finalFileName.replaceAll("(?i)%day", String.valueOf(t.monthDay));
        return finalFileName;

    }


}
