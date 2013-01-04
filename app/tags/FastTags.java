/*
 * Copyright 2013, L. Murer.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see < http://www.gnu.org/licenses/ >.
 */

package tags;

import de.schildbach.pte.dto.Connection.Footway;
import de.schildbach.pte.dto.Connection.Part;
import de.schildbach.pte.dto.Connection.Trip;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import models.Directs;
import models.KnownProvider;
import models.Stations;
import play.api.templates.Html;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * User: Laurin
 * Date: Oct 19, 2011
 * Time: 10:41:52 AM
 * To change this template use File | Settings | File Templates.
 */
//Although the IDE doesn't recognise it, the methods are used from the templates
@SuppressWarnings("UnusedDeclaration")
public class FastTags {
    /**
     * This Tag verifies, that an object is an instance of a specific class.
     */
    public static boolean isit(Object o, Object clazz) {
        if (!(clazz instanceof String)) {
            clazz = clazz.getClass().getSimpleName();
        }
        return o.getClass().getSimpleName().equals(clazz);
    }

    /**
     * It prints out the amount of changes of a trip
     *
     * @param vias        List with multiple Parts (e.g. a List of Vias)
     * @param text        String to describe the amount (will be displayed)
     * @param text_plural (optional) String to describe the amount in plural
     */
    public static String getAmountOfChanges(List<Part> vias, String text, @Nullable String text_plural) {
        int amount = -1; //the first part obviously doesn't count as a change
        //for each Part that isn't a Footway, count one up
        for (Part via : vias) {
            if (!(via instanceof Footway)) {
                amount++;
            }
        }
        //print the output
        if (amount == 1) {
            return "1" + text;
        } else if (amount > 1) {
            if (text_plural != null) {
                return amount + text_plural;
            } else {
                return amount + text + "s";
            }
        }
        return "";
    }


    /**
     * It prints out the body part, but only if there are multiple parts that need to be displayed
     *
     * @param vias List with multiple Parts (e.g. a List of Vias)
     * @param body displayed, if vias can be displayed
     */
    public static String showDetails(List<Part> vias, String body) {
        if (hasDetails(vias)) {
            return body;
        } else {
            return "";
        }
    }

