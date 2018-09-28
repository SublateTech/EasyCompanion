package eu.siacs.conversations.utils;

import android.content.Context;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

public class ExceptionHelper {
	private static SimpleDateFormat DATE_FORMATs = new SimpleDateFormat("yyyy-MM-dd");
	public static void init(Context context) {
		if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(
					context));
		}
	}



	public static void writeToStacktraceFile(Context context, String msg) {
		try {
			OutputStream os = context.openFileOutput("stacktrace.txt", Context.MODE_PRIVATE);
			os.write(msg.getBytes());
			os.flush();
			os.close();
		} catch (IOException ignored) {
		}
	}
}
