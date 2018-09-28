package com.sublate.xmppsensor.utils;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Pair;


import com.sublate.xmppsensor.R;
import com.sublate.xmppsensor.entities.ListItem;
import com.sublate.xmppsensor.entities.Presence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.sublate.xmppsensor.entities.Presence.Status.AWAY;
import static com.sublate.xmppsensor.entities.Presence.Status.CHAT;
import static com.sublate.xmppsensor.entities.Presence.Status.DND;
import static com.sublate.xmppsensor.entities.Presence.Status.XA;


public class UIHelper {

	private static String BLACK_HEART_SUIT = "\u2665";
	private static String HEAVY_BLACK_HEART_SUIT = "\u2764";
	private static String WHITE_HEART_SUIT = "\u2661";

	public static final ArrayList<String> HEARTS = new ArrayList<>(Arrays.asList(BLACK_HEART_SUIT,HEAVY_BLACK_HEART_SUIT,WHITE_HEART_SUIT));

	private static final ArrayList<String> LOCATION_QUESTIONS = new ArrayList<>(Arrays.asList(
			"where are you", //en
			"where are you now", //en
			"where are you right now", //en
			"whats your 20", //en
			"what is your 20", //en
			"what's your 20", //en
			"whats your twenty", //en
			"what is your twenty", //en
			"what's your twenty", //en
			"wo bist du", //de
			"wo bist du jetzt", //de
			"wo bist du gerade", //de
			"wo seid ihr", //de
			"wo seid ihr jetzt", //de
			"wo seid ihr gerade", //de
			"dónde estás", //es
			"donde estas" //es
		));

	private static final int SHORT_DATE_FLAGS = DateUtils.FORMAT_SHOW_DATE
		| DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_ABBREV_ALL;
	private static final int FULL_DATE_FLAGS = DateUtils.FORMAT_SHOW_TIME
		| DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE;

	public static String readableTimeDifference(Context context, long time) {
		return readableTimeDifference(context, time, false);
	}

	public static String readableTimeDifferenceFull(Context context, long time) {
		return readableTimeDifference(context, time, true);
	}

	private static String readableTimeDifference(Context context, long time,
			boolean fullDate) {
		if (time == 0) {
			return context.getString(R.string.just_now);
		}
		Date date = new Date(time);
		long difference = (System.currentTimeMillis() - time) / 1000;
		if (difference < 60) {
			return context.getString(R.string.just_now);
		} else if (difference < 60 * 2) {
			return context.getString(R.string.minute_ago);
		} else if (difference < 60 * 15) {
			return context.getString(R.string.minutes_ago,Math.round(difference / 60.0));
		} else if (today(date)) {
			java.text.DateFormat df = DateFormat.getTimeFormat(context);
			return df.format(date);
		} else {
			if (fullDate) {
				return DateUtils.formatDateTime(context, date.getTime(),
						FULL_DATE_FLAGS);
			} else {
				return DateUtils.formatDateTime(context, date.getTime(),
						SHORT_DATE_FLAGS);
			}
		}
	}

	private static boolean today(Date date) {
		return sameDay(date,new Date(System.currentTimeMillis()));
	}

	private static boolean sameDay(Date a, Date b) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(a);
		cal2.setTime(b);
		return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
			&& cal1.get(Calendar.DAY_OF_YEAR) == cal2
			.get(Calendar.DAY_OF_YEAR);
	}

	public static String lastseen(Context context, boolean active, long time) {
		long difference = (System.currentTimeMillis() - time) / 1000;
		active = active && difference <= 300;
		if (active || difference < 60) {
			return context.getString(R.string.last_seen_now);
		} else if (difference < 60 * 2) {
			return context.getString(R.string.last_seen_min);
		} else if (difference < 60 * 60) {
			return context.getString(R.string.last_seen_mins,
					Math.round(difference / 60.0));
		} else if (difference < 60 * 60 * 2) {
			return context.getString(R.string.last_seen_hour);
		} else if (difference < 60 * 60 * 24) {
			return context.getString(R.string.last_seen_hours,
					Math.round(difference / (60.0 * 60.0)));
		} else if (difference < 60 * 60 * 48) {
			return context.getString(R.string.last_seen_day);
		} else {
			return context.getString(R.string.last_seen_days,
					Math.round(difference / (60.0 * 60.0 * 24.0)));
		}
	}

	public static int getColorForName(String name) {
		if (name == null || name.isEmpty()) {
			return 0xFF202020;
		}
		int colors[] = {0xFFe91e63, 0xFF9c27b0, 0xFF673ab7, 0xFF3f51b5,
			0xFF5677fc, 0xFF03a9f4, 0xFF00bcd4, 0xFF009688, 0xFFff5722,
			0xFF795548, 0xFF607d8b};
		return colors[(int) ((name.hashCode() & 0xffffffffl) % colors.length)];
	}








	public static ListItem.Tag getTagForStatus(Context context, Presence.Status status) {
		switch (status) {
			case CHAT:
				return new ListItem.Tag(context.getString(R.string.presence_chat), 0xff259b24);
			case AWAY:
				return new ListItem.Tag(context.getString(R.string.presence_away), 0xffff9800);
			case XA:
				return new ListItem.Tag(context.getString(R.string.presence_xa), 0xfff44336);
			case DND:
				return new ListItem.Tag(context.getString(R.string.presence_dnd), 0xfff44336);
			default:
				return new ListItem.Tag(context.getString(R.string.presence_online), 0xff259b24);
		}
	}

	public static String tranlasteType(Context context, String type) {
		switch (type.toLowerCase()) {
			case "pc":
				return context.getString(R.string.type_pc);
			case "phone":
				return context.getString(R.string.type_phone);
			case "tablet":
				return context.getString(R.string.type_tablet);
			case "web":
				return context.getString(R.string.type_web);
			case "console":
				return context.getString(R.string.type_console);
			default:
				return type;
		}
	}
}