    /**
     * If there are multiple parts that need to be displayed
     *
     * @param vias List with multiple Parts (e.g. a List of Vias)
     */
    public static boolean hasDetails(List<Part> vias) {
        int totalRealParts = 0;
        for (Part via : vias) {
            if (!(via instanceof Footway)) {
                totalRealParts++;
                if (totalRealParts > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * It prints out as a table containing how to change from one Part to the next Part.
     *
     * @param vias     List with multiple Parts (e.g. a List of Vias)
     * @param provider used for getting the timezone
     */
    public static Html getVias(List<Part> vias, KnownProvider provider) {
        //The default argument should be a List Object containing only parts. Otherwise, there will be thrown an Exception.
        TimeZone timeZone = provider.timeZone;
        //For all Vias, do the following
        boolean isFirstElement = true; //is it the first via?
        boolean isWalking = false; //is this/the previous via a part to walk (Footway)
        String lastStationName = ""; //the name of the station of the last via
        StringBuilder ret = new StringBuilder("<table>");
        //For each via as a Part
        for (Part via : vias) {

            if (via instanceof Footway) { // don't display this, if it's to walk but remember it.
                isWalking = true;
                continue;
            }
            //We don't want to display the departure part of the first element, as it's already in the overview.
            if (isFirstElement) {
                isFirstElement = false;
            } else {
                //Write the part where to depart from this Part (second part in output view)
                ret.append("\t\t<td style='padding:0 10px;'>");
                //If the last station was the same as this one, no need to display its name again (as it's the second part of the line in the output view)
                if (lastStationName.equals(getLocation(via.departure)) && (via instanceof Trip)) {
                    //make simple output (with Track and Time only)
                    Trip trip = (Trip) via;
                    String gleis = trip.departurePosition;
                    if (gleis != null && gleis.length() > 0) {
                        ret.append("umsteigen auf</td>\n\t\t<td>Gleis ").append(gleis).append(" (").append(printTime(trip.departureTime, timeZone)).append(")");
                    } else {
                        ret.append("Umstieg auf Anschluss</td>\n\t\t<td>").append(printTime(trip.departureTime, timeZone));
                    }
                } else {
                    //if it's not simple to display...
                    if (isWalking) { //if we walked to here
                        isWalking = false;
                        ret.append("Fussweg nach</td>\n\t\t<td>");
                    } else {
                        //or use a general description
                        ret.append("umsteigen auf</td>\n\t\t<td>");
                    }
                    //and the target departure to change to
                    ret.append(getPartOfPart(via, true, timeZone)).append("</td>");
                }
                ret.append("\t</tr>\n");
            }
            ret.append("\t<tr><td></td><td align=\"left\" colspan=\"2\">").append(getPartIcon(via, false)).append("</td></tr>\n");

            //We don't want to display the arriving part of the last element, as it's already in the overview
//                    if (via_iterator.hasNext()) {
            //Write the part where to arrive at by this Part (first part in output view)
            ret.append("\t<tr>\n"); //start a new line
            ret.append("\t\t<td>").append(getPartOfPart(via, false, timeZone)).append("</td>"); //show the full information of this Parts arrival
            lastStationName = getLocation(via.arrival); //set the name of this arrival station (to not show it in the second part of the output again)
//                    }
        }
        ret.append("\t\t<td></td>\n\t\t<td></td>\n\t</tr>\n</table>\n");
        return new Html(ret.toString());
    }

    /**
     * Creates HTML code to display an icon for a part of a connection
     *
     * @param p        the part to give the icon for
     * @param iconOnly whether only the icon should be displayed (and not also a description behind)
     * @return HTML code to display an icon for the given connection part
     */
    private static String getPartIcon(Part p, boolean iconOnly) {
        if (p instanceof Footway) {
            return "<img src=\"/public/images/icons/foot.png\" width=\"22\" height=\"22\" alt=\"" + (iconOnly ? "Fussweg" : "") + "\">" + (iconOnly ? "" : " Fussweg");
        } else if (p instanceof Trip) {
            Trip t = (Trip) p;
            Line line = t.line;
            if (line != null) {
                String label = line.label;
                if (label.length() > 2) {
                    return "<img src=\"/public/images/icons/" + label.substring(0, 1).toLowerCase() + ".png\" width=\"22\" height=\"22\" alt=\"" + (iconOnly ? label.substring(1) : "") + "\">" + (iconOnly ? "" : " " + label.substring(1));
                }
            }
        }
        if (iconOnly) {
            return "<img src=\"/public/images/icons/unknown.png\" width=\"22\" height=\"22\" alt=\"Unbekanntes Verkehrsmittel\">";
        } else {
            return "Reise";
        }
    }

    /**
     * It prints out the duration of a connection in hours and minutes
     *
     * @param departure departure time
     * @param arrival   arrival time
     */
    public static String getDuration(Date departure, Date arrival) {
        long duration = (arrival.getTime() - departure.getTime());
        duration /= 60000; //to minutes
        StringBuilder ret = new StringBuilder();
        if (duration >= 60) {
            ret.append(duration / 60);
            ret.append("h ");
        }
        ret.append(duration % 60);
        ret.append("min");
        return ret.toString();
    }

    /**
     * Parses the time to be displayed
     *
     * @param date     to be displayed
     * @param provider used for getting the timezone
     */
    public static String parseTime(Date date, KnownProvider provider) {
        return printTime(date, provider.timeZone);
    }

    /**
     * It prints out the current time
     *
     * @param provider The provider to get the timezone from
     */
    public static String getCurrentTime(KnownProvider provider) {
        DateFormat now = new SimpleDateFormat("d.M.yy HH:mm");
        now.setTimeZone(provider.timeZone);
        return now.format(new Date());
    }

    /**
     * Returns the location as a string to display
     *
     * @param location the location to be returned as a string
     * @return the name of the location
     */
    private static String getLocation(Location location) {
        if (location == null) return "";
        String out = location.name;
        if (out == null) out = "";
        return out;
    }

    /**
     * Gets the departure or arrival of a Via/Part as a String
     *
     * @param via          the part to render (should, but don't have to, be a Trip)
     * @param getDeparture Show the departure? (or the arrival)
     * @param timeZone     In which timezone the times should be displayed
     * @return Description of the departure/arrival of the part
     */
    private static String getPartOfPart(Part via, boolean getDeparture, TimeZone timeZone) {
        //Make sure, via is not null
        if (via == null) return "";
        //Define the output
        StringBuilder location;
        if (getDeparture) { //what should be printed (departure or arrival)
            location = new StringBuilder(getLocation(via.departure));
        } else {
            location = new StringBuilder(getLocation(via.arrival));
        }
        //if it's a Trip, add more information to the output
        if (via instanceof Trip) {
            Trip trip = (Trip) via; //yeah, it's a trip
            // Read track and time
            String gleis;
            Date zeit;
            if (getDeparture) {
                gleis = trip.departurePosition;
                zeit = trip.departureTime;
            } else {
                gleis = trip.arrivalPosition;
                zeit = trip.arrivalTime;
            }
            location.append(" (");
            //Add track, if it's defined
            if (gleis != null && gleis.length() > 0) {
                location.append("Gleis ");
                location.append(gleis);
                location.append(", ");
            }
            //add time
            location.append(printTime(zeit, timeZone));
            location.append(")");
        }
        return location.toString();
    }

    /**
     * Renders the time for display
     *
     * @param date     Which time to print
     * @param timeZone In which timezone should the time be
     * @return The time to display
     */
    private static String printTime(Date date, TimeZone timeZone) {
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(date);
    }

    /**
     * Removes the matching of a regular expression from the body
     *
     * @param regex The regular expression needle that's been removed
     * @param body  haystack
     */
    public static String remove(String regex, String body) {
        if (body == null) return null;
        if (regex == null) {
            return body;
        } else {
            return body.replaceAll(regex, "");
        }
    }

    /**
     * Prints the list of the checkboxes marking direct-connections only
     *
     * @param isCrossover If the user filed a crossover request
     * @param from        The list of froms to get the amount
     * @param to          The list of tos to get the amount
     * @param direct      The already given direct/none-direct checks to be displayed
     * @return The HTML-Code for the direct-connection-only checkboxes
     */
    @SuppressWarnings("UnusedDeclaration")
    public static Html printDirectCheckboxes(boolean isCrossover, Stations from, Stations to, Directs direct) {
        StringBuilder sb = new StringBuilder();
        final int repeats = Math.max(Math.max(from.size(), to.size()), 2) - 1;
        for (int i = 0; i < repeats; i++) {
            if (!isCrossover || i == 0) {
                sb.append("<input id='direct' class='direct' type='checkbox' name='direct[]' value='").append(i).append("'");
                if (i < direct.size() && direct.get(i)) {
                    sb.append("checked");
                }
                sb.append("/>");
            }
        }
        return new Html(sb.toString());
    }

    public static class IteratorExtender {
        private int i = 0;

        public int get() {
            return i;
        }

        public void next() {
            ++i;
        }

        public boolean isEven() {
            return i % 2 == 0;
        }

        public void reset() {
            i = 0;
        }
    }

    public static Class getInnerClass(String outerClassName, String innerClassName) throws ClassNotFoundException {
        for (Class innerClass : Class.forName(outerClassName).getClasses()) {
            if (innerClass.getSimpleName().equals(innerClassName)) {
                return innerClass;
            }
        }
        return null;
    }

    public static boolean hasDeparturePosition(Part part) {
        return part instanceof Trip && ((Trip) part).departurePosition != null;
    }

    public static String getDeparturePosition(Part part) {
        assert hasDeparturePosition(part);
        return ((Trip) part).departurePosition;
    }

    public static boolean hasArrivalPosition(Part part) {
        return part instanceof Trip && ((Trip) part).arrivalPosition != null;
    }

    public static String getArrivalPosition(Part part) {
        assert hasDeparturePosition(part);
        return ((Trip) part).arrivalPosition;
    }
}