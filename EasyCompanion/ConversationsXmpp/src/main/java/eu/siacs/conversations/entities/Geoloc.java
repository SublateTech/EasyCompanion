package eu.siacs.conversations.entities;

import android.location.Location;

import eu.siacs.conversations.xml.Element;

/**
 * Created by Alvaro on 1/9/2017.
 */

public class Geoloc  {

    private Contact contact;
    private double lat = 0.00;
    private double lon = 0.00;
    private double alt = 0.00;


    public Geoloc()
    {
    }

    public Geoloc(Location location)
    {
        setAlt(location.getAltitude());
        setLat(location.getLatitude());
        setLon(location.getLongitude());
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public void setAlt(double alt) {
        this.alt = alt;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Double getAlt() {
        return alt;
    }

    public Contact getContact() {
        return contact;
    }

    public String toXML() {
        StringBuilder builder = new StringBuilder(); //"<geoloc xmlns='http://jabber.org/protocol/geoloc' xml:lang='en'>");

        if (lat != 0 && lon!=0)
        {
            builder.append("<lat>");
            builder.append(lat);
            builder.append("</lat>");
            builder.append("<lon>");
            builder.append(lon);
            builder.append("</lon>");
            builder.append("<alt>");
            builder.append(alt);
            builder.append("</alt>");
        }

        //builder.append("</geoloc>");
        return builder.toString();
    }

    public Element getItem()
    {
        Element item = new Element("geoloc");
        item.setAttribute("xmlns", "http://jabber.org/protocol/geoloc");

        item.addChild(new Element("lat").setContent(getLat().toString()));
        item.addChild(new Element("alt").setContent(getAlt().toString()));
        item.addChild(new Element("lon").setContent(getLon().toString()));

        return item;
    }
}
