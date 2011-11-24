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

package models;

import controllers.Application;
import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.Location;
import play.data.validation.Validation;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: Oct 10, 2011
 * Time: 7:07:02 PM
 */
public class InputChecker {

    /**
     * String to boolean
     *
     * @param bool          String to evaluate
     * @param defaultReturn if String is neither "true" nor "false", what to return then
     * @return the String ("true"/"false") as its equivalent boolean or the defaultReturn value if it's not clear
     */
    public static boolean getAndValidateBoolean(String bool, boolean defaultReturn) {
        if (bool == null) {
            return defaultReturn;
        } else if (bool.toLowerCase().equals("true")) {
            return true;
        } else if (bool.toLowerCase().equals("false")) {
            return false;
        } else {
            return defaultReturn;
        }
    }

    /**
     * String to Date
     *
     * @param time      String to evaluate (HH:mm)
     * @param fieldName The name of the field that's evaluated (used for error output)
     * @return a date with the given time within the last 2 and the next 22hours (or if no String got delivered, null)
     */
    public static Date getAndValidateTime(String time, String fieldName) {
        //if nothing got delivered, return the current time
        if (time == null || time.trim().length() == 0) {
            return null;
        }
        //remove spaces from the input
        time = time.trim();
        //fix issue with hours like "8:15" (to "08:15")
        if (time.length() == 4) time = "0" + time;
        if (time.length() == 5) {
            //fix issues with "24:XY" (to "00:XY"
            if (time.startsWith("24")) time = "00" + time.substring(3, 2);
            //Guarantee legal values for the time
            if (time.matches("(([01][0-9])|(2[0-3])).[0123456][0-9]")) {
                Calendar now = new GregorianCalendar();
                //create the date (as a Calendar)
                Calendar c = new GregorianCalendar();
                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
                c.set(Calendar.MINUTE, Integer.parseInt(time.substring(3, 5)));
                //within how many hours in the past is ok?
                int hours = 2;
                //check if it leads to a selection of yesterday
                if (now.get(Calendar.HOUR_OF_DAY) < hours && (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) > ((now.get(Calendar.HOUR_OF_DAY) + 24 - hours) * 60 + now.get(Calendar.MINUTE))) {
                    c.add(Calendar.DAY_OF_YEAR, -1);
                } else
                    //or to a selection of tomorrow
                    if ((now.get(Calendar.HOUR_OF_DAY) - hours) * 60 + now.get(Calendar.MINUTE) > c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) {
                        c.add(Calendar.DAY_OF_YEAR, 1);
                    }
                return c.getTime();
            }
        }
        Validation.addError(fieldName, "Falsches Zeitformat. Zeit angeben in HH:MM.");
        return null;
    }

    /**
     * Strings to Locations
     *
     * @param provider    the provider of the timetable/the station names
     * @param stations    a list of station names
     * @param description The human readable name of the field that's evaluated (used for error output)
     * @param fieldName   The internal name of the field that's evaluated (used for error output)
     * @param shownErrors a list of shown errors, that only should be shown once
     * @return a list of stations (might have 0 elements)
     */
    public static Set<Location> getAndValidateStations(NetworkProvider provider, String[] stations, String description, String fieldName, Set<String> shownErrors) {
        //check for no station given
        if (stations == null || stations.length == 0) {
            Validation.addError(fieldName, "Sie müssen mindestens eine " + description + " angeben.");
            return new HashSet<Location>(0);
        }
        //will be true, if an error occurred
        boolean errorOccurred = false;
        Set<Location> stationSet = new HashSet<Location>(stations.length);
        //for each station that's not ""
        for (String station : stations) {
            if (!station.equals("")) {
                List<Location> locationList = null;
                try {
                    //get the list of Locations from the DataProvider
                    try {
                        locationList = provider.autocompleteStations(station);
                        if (locationList == null) {
                            errorOccurred = true;
                            Validation.addError(fieldName, "Fehler beim Finden der " + description + " '" + station + "'.");
                        }
                    } catch (UnknownHostException e) {
                        errorOccurred = true;
                        final String ErrorBahnanbieter = "ErrorBahnanbieter";
                        if (!shownErrors.contains(ErrorBahnanbieter)) {
                            shownErrors.add(ErrorBahnanbieter);
                            Validation.addError(fieldName, "Konnte Bahnanbieter-API nicht finden. (" + KnownProvider.get(provider) + ")");
                        }
                    }
                } catch (IOException e) {
                    errorOccurred = true;
                    final String ErrorDatasource = "ErrorDatasource";
                    if (!shownErrors.contains(ErrorDatasource)) {
                        shownErrors.add(ErrorDatasource);
                        Validation.addError(fieldName, "Fehler beim Verbinden zur Datenquelle.");
                        e.printStackTrace();
                    }
                }
                if (locationList == null) {
                    //error already put out
                } else if (locationList.size() == 1) { //There is only one station possible -> choose it
                    stationSet.add(locationList.get(0));
                } else if (locationList.size() == 0) { //There is no station possible -> print error
                    errorOccurred = true;
                    Validation.addError(fieldName, description + " '" + station + "' nicht gefunden.");
                } else { //There are multiple stations possible -> check all and add the correct matching to the list (or print error)
                    Location l = null;
                    for (Location location : locationList) {
                        if (station.equals(location.uniqueShortName())) {
                            l = location;
                            break;
                        }
                    }
                    if (l == null) {
                        errorOccurred = true;
                        Validation.addError(fieldName, description + " '" + station + "' ist nicht eindeutig.");
                    } else {
                        stationSet.add(l);
                    }
                }
            }
        }
        //if there have not yet been an error put out but still no station found, add an error.
        if (!errorOccurred && (stationSet.size() == 0)) {
            Validation.addError(fieldName, "Sie müssen mindestens eine " + description + " angeben.");
        }
        return stationSet;
    }

    /**
     * Checks if the given provider exists
     *
     * @param provider id of the provider to find
     * @return the found provider or null
     */
    public static NetworkProvider getAndValidateProvider(String provider) {
        NetworkProvider provider_object = KnownProvider.get(provider);
        if (provider_object == null) {
            Validation.addError("provider", "Unbekannter Bahnanbieter (" + provider + ")");
            Application.index();
            return null;
        }
        return provider_object;
    }
}
