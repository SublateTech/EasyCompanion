package eu.siacs.conversations.location;

import android.location.Location;
import android.os.Bundle;

import eu.siacs.conversations.entities.Contact;


public abstract class BestLocationListener {

	public abstract void onLocationUpdate(Location location, BestLocationProvider.LocationType type, boolean isFresh, Contact contact);
	
	public abstract void onLocationUpdateTimeoutExceeded(BestLocationProvider.LocationType type);
	
	public abstract void onStatusChanged(String provider, int status, Bundle extras);
	
	public abstract void onProviderEnabled(String provider);
	
	public abstract void onProviderDisabled(String provider);

}
