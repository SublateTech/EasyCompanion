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

package com.sublate.gps.exporters;

import android.content.Context;

import com.sublate.gps.Config;
import com.sublate.gps.abstracts.IActionListener;
import com.sublate.gps.helper.Utilities;
import com.sublate.gps.model.Route;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FileSenderFactory {

    public static void SendFiles(Context applicationContext, IActionListener callback) {


        List<Route> routes = Config.GPSDataBase.getListRouteByDate(new Date());

        final String currentFileName = ""; //Session.getCurrentFileName();
        //tracer.info("Sending file " + currentFileName);

        File gpxFolder = new File(Config.getGpsLoggerFolder());

        if (Utilities.GetFilesInFolder(gpxFolder).length < 1) {
            callback.OnFailure();
            return;
        }

        List<File> files = new ArrayList<File>(Arrays.asList(Utilities.GetFilesInFolder(gpxFolder, new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(currentFileName) && !s.contains("zip");
            }
        })));

        if (files.size() == 0) {
            callback.OnFailure();
            return;
        }

        List<IFileSender> senders = GetFileSenders(applicationContext, callback);

        for (IFileSender sender : senders) {
            sender.UploadFile(files);
        }

    }


    public static List<IFileSender> GetFileSenders(Context applicationContext, IActionListener callback) {
        List<IFileSender> senders = new ArrayList<IFileSender>();

        /*
        if (GDocsHelper.IsLinked(applicationContext)) {
            senders.add(new GDocsHelper(applicationContext, callback));
        }

        if (OSMHelper.IsOsmAuthorized(applicationContext)) {
            senders.add(new OSMHelper(applicationContext, callback));
        }

        if (AppSettings.isAutoEmailEnabled()) {
            senders.add(new AutoEmailHelper(callback));
        }

        DropBoxHelper dh = new DropBoxHelper(applicationContext, callback);

        if (dh.IsLinked()) {
            senders.add(dh);
        }

        if (AppSettings.isAutoOpenGTSEnabled()) {
            senders.add(new OpenGTSHelper(applicationContext, callback));
        }

        if (AppSettings.isAutoFtpEnabled()) {
            senders.add(new FtpHelper(callback));
        }
        */

        return senders;

    }
}
