package com.sublate.gps.exporters.gpx;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.compat.BuildConfig;
import android.util.Log;


import com.sublate.gps.Config;
import com.sublate.gps.abstracts.IActionListener;
import com.sublate.gps.exporters.EGM96;
import com.sublate.gps.exporters.LocationExtended;
import com.sublate.gps.helper.ProgressBarNotification;
import com.sublate.gps.model.Route;
import com.sublate.gps.preferences.Preferences;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Base class to writes a GPX file and export
 * track media (Photos, Sounds)
 * 
 * @author Nicolas Guillaumin
 *
 */
public abstract class  ExportRouteTask extends AsyncTask<Void, Long, Boolean> implements IActionListener {

	private static final String TAG = ExportRouteTask.class.getSimpleName();

	public static final int NOT_AVAILABLE = -100000;

	Route route = null;
	int GroupOfLocations = 200; // Reads and writes location grouped by 200;
	boolean ExportKML = true;
	boolean ExportGPX = true;
	private String SaveIntoFolder = "/";
	double AltitudeManualCorrection = 0;
	boolean EGMAltitudeCorrection = false;
	int getPrefKMLAltitudeMode = 0;

	String versionName = BuildConfig.VERSION_NAME;


	private String filename;

	/**
	 * {@link Context} to get resources
	 */
	protected Context context;

	/**
	 * Point IDs to export
	 */
	protected long[] trackIds;

	/**
	 * Dialog to display while exporting
	 */


	protected ProgressBarNotification mProgressBar;

	/**
	 * Message in case of an error
	 */
	private String errorMsg = null;


	public ExportRouteTask(Context context, long... trackIds) {
		this.context = context;
		this.trackIds = trackIds;


		AltitudeManualCorrection = Preferences.Export.getPrefAltitudeCorrection();
		EGMAltitudeCorrection = Preferences.Export.getPrefEGM96AltitudeCorrection();
		getPrefKMLAltitudeMode = Preferences.Export.getPrefKMLAltitudeMode();

		this.ExportGPX = ExportGPX;
		this.ExportKML = ExportKML;
		this.SaveIntoFolder = Config.getGpsLoggerFolder(); //SaveIntoFolder;

		mProgressBar = new ProgressBarNotification(context,"Upload",1);
	}

	@Override
	protected void onPreExecute() {
		// Display dialog
		/*
		dialog = new ProgressDialog(context);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.setMessage(context.getResources().getString(R.string.trackmgr_exporting_prepare));
		dialog.show();
		*/

		mProgressBar.PreExecute();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			for (int i=0; i<trackIds.length; i++) {
				exportTrackAsGpx(trackIds[i],ExportRouteTask.this);

			}
		} catch (ExportTrackException ete) {
			errorMsg = ete.getMessage();
			return false;
		}
		return true;
	}

	@Override
	protected void onProgressUpdate(Long... values) {
		/*
		if (values.length == 1) {
			// Standard progress update
			dialog.incrementProgressBy(values[0].intValue());
		} else if (values.length == 3) {
			// To initialise the dialog, 3 values are passed to onProgressUpdate()
			// trackId, number of track points, number of waypoints
			dialog.dismiss();

			dialog = new ProgressDialog(context);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(false);
			dialog.setCancelable(false);
			dialog.setProgress(0);
			dialog.setMax(values[1].intValue() + values[2].intValue());
			dialog.setTitle(
					context.getResources().getString(R.string.trackmgr_exporting)
					.replace("{0}", Long.toString(values[0])));
			dialog.show();




		}*/

		mProgressBar.ProgressUpdate(values[0].intValue());
	}

	@Override
	protected void onPostExecute(Boolean success) {
		//dialog.dismiss();
		if (!success) {
			/*
			new AlertDialog.Builder(context)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(context.getResources()
						.getString(R.string.trackmgr_export_error)
						.replace("{0}", errorMsg))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
			*/


		}
		mProgressBar.PostExecute(success);
		OnComplete();
	}


	private void exportTrackAsGpx(long routeId, ExportRouteTask This) throws ExportTrackException
	{
		long v=0;
		route = Config.GPSDataBase.getRoute(routeId);
		// ------------------------------------------------- Create the Directory tree if not exist

		/*
		File sd = new File(Environment.getExternalStorageDirectory() + "/kml");
		if (!sd.exists()) {
			sd.mkdir();
		}

		sd = new File(Environment.getExternalStorageDirectory() + "/kml/AppData");
		if (!sd.exists()) {
			sd.mkdir();
		}
		*/

		// ----------------------------------------------------------------------------------------

		if (route == null) {
			Log.w("myApp", "[#] Exporter.java - Point = null!!");
			return;
		}
		if (route.getNumberOfLocations() + route.getNumberOfPlacemarks() == 0) return;

		//EventBus.getDefault().post("TRACK_SETPROGRESS " + route.getId() + " 1");
		this.publishProgress(1L);


		if (EGMAltitudeCorrection && EGM96.getInstance().isEGMGridLoading()) {
			try {
				Log.w("myApp", "[#] Exporter.java - Wait, EGMGrid is loading");
				do {
					Thread.sleep(200);
					// Lazy polling until EGM grid finish to load
				} while (EGM96.getInstance().isEGMGridLoading());
			} catch (InterruptedException e) {
				Log.w("myApp", "[#] Exporter.java - Cannot wait!!");
			}
		}

		SimpleDateFormat dfdate = new SimpleDateFormat("yyyy-MM-dd");          // date formatter for GPX timestamp
		SimpleDateFormat dftime = new SimpleDateFormat("HH:mm:ss");            // time formatter for GPX timestamp
		File KMLfile = null;
		File GPXfile = null;
		final String newLine = System.getProperty("line.separator");

		// Verify if Folder exists
		File sd = new File(Environment.getExternalStorageDirectory() + SaveIntoFolder);
		boolean success = true;
		if (!sd.exists()) {
			success = sd.mkdir();
		}
		if (!success) return;

		// Create files, deleting old version if exists
		if (ExportKML) {
			KMLfile = new File(sd,  route.getFileName() + ".kml");
			if (KMLfile.exists()) KMLfile.delete();
		}
		if (ExportGPX) {
			filename = route.getFileName() + ".gpx";
			GPXfile = new File(sd, (route.getFileName() + ".gpx"));
			if (GPXfile.exists()) GPXfile.delete();
		}

		// Create buffers for Write operations
		PrintWriter KMLfw = null;
		BufferedWriter KMLbw = null;
		PrintWriter GPXfw = null;
		BufferedWriter GPXbw = null;


		try {
			if (ExportKML) {
				KMLfw = new PrintWriter(KMLfile);
				KMLbw = new BufferedWriter(KMLfw);
			}
			if (ExportGPX) {
				GPXfw = new PrintWriter(GPXfile);
				GPXbw = new BufferedWriter(GPXfw);
			}

			// ---------------------------------------------------------------------- Writing Heads
			Log.w("myApp", "[#] Exporter.java - Writing Heads");

			if (ExportKML) {
				// Writing head of KML file

				KMLbw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newLine);
				KMLbw.write("<!-- Created with Sublate Easy Companion for Android - ver. " + versionName + " -->" + newLine);
				KMLbw.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">" + newLine);
				KMLbw.write(" <Document>" + newLine);
				KMLbw.write("  <name>Paths</name>" + newLine);
				KMLbw.write("  <description>Ruta: " + route.getName() + ":" + route.getDateStamp() + newLine);
				KMLbw.write("  </description>" + newLine);

				KMLbw.write("  <Style id=\"TrackLineAndPoly\">" + newLine);
				KMLbw.write("   <LineStyle>" + newLine);
				KMLbw.write("    <color>ff0000ff</color>" + newLine);
				KMLbw.write("    <width>4</width>" + newLine);
				KMLbw.write("   </LineStyle>" + newLine);
				KMLbw.write("   <PolyStyle>" + newLine);
				KMLbw.write("    <color>7f0000ff</color>" + newLine);
				KMLbw.write("   </PolyStyle>" + newLine);
				KMLbw.write("  </Style>" + newLine);

				KMLbw.write("  <Style id=\"Bookmark_Style\">" + newLine);
				KMLbw.write("   <IconStyle>" + newLine);
				KMLbw.write("    <Icon> <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle_highlight.png</href> </Icon>" + newLine);
				KMLbw.write("   </IconStyle>" + newLine);
				KMLbw.write("  </Style>" + newLine);
			}

			if (ExportGPX) {
				// Writing head of GPX file

				GPXbw.write("<?xml version=\"1.0\"?>" + newLine);
				GPXbw.write("<!-- Created with BasicAirData GPS Logger for Android - ver. " + versionName + " -->" + newLine);
				GPXbw.write("<gpx creator=\"BasicAirData GPS Logger\" version=\"" + versionName + "\" xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">" + newLine);
			}


			// ---------------------------------------------------------------- Writing Trackpoints
			// Approximation: 0.00000001 = 0° 0' 0.000036"
			// On equator 1" ~= 31 m  ->  0.000036" ~= 1.1 mm
			// We'll use 1 mm also for approx. altitudes!
			Log.w("myApp", "[#] Exporter.java - Writing Trackpoints");

			if (route.getNumberOfLocations() > 0) {
				String formattedLatitude = "";
				String formattedLongitude = "";
				String formattedAltitude = "";

				// Writes track headings
				if (ExportKML) {
					KMLbw.write("  <Placemark>" + newLine);
					KMLbw.write("   <name>GPS Logger</name>" + newLine);
					KMLbw.write("   <description>Point: " + route.getName() + " </description>" + newLine);
					KMLbw.write("   <styleUrl>#TrackLineAndPoly</styleUrl>" + newLine);
					KMLbw.write("   <LineString>" + newLine);
					KMLbw.write("    <extrude>0</extrude>" + newLine);
					KMLbw.write("    <tessellate>0</tessellate>" + newLine);
					KMLbw.write("    <altitudeMode>" + (getPrefKMLAltitudeMode == 1 ? "clampToGround" : "absolute") + "</altitudeMode>" + newLine);
					KMLbw.write("    <coordinates>" + newLine);
				}
				if (ExportGPX) {
					GPXbw.write("<trk>" + newLine);
					GPXbw.write(" <name>" + route.getName() + "</name>" + newLine);
					GPXbw.write(" <desc>GPS Logger: " + route.getName() + "</desc>" + newLine);
					GPXbw.write(" <trkseg>" + newLine);
				}

				List<LocationExtended> locationList = new ArrayList<LocationExtended>();

				for (int i = 0; i < route.getNumberOfLocations(); i += GroupOfLocations) {
					//Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
					if (!locationList.isEmpty()) locationList.clear();
					//Log.w("myApp", "[#] Exporter.java - DB query + list.addall(...)");
					locationList.addAll(Config.GPSDataBase.getLocationsList(route)); //, i, i + GroupOfLocations - 1));
					//Log.w("myApp", "[#] Exporter.java - Write files");

					if (!locationList.isEmpty()) {
						for (LocationExtended loc : locationList) {
							// Create formatted strings
							if (ExportKML || ExportGPX) {
								formattedLatitude = String.format(Locale.US, "%.8f", loc.getLocation().getLatitude());
								formattedLongitude = String.format(Locale.US, "%.8f", loc.getLocation().getLongitude());
								if (loc.getLocation().hasAltitude()) formattedAltitude = String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + AltitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!EGMAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction()));
							}

							// KML
							if (ExportKML) {
								if (loc.getLocation().hasAltitude()) KMLbw.write("     " + formattedLongitude + "," + formattedLatitude + "," + formattedAltitude + newLine);
								else KMLbw.write("     " + formattedLongitude + "," + formattedLatitude + ",0" + newLine);
							}
							// GPX
							if (ExportGPX) {
								GPXbw.write("  <trkpt lat=\"" + formattedLatitude + "\" lon=\"" + formattedLongitude + "\">" + newLine);
								if (loc.getLocation().hasAltitude()) {
									GPXbw.write("   <ele>");     // Elevation
									GPXbw.write(formattedAltitude);
									GPXbw.write("</ele>" + newLine);
								}
								if (loc.getLocation().hasSpeed()) {
									GPXbw.write("   <speed>");     // Speed
									GPXbw.write(String.format(Locale.US, "%.3f", loc.getLocation().getSpeed()));
									GPXbw.write("</speed>" + newLine);
								}
								GPXbw.write("   <time>");     // Time
								GPXbw.write(dfdate.format(loc.getLocation().getTime()) + "T" + dftime.format(loc.getLocation().getTime()) + "Z");
								GPXbw.write("</time>" + newLine);
								GPXbw.write("  </trkpt>" + newLine);
							}
						}
					}

					long progress = 100L * (i + GroupOfLocations) / (route.getNumberOfLocations() + route.getNumberOfPlacemarks());
					if (progress > 99) progress = 99;
					if (progress < 1) progress = 1;
					//EventBus.getDefault().post("TRACK_SETPROGRESS " + route.getId() + " " + progress);
					this.publishProgress(progress);
				}

				if (ExportKML) {
					KMLbw.write("    </coordinates>" + newLine);
					KMLbw.write("   </LineString>" + newLine);
					KMLbw.write("  </Placemark>" + newLine + newLine);
				}
				if (ExportGPX) {
					GPXbw.write(" </trkseg>" + newLine);
					GPXbw.write("</trk>" + newLine + newLine);
				}
			}

			v=50;
			this.publishProgress(Math.min(v, 100));


			// ---------------------------------------------------------------- Writing Placemarks
			Log.w("myApp", "[#] Exporter.java - Writing Placemarks");

			if (route.getNumberOfPlacemarks() > 0) {
				// Writes track headings

				List<LocationExtended> placemarkList = new ArrayList<LocationExtended>();

				for (int i = 0; i < route.getNumberOfPlacemarks(); i += GroupOfLocations) {
					//Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
					if (!placemarkList.isEmpty()) placemarkList.clear();
					placemarkList.addAll(Config.GPSDataBase.getPlacemarksList(route)); //.getId(), i, i + GroupOfLocations - 1));

					if (!placemarkList.isEmpty()) {
						for (LocationExtended loc : placemarkList) {

							// KML
							if (ExportKML) {
								KMLbw.write(newLine + "  <Placemark>" + newLine);
								KMLbw.write("   <name>");
								KMLbw.write(loc.getDescription()
										.replace("<","&lt;")
										.replace("&","&amp;")
										.replace(">","&gt;")
										.replace("\"","&quot;")
										.replace("'","&apos;"));
								KMLbw.write("</name>" + newLine);
								KMLbw.write("   <styleUrl>#Bookmark_Style</styleUrl>" + newLine);
								KMLbw.write("   <Point>" + newLine);
								KMLbw.write("    <altitudeMode>absolute</altitudeMode>" + newLine);
								KMLbw.write("    <coordinates>");
								if (loc.getLocation().hasAltitude()) {
									KMLbw.write(String.format(Locale.US, "%.8f", loc.getLocation().getLongitude()) + "," +
											String.format(Locale.US, "%.8f", loc.getLocation().getLatitude()) + "," +
											String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + AltitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!EGMAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction())));
								} else {
									KMLbw.write(String.format(Locale.US, "%.8f", loc.getLocation().getLongitude()) + "," +
											String.format(Locale.US, "%.8f", loc.getLocation().getLatitude()) + "," +
											"0");
								}
								KMLbw.write("</coordinates>" + newLine);
								KMLbw.write("    <extrude>1</extrude>" + newLine);
								KMLbw.write("   </Point>" + newLine);
								KMLbw.write("  </Placemark>" + newLine);
							}


							// GPX
							if (ExportGPX) {
								GPXbw.write(newLine + "<wpt lat=\"");
								GPXbw.write(String.format(Locale.US, "%.8f", loc.getLocation().getLatitude()) + "\" lon=\"" +
										String.format(Locale.US, "%.8f", loc.getLocation().getLongitude()) + "\">" + newLine);

								if (loc.getLocation().hasAltitude()) {
									GPXbw.write(" <ele>");     // Elevation
									GPXbw.write(String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + AltitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!EGMAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction())));
									GPXbw.write("</ele>" + newLine);
								}

								GPXbw.write(" <time>");     // Time
								GPXbw.write(dfdate.format(loc.getLocation().getTime()) + "T" + dftime.format(loc.getLocation().getTime()) + "Z");
								GPXbw.write("</time>" + newLine);

								GPXbw.write(" <name>");     // Name
								GPXbw.write(loc.getDescription()
										.replace("<","&lt;")
										.replace("&","&amp;")
										.replace(">","&gt;")
										.replace("\"","&quot;")
										.replace("'","&apos;"));
								GPXbw.write("</name>" + newLine);

								GPXbw.write("</wpt>" + newLine);
							}
						}
					}

					long progress = 100L * (route.getNumberOfLocations() + i + GroupOfLocations) / (route.getNumberOfLocations() + route.getNumberOfPlacemarks());
					if (progress > 99) progress = 99;
					if (progress < 1) progress = 1;
					//EventBus.getDefault().post("TRACK_SETPROGRESS " + route.getId() + " " + progress);
					this.publishProgress(progress);
				}
			}


			// ------------------------------------------------------------ Writing tails and close
			Log.w("myApp", "[#] Exporter.java - Writing Tails and close files");

			if (ExportKML) {
				KMLbw.write(" </Document>" + newLine);
				KMLbw.write("</kml>");

				KMLbw.close();
				KMLfw.close();
			}
			if (ExportGPX) {
				GPXbw.write("</gpx>");

				GPXbw.close();
				GPXfw.close();
			}

			Log.w("myApp", "[#] Exporter.java - Files exported!");

			//EventBus.getDefault().post("TRACK_SETPROGRESS " + route.getId() + " 100");
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				Log.w("myApp", "[#] Exporter.java - Cannot wait!!");
			}

			//EventBus.getDefault().post("TRACK_EXPORTED " + route.getId());

		} catch (IOException e) {
			e.printStackTrace();
			//Toast.makeText(this, "File not saved",Toast.LENGTH_SHORT).show();
		}

	}

	public String  getDirFile() {
		return SaveIntoFolder;
	}

	public String getFilename() {
		return filename;
	}


}
