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
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import groovy.lang.Closure;
import models.KnownProvider;
import play.mvc.Scope;
import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User: Laurin
 * Date: Oct 19, 2011
 * Time: 10:41:52 AM
 * To change this template use File | Settings | File Templates.
 */
@FastTags.Namespace("my")
@SuppressWarnings("unused")
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
     * It prints out the amount of changes of a trip
     *
     * @param args     the default argument (arg) must include an object containing a List with multiple Parts (e.g. a List of Vias); the argument 'text' must include a String to describe the amount (will also be displayed), the optional argument 'pl' can define the pluralised argument 'text'.
     * @param body     ignored
     * @param out      used to return the result
     * @param template ignored
     * @param fromLine ignored
     */
    public static void _getAmountOfChanges(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        //prepare the parameteres to use
        Object object_via = args.get("arg");
        assert object_via instanceof List : "First parameter must be a list object. But is " + object_via;
        List list_vias = (List) object_via;
        Object object_text = args.get("text");
        assert object_text instanceof String : "Parameter text must be a String. But is " + object_text;
        String text = (String) object_text;
        int amount = -1; //the first part obviously doesn't count as a change
        //for each Part that isn't a Footway, count one up
        for (Object via : list_vias) {
            if (via instanceof Part) {
                if (!(via instanceof Footway)) {
                    amount++;
                }
            }
        }
        //print the output
        if (amount == 1) {
            out.print("1" + text);
        } else if (amount > 1) {
            if (args.containsKey("pl")) {
                out.print(amount + "" + args.get("pl"));
            } else {
                out.print(amount + text + "s");
            }
        }
    }


    /**
     * It prints out the body part, but only if there are multiple parts that need to be displayed
     *
     * @param args     the default argument (arg) must include an object containing a List with multiple Parts (e.g. a List of Vias)
     * @param body     displayed, if vias can be displayed
     * @param out      used to return the result
     * @param template ignored
     * @param fromLine ignored
     */
    public static void _showDetails(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        //prepare the parameteres to use
        Object object_via = args.get("arg");
        assert object_via instanceof List : "First parameter must be a list object. But is " + object_via;
        List list_vias = (List) object_via;
        int totalRealParts = 0;
        for (Object via : list_vias) {
            if (via instanceof Part) {
                if (!(via instanceof Footway)) {
                    totalRealParts++;
                    if (totalRealParts > 1) {
                        out.print(JavaExtensions.toString(body));
                        return;
                    }
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
        TimeZone timeZone = getTimezone(template);
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

                    if (via instanceof Footway) { // don't display this, if it's to walk but remember it.
                        isWalking = true;
                        continue;
                    }
                    //We don't want to display the departure part of the first element, as it's already in the overview.
                    if (isFirstElement) {
                        isFirstElement = false;
                    } else {
                        //Write the part where to depart from this Part (second part in output view)
                        out.print("\t\t<td style='padding:0 10px;'>");
                        //If the last station was the same as this one, no need to display its name again (as it's the second part of the line in the output view)
                        if (lastStationName.equals(getLocation(via.departure)) && (via instanceof Trip)) {
                            //make simple output (with Track and Time only)
                            Trip trip = (Trip) via;
                            String gleis = trip.departurePosition;
                            if (gleis != null && gleis.length() > 0) {
                                out.print("umsteigen auf</td>\n\t\t<td>Gleis " + gleis + " (" + printTime(trip.departureTime, timeZone) + ")");
                            } else {
                                out.print("Umstieg auf Anschluss</td>\n\t\t<td>" + printTime(trip.departureTime, timeZone));
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
                            out.print(getPartOfPart(via, true, timeZone) + "</td>");
                        }
                        out.println("\t</tr>");
                    }
                    out.println("\t<tr><td></td><td align=\"left\" colspan=\"2\">" + getPartIcon(via, false) + "</td></tr>");

                    //We don't want to display the arriving part of the last element, as it's already in the overview
//                    if (via_iterator.hasNext()) {
                    //Write the part where to arrive at by this Part (first part in output view)
                    out.println("\t<tr>"); //start a new line
                    out.print("\t\t<td>" + getPartOfPart(via, false, timeZone) + "</td>"); //show the full information of this Parts arrival
                    lastStationName = getLocation(via.arrival); //set the name of this arrival station (to not show it in the second part of the output again)
//                    }
                }
                out.println("\t\t<td></td>\n\t\t<td></td>\n\t</tr>\n</table>");
                return;
            }
        }
        //If there was an error with the parameters...
        throw new InvalidParameterException("Default argument should be a list of Connection Parts.");
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
     * @param args     the argument 'dt' must include the departure time (as a Date), the argument 'at' must include the arrival time (as a Date)
     * @param body     ignored
     * @param out      used to return the result
     * @param template ignored
     * @param fromLine ignored
     */
    public static void _getDuration(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        assert args.containsKey("dt") : "Argument 'dt' must be given.";
        assert args.get("dt") instanceof Date : "Argument 'dt' must be a Date.";
        assert args.containsKey("at") : "Argument 'at' must be given.";
        assert args.get("at") instanceof Date : "Argument 'at' must be a Date.";
        Date dt = (Date) args.get("dt");
        Date at = (Date) args.get("at");
        long duration = (at.getTime() - dt.getTime());
        duration /= 60000; //to minutes
        if (duration >= 60) {
            out.print(duration / 60);
            out.print("h ");
        }
        out.print(duration % 60);
        out.print("min");
    }

    /**
     * Parses the time to be displayed
     *
     * @param args     the first argument must be the time to display
     * @param body     ignored
     * @param out      used to return the result
     * @param template used for getting the provider for which the time will be generated
     * @param fromLine ignored
     */
    public static void _parseTime(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        out.print(printTime(args.get("arg"), getTimezone(template)));
    }

    /**
     * It prints out the current time
     *
     * @param args     ignored
     * @param body     ignored
     * @param out      used to return the result
     * @param template used for getting the provider for which the time will be generated
     * @param fromLine ignored
     */
    public static void _getCurrentTime(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        DateFormat now = new SimpleDateFormat("d.M.yy HH:mm");
        try {
            now.setTimeZone(KnownProvider.getById(((Scope.Params) template.getBinding().getVariable("params")).get("provider")).timeZone);
        } catch (Exception e) {
            now.setTimeZone(TimeZone.getTimeZone("CET"));
            out.print("CET ");
        }
        out.print(now.format(new Date()));
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
     * Reads the timezone from the Template arguents
     *
     * @param template To get the timezone from
     * @return The timezone for the template
     */
    private static TimeZone getTimezone(ExecutableTemplate template) {
        Object o = template.getProperty("timezone");
        if (o instanceof TimeZone) return (TimeZone) o;
        return TimeZone.getDefault();
    }

    /**
     * Renders the time for display
     *
     * @param date     Which time to print
     * @param timeZone In which timezone should the time be
     * @return The time to display
     */
    private static String printTime(Object date, TimeZone timeZone) {
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