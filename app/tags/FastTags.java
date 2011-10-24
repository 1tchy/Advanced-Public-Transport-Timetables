/*
 * Copyright 2011, L. Murer.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tags;

import de.schildbach.pte.dto.Connection.Footway;
import de.schildbach.pte.dto.Connection.Part;
import de.schildbach.pte.dto.Connection.Trip;
import de.schildbach.pte.dto.Location;
import groovy.lang.Closure;
import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: Laurin
 * Date: Oct 19, 2011
 * Time: 10:41:52 AM
 * To change this template use File | Settings | File Templates.
 */
@FastTags.Namespace("my")
public class FastTags extends play.templates.FastTags {
    /**
     * This Tag verifies, that an object is an instance of a specific class. If it is, it will return the body-part of the tag
     *
     * @param args     the default argument (arg) must include an object; the argument 'a' must include a String with a simple Class name (e.g. 'String')
     * @param body     displayed, if "it is"
     * @param out      used to return the result
     * @param template ignored
     * @param fromLine ignored
     */
    public static void _isit(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {

        Object klasse = args.get("arg");
        Object klassenname = args.get("a");
        if (klasse == null || klassenname == null) {
            return;
        }
        Class theClass = klasse.getClass();
        if (theClass == null) {
            return;
        }
        klasse = theClass.getSimpleName();
        if (!(klassenname instanceof String)) {
            klassenname = klassenname.getClass();
            if (klassenname == null) {
                return;
            }
            klassenname = klassenname.getClass().getSimpleName();

        }
        if (klasse == null) {
            return;
        }
        if (klasse.equals(klassenname)) {
            out.print(JavaExtensions.toString(body));
        } else {
            for (Class allClasses : theClass.getClasses()) {
                if (allClasses.getSimpleName().equals(klassenname)) {
                    out.print(JavaExtensions.toString(body));
                    return;
                }
            }
        }
    }

    /**
     * It prints out as a table containing how to change from one Part to the next Part.
     *
     * @param args     the default argument (arg) must include an object containing a List with multiple Parts (e.g. a List of Vias)
     * @param body     ignored
     * @param out      used to return the result
     * @param template ignored
     * @param fromLine ignored
     */
    public static void _getVias(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        //The default argument should be a List Object containing only parts. Otherwise, there will be thrown an Exception.
        Object object_via = args.get("arg");
        if (object_via instanceof List) {
            List list_vias = (List) object_via;
            boolean allViasPart = true;
            for (Object via : list_vias) {
                if (!(via instanceof Part)) {
                    allViasPart = false;
                    break;
                }
            }
            if (allViasPart) {
                //For all Vias, do the following
                final Iterator via_iterator = list_vias.iterator();
                boolean isFirstElement = true; //is it the first via?
                boolean isWalking = false; //is this/the previous via a part to walk (Footway)
                String lastStationName = ""; //the name of the station of the last via
                out.println("<table>");
                //For each via as a Part
                while (via_iterator.hasNext()) {
                    Object via_obj = via_iterator.next();
                    assert via_obj instanceof Part;
                    Part via = (Part) via_obj;

                    //We don't want to display the departure part of the first element, as it's already in the overview.
                    if (isFirstElement) {
                        isFirstElement = false;
                    } else {
                        //Write the part where to depart from this Part (second part in output view)
                        if (via instanceof Footway) { // don't display this, if it's to walk but remember it.
                            isWalking = true;
                            continue;
                        }
                        out.print("\t\t<td style='padding:0 10px;'>");
                        //If the last station was the same as this one, no need to display its name again (as it's the second part of the line in the output view)
                        if (lastStationName.equals(getLocation(via.departure)) && (via instanceof Trip)) {
                            //make simple output (with Track and Time only)
                            Trip trip = (Trip) via;
                            String gleis = trip.departurePosition;
                            if (gleis != null && gleis.length() > 0) {
                                out.print("umsteigen auf</td>\n\t\t<td>Gleis " + gleis + " (" + getTime(trip.departureTime) + ")");
                            } else {
                                out.print(", Weiterfahrt</td>\n\t\t<td>" + getTime(trip.departureTime));
                            }
                        } else {
                            //if it's not simple to display...
                            if (isWalking) { //if we walked to here
                                isWalking = false;
                                out.print("Fussweg nach</td>\n\t\t<td>");
                            } else {
                                //or use a general description
                                out.print("umsteigen auf</td>\n\t\t<td>");
                            }
                            //and the target departure to change to
                            out.print(getPartOfPart(via, true) + "</td>");
                        }
                        out.println("\t</tr>");
                    }

                    //We don't want to display the arriving part of the last element, as it's already in the overview
                    if (via_iterator.hasNext()) {
                        //Write the part where to arrive at by this Part (first part in output view)
                        out.println("\t<tr>"); //start a new line
                        out.print("\t\t<td>" + getPartOfPart(via, false) + "</td>"); //show the full information of this Parts arrival
                        lastStationName = getLocation(via.arrival); //set the name of this arrival station (to not show it in the second part of the output again)
                    }
                }
                out.println("</table>");
                return;
            }
        }
        //If there was an error with the parameters...
        throw new InvalidParameterException("Default argument should be a list of Connection Parts.");
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
     * @return Description of the departure/arrival of the part
     */
    private static String getPartOfPart(Part via, boolean getDeparture) {
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
            location.append(getTime(zeit));
            location.append(")");
        }
        return location.toString();
    }

    /**
     * Renders the time for display
     *
     * @param zeit a date to render
     * @return a string with the time
     */
    private static String getTime(Date zeit) {
        if (zeit == null) return "??:??";
        return new SimpleDateFormat("HH:mm").format(zeit);
    }

    /**
     * Removes the matching of a regular expression from the body
     *
     * @param args     the default argument (arg) is the regular expression needle that's been removed
     * @param body     haystack
     * @param out      used to return the result
     * @param template ignored
     * @param fromLine ignored
     */
    public static void _remove(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        String s = JavaExtensions.toString(body);
        if (s == null) return;
        Object arg = args.get("arg");
        if (arg == null) {
            out.print(body);
        } else {
            out.print(s.replaceAll(arg.toString(), ""));
        }
    }
}