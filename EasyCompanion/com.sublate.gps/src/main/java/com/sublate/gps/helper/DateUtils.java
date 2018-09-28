/*
 * Copyright (C) 2014-2016 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sublate.gps.helper;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Simple Date and time utils.
 */
public class DateUtils {
    /**
     * Calendar objects are rather expensive: for heavy usage it's a good idea to use a single instance per thread
     * instead of calling Calendar.getInstance() multiple times. Calendar.getInstance() creates a new instance each
     * time.
     */
    public static final class DefaultCalendarThreadLocal extends ThreadLocal<Calendar> {
        @Override
        protected Calendar initialValue() {
            return Calendar.getInstance();
        }
    }

    private static ThreadLocal<Calendar> calendarThreadLocal = new DefaultCalendarThreadLocal();

    public static long getTimeForDay(int year, int month, int day) {
        return getTimeForDay(calendarThreadLocal.get(), year, month, day);
    }

    /**
     * @param calendar helper object needed for conversion
     */
    public static long getTimeForDay(Calendar calendar, int year, int month, int day) {
        calendar.clear();
        calendar.set(year, month - 1, day);
        return calendar.getTimeInMillis();
    }

    /**
     * Sets hour, minutes, seconds and milliseconds to the given values. Leaves date info untouched.
     */
    public static void setTime(Calendar calendar, int hourOfDay, int minute, int second, int millisecond) {
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);
    }

    /**
     * Readable yyyyMMdd int representation of a day, which is also sortable.
     */
    public static int getDayAsReadableInt(long time) {
        Calendar cal = calendarThreadLocal.get();
        cal.setTimeInMillis(time);
        return getDayAsReadableInt(cal);
    }

    /**
     * Readable yyyyMMdd representation of a day, which is also sortable.
     */
    public static int getDayAsReadableInt(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        return year * 10000 + month * 100 + day;
    }

    /**
     * Returns midnight of the given day.
     */
    public static long getTimeFromDayReadableInt(int day) {
        return getTimeFromDayReadableInt(calendarThreadLocal.get(), day, 0);
    }

    /**
     * @param calendar helper object needed for conversion
     */
    public static long getTimeFromDayReadableInt(Calendar calendar, int readableDay, int hour) {
        int day = readableDay % 100;
        int month = readableDay / 100 % 100;
        int year = readableDay / 10000;

        calendar.clear(); // We don't set all fields, so we should clear the calendar first
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.YEAR, year);

        return calendar.getTimeInMillis();
    }

    public static int getDayDifferenceOfReadableInts(int dayOfBroadcast1, int dayOfBroadcast2) {
        long time1 = getTimeFromDayReadableInt(dayOfBroadcast1);
        long time2 = getTimeFromDayReadableInt(dayOfBroadcast2);

        // Don't use getDayDifference(time1, time2) here, it's wrong for some days.
        // Do float calculation and rounding at the end to cover daylight saving stuff etc.
        float daysFloat = (time2 - time1) / 1000 / 60 / 60 / 24f;
        return Math.round(daysFloat);
    }

    public static int getDayDifference(long time1, long time2) {
        return (int) ((time2 - time1) / 1000 / 60 / 60 / 24);
    }

    public static long addDays(long time, int days) {
        Calendar calendar = calendarThreadLocal.get();
        calendar.setTimeInMillis(time);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTimeInMillis();
    }

    public static void addDays(Calendar calendar, int days) {
        calendar.add(Calendar.DAY_OF_YEAR, days);
    }


    public static String dateToString1(Date date, String format) {
        String str = null;
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            str = dateFormat.format(date);
        }
        return str;
    }


    public static Date stringToDate1(String str, String format) {
        Date date = null;
        if (str != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            try {
                date = dateFormat.parse(str);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return date;
    }


    public static Calendar dateToCalendar(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }


    public static Date calendarToDate(Calendar calendar) {
        return calendar.getTime();
    }


    public static Calendar stringToCalendar(String str, String format) {
        Date date = stringToDate1(str, format);
        return dateToCalendar(date);
    }


    public static String calendarToString(Calendar calendar, String format) {
        Date date = calendarToDate(calendar);
        return dateToString1(date, format);
    }

    public static long getDateDifference(Calendar startDate, Calendar endDate) {
        startDate.set(Calendar.HOUR, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.AM_PM, 0);

        endDate.set(Calendar.HOUR, 0);
        endDate.set(Calendar.MINUTE, 0);
        endDate.set(Calendar.SECOND, 0);
        endDate.set(Calendar.AM_PM, 0);

        long milis1 = startDate.getTimeInMillis();
        long milis2 = endDate.getTimeInMillis();

        return getDateDifference(milis1, milis2);
    }

    public static long getDateDifference(long strtDate, long endDate) {
        long diff = endDate - strtDate;
        long diffDays = diff / (24 * 60 * 60 * 1000);

        return diffDays;
    }

    public static long getMonthDifference(Calendar startDate, Calendar endDate) {
        long monthDiff = endDate.get(Calendar.MONTH) - startDate.get(Calendar.MONTH) + (endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR)) * 12;
        return -monthDiff;
    }

    public static long getMinuteDifference(long startDate, long endDate) {
        long diff = endDate - startDate;
        long minutes = (diff / (1000 * 60));
        return minutes;
    }

    public static Calendar addMonth(Calendar cal, int numberOfMonth) {
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + numberOfMonth);
        return cal;
    }

    public static String getFormatedDateAsString(String strDate, String parseFormat) {
        return convertCalendarToString(convertStringToCalendar(strDate, "yyyy-MM-dd"), parseFormat);
    }

    @SuppressLint("SimpleDateFormat")
    public static String getFormatedDateAsStringwithTime(String strDate, String parseFormat) {

        Date date = null;
        SimpleDateFormat simpleDateFormat;
        try {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            date = simpleDateFormat.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String newString = new SimpleDateFormat(parseFormat).format(date);

        if (newString.endsWith("AM"))
            newString = newString.substring(0, newString.length() - 2) + "am";
        else if (newString.endsWith("PM"))
            newString = newString.substring(0, newString.length() - 2) + "pm";

        return newString;

    }

    @SuppressLint("SimpleDateFormat")
    public static String getDate(String strDate) {

        Date date = null;
        SimpleDateFormat simpleDateFormat;
        try {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            date = simpleDateFormat.parse(strDate);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        //need to change convertCalendarToString () method
        return new SimpleDateFormat("MMMMM dd, yyyy").format(date).toString();

    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SimpleDateFormat")
    public static Calendar convertStringToCalendar(String strDate, String parseFormat) {
        DateFormat formatter;
        Date date = null;
        formatter = new SimpleDateFormat(parseFormat);
        try {
            date = (Date) formatter.parse(strDate);
        } catch (Exception e) {
            Log.d("DateConversion", "Error parsing Date : " + strDate);
            date = new Date(strDate);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    @SuppressLint("SimpleDateFormat")
    public static String convertCalendarToString(Calendar cal, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(cal.getTime());
    }


    public static String convertCalendarToString(Calendar cal) {
        SimpleDateFormat df = new SimpleDateFormat("MMMMM dd, yyyy");
        return df.format(cal.getTime());
    }

    public static String getCurrentDateTime() {
        Calendar cal = Calendar.getInstance();

        int a = cal.get(Calendar.AM_PM);
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
        String newString = df.format(cal.getTime());
        if (a == Calendar.AM) {
            newString = newString + " am";
        } else if (a == Calendar.PM) {
            newString = newString + " pm";
        }
        return newString;
    }

    public static boolean compareWithCurrentDate(Calendar cal2) {
        Calendar cal1 = Calendar.getInstance();
        String date1 = convertCalendarToString(cal1, "yyyy-MM-dd");
        String date2 = convertCalendarToString(cal2, "yyyy-MM-dd");

        if (date1.equalsIgnoreCase(date2))
            return true;
        else
            return false;
    }

    @SuppressWarnings("deprecation")
    public static Date stringToDate2(String strDate, String parseFormat) {
        DateFormat formatter;
        Date date = null;
        formatter = new SimpleDateFormat(parseFormat);
        try {
            date = (Date) formatter.parse(strDate);
        } catch (ParseException e) {
            date = new Date(strDate);
        }
        return date;
    }

    public static String calendarToStringwithslash(Calendar cal) {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
        return df.format(cal.getTime());
    }

    public static Calendar getPreviousMonth(Calendar cal) {
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
        return cal;
    }

    public static Calendar getNextMonth(Calendar cal) {
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 1);
        return cal;
    }

    public static String getTimeFromLong(long time, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(time));
    }

	/*public static String getAKDTTimeFromDate(Date date, String format){
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
		return sdf.format(date);
	}

	public static String getISTTimeFromDate(Date date, String format){
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("IST"));
		return sdf.format(date);
	}*/

    public static String getTimeFromDate(Date date, String format) {
        return getTimeFromDate(date, format, TimeZone.getDefault());
    }

    public static String getTimeFromDate(Date date, String format, TimeZone timeZone) {

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        if (timeZone != null) {
            sdf.setTimeZone(timeZone);
        }
        return sdf.format(date);

    }


    /**
     * The time(long) value is seconds not millis
     *
     * @param timeZone String representation of time format
     * @param time     time as long value in seconds
     * @return time  time as long in seconds
     */



    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static Date date1, date2;
    public static long diff;
    public static String TAG = "DateConversion";

    public static boolean isAfter(String currentDate, String checkDate) {

        try {

            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            date1 = simpleDateFormat.parse(currentDate);
            date2 = simpleDateFormat.parse(checkDate);

            //in milliseconds
            diff = date2.getTime() - date1.getTime();

//			PrintLog.debug(TAG, "Difference: "+diff);
            if (diff <= 0) {
                return false;
            } else {
                return true;
            }


//			long diffSeconds = diff / 1000 % 60;
//			long diffMinutes = diff / (60 * 1000) % 60;
//			long diffHours = diff / (60 * 60 * 1000) % 24;
//			long diffDays = diff / (24 * 60 * 60 * 1000);
//			System.out.print(diffDays 	+ " days, ");
//			System.out.print(diffHours 	+ " hours, ");
//			System.out.print(diffMinutes+ " minutes, ");
//			System.out.print(diffSeconds+ " seconds.");

        } catch (Exception e) {
            //PrintLog.error(TAG, "" + e);
            return true;
        }
    }

    public static final String yyyyMMDD = "yyyy-MM-dd";
    public static final String yyyyMMddHHmmss = "yyyy-MM-dd HH:mm:ss";
    public static final String HHmmss = "HH:mm:ss";
    public static final String hhmmss = "HH:mm:ss";
    public static final String LOCALE_DATE_FORMAT = "yyyy年M月d日 HH:mm:ss";
    public static final String DB_DATA_FORMAT = "yyyy-MM-DD HH:mm:ss";
    public static final String NEWS_ITEM_DATE_FORMAT = "hh:mm M月d日 yyyy";


    public static String dateToString(Date date, String pattern)
            throws Exception {
        return new SimpleDateFormat(pattern).format(date);
    }

    public static Date stringToDate(String dateStr, String pattern)
            throws Exception {
        return new SimpleDateFormat(pattern).parse(dateStr);
    }

    /**
     * 将Date类型转换为日期字符串
     *
     * @param date Date对象
     * @param type 需要的日期格式
     * @return 按照需求格式的日期字符串
     */
    public static String formatDate(Date date, String type) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(type);
            return df.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将日期字符串转换为Date类型
     *
     * @param dateStr 日期字符串
     * @param type    日期字符串格式
     * @return Date对象
     */
    public static Date parseDate(String dateStr, String type) {
        SimpleDateFormat df = new SimpleDateFormat(type);
        Date date = null;
        try {
            date = df.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;

    }

    /**
     * 得到年
     *
     * @param date Date对象
     * @return 年
     */
    public static int getYear(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR);
    }

    /**
     * 得到月
     *
     * @param date Date对象
     * @return 月
     */
    public static int getMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.MONTH) + 1;

    }

    /**
     * 得到日
     *
     * @param date Date对象
     * @return 日
     */
    public static int getDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 转换日期 将日期转为今天, 昨天, 前天, XXXX-XX-XX, ...
     *
     * @param time 时间
     * @return 当前日期转换为更容易理解的方式
     */
    public static String translateDate(Long time) {
        long oneDay = 24 * 60 * 60 * 1000;
        Calendar current = Calendar.getInstance();
        Calendar today = Calendar.getInstance();    //今天

        today.set(Calendar.YEAR, current.get(Calendar.YEAR));
        today.set(Calendar.MONTH, current.get(Calendar.MONTH));
        today.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
        //  Calendar.HOUR——12小时制的小时数 Calendar.HOUR_OF_DAY——24小时制的小时数
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        long todayStartTime = today.getTimeInMillis();

        if (time >= todayStartTime && time < todayStartTime + oneDay) { // today
            return "今天";
        } else if (time >= todayStartTime - oneDay && time < todayStartTime) { // yesterday
            return "昨天";
        } else if (time >= todayStartTime - oneDay * 2 && time < todayStartTime - oneDay) { // the day before yesterday
            return "前天";
        } else if (time > todayStartTime + oneDay) { // future
            return "将来某一天";
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date(time);
            return dateFormat.format(date);
        }
    }

    /**
     * 转换日期 转换为更为人性化的时间
     *
     * @param time 时间
     * @return
     */
    private String translateDate(long time, long curTime) {
        long oneDay = 24 * 60 * 60;
        Calendar today = Calendar.getInstance();    //今天
        today.setTimeInMillis(curTime * 1000);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        long todayStartTime = today.getTimeInMillis() / 1000;
        if (time >= todayStartTime) {
            long d = curTime - time;
            if (d <= 60) {
                return "1分钟前";
            } else if (d <= 60 * 60) {
                long m = d / 60;
                if (m <= 0) {
                    m = 1;
                }
                return m + "分钟前";
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("今天 HH:mm");
                Date date = new Date(time * 1000);
                String dateStr = dateFormat.format(date);
                if (!TextUtils.isEmpty(dateStr) && dateStr.contains(" 0")) {
                    dateStr = dateStr.replace(" 0", " ");
                }
                return dateStr;
            }
        } else {
            if (time < todayStartTime && time > todayStartTime - oneDay) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("昨天 HH:mm");
                Date date = new Date(time * 1000);
                String dateStr = dateFormat.format(date);
                if (!TextUtils.isEmpty(dateStr) && dateStr.contains(" 0")) {

                    dateStr = dateStr.replace(" 0", " ");
                }
                return dateStr;
            } else if (time < todayStartTime - oneDay && time > todayStartTime - 2 * oneDay) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("前天 HH:mm");
                Date date = new Date(time * 1000);
                String dateStr = dateFormat.format(date);
                if (!TextUtils.isEmpty(dateStr) && dateStr.contains(" 0")) {
                    dateStr = dateStr.replace(" 0", " ");
                }
                return dateStr;
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date date = new Date(time * 1000);
                String dateStr = dateFormat.format(date);
                if (!TextUtils.isEmpty(dateStr) && dateStr.contains(" 0")) {
                    dateStr = dateStr.replace(" 0", " ");
                }
                return dateStr;
            }
        }
    }

    /**
     * Returns timestamp given by param in KML format ie yyyy-mm-ddThh:mm:ssZ,
     * where T is the separator between the date and the time and the time zone
     * is Z (for UTC)
     *
     * @return KML timestamp as String
     */
    public static String getKMLTimestamp(long when) {
        TimeZone tz = TimeZone.getTimeZone("GMT-5");
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(when);
        return String.format("%tY-%tm-%tdT%tH:%tM:%tSZ", c, c, c, c, c, c);
    }

    /**
     * Helper version of getKMLTimestamp, that returns timestamp for current
     * time
     */
    public static String getCurrentKMLTimestamp() {
        return getKMLTimestamp(System.currentTimeMillis());
    }

    /**
     * Returns timestamp in following format: yyyy-mm-dd-hh-mm-ss
     */
    public static String getCurrentTimestamp() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        return String.format("%tY-%tm-%td-%tH-%tM-%tS", c, c, c, c, c, c);
    }

}
