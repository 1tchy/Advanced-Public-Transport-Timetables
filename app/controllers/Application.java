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
package controllers;

import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.OpenDataProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import models.Autocomplete;
import models.ComplexRequests;
import models.InputChecker;
import models.KnownProvider;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import play.data.validation.Validation;
import play.libs.Mail;
import play.mvc.Catch;
import play.mvc.Controller;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Application extends Controller {

    /**
     * Shows a webpage to the user, that displays some input fields to start a request. There may be some predefined values
     * //@param provider the ID of the provider to get the timetable from
     */
    public static void index() {
        Validation.clear();
        NetworkProvider provider = InputChecker.getAndValidateProvider("sbb");
        if (provider == null) return;
        //prepare all input for the template. No verification done.
        putAllParamsToRenderArgs();
        //if no start/stop field is already set, make sure that anyhow two fields are displayed
        if (renderArgs.get("start") == null) {
            renderArgs.put("start", new String[]{null});
        }
        if (renderArgs.get("stop") == null) {
            renderArgs.put("stop", new String[]{null});
        }
        DateFormat now = new SimpleDateFormat("HH:mm");
        now.setTimeZone(KnownProvider.getTimeZone(provider));
        renderArgs.put("now", now.format(new Date()));
        render("Application/index.html");
    }


    /**
     * Shows a webpage with some timetables to the user, depending on the given parameters
     * //@param provider the ID of the provider to get the timetable from
     */
    @SuppressWarnings("unused")
    public static void showTimetable() {
        String provider = "sbb";
        Validation.clear();
        NetworkProvider provider_object = InputChecker.getAndValidateProvider(provider);
        if (provider_object == null) {
            return;
        }
        //checks the given parameters and creates correct Objects. On error, create error output.
        Set<String> shownErrors = new HashSet<String>(1);
        List<Location> starts = InputChecker.getAndValidateStations(provider_object, getArrayParams("start"), "Abfahrtshaltestelle", "start", shownErrors);
        boolean isCrossover = InputChecker.getAndValidateBoolean(params.get("crossover"), true);
        List<Location> stops = InputChecker.getAndValidateStations(provider_object, getArrayParams("stop"), "Zielhaltestelle", "stop", shownErrors);
        LinkedList<Boolean> direct = InputChecker.getAndValidateDirects(getArrayParams("direkt"), Math.max(starts.size(), stops.size()));
        if (!isCrossover && (starts.size() != stops.size())) {
            Validation.addError("crossover", "Wenn nicht alle Verbindungen von <b>allen</b> Abfahrtshaltestellen zu <b>allen</b> Zielhaltestellen gesucht werden, müssen gleich viele Abfahrts- wie Zielhaltestellen angegeben werden.");
        }
        TimeZone timeZone = KnownProvider.getTimeZone(provider_object);
        Date datetime = InputChecker.getAndValidateTime(params.get("time"), "time", timeZone);
        if (datetime == null) {
            datetime = new Date();
        }
        boolean isTimeAsDeparture = InputChecker.getAndValidateBoolean(params.get("timeAsDeparture"), true);
        //now, all parameters got handled. Did there occur an error?
        if (Validation.hasErrors()) {
            //On any error, show the input page again.
            showInputPageAgain();
        } else {
            //If everything is fine, create the desired timetables and print them out
            try {
                renderArgs.put("connections", ComplexRequests.getMultipleTimetables(provider_object, starts, isCrossover, stops, direct, datetime, isTimeAsDeparture));
            } catch (ComplexRequests.ServerNotReachableException e) {
                Validation.addError("general", e.getLocalizedMessage());
                showInputPageAgain();
                return;
            }
            renderArgs.put("starts", starts);
            renderArgs.put("stops", stops);
            renderArgs.put("timezone", timeZone);
            render("Application/showTimetable.html");
        }
    }

    private static void showInputPageAgain() {
        putAllParamsToRenderArgs();
        Validation.keep();
        render("Application/index.html");
    }

    /**
     * Reads all Parameter from the URLs GET-List and puts the value(s) to the renderArgs
     */
    private static void putAllParamsToRenderArgs() {
        for (String param : params.all().keySet()) {
            if (param.matches(".*\\[\\d*\\]")) {
                String newKey = param.replaceAll("\\[\\d*\\]", "");
                renderArgs.put(newKey, getArrayParams(newKey));
            } else {
                renderArgs.put(param, params.get(param));
            }
        }
    }

    /**
     * Gets a parameter of the URL that is an array.
     *
     * @param name The parameters name to get all values for
     * @return All parameters
     */
    private static String[] getArrayParams(String name) {
        //add all params with the format "name[]", "name[]", ...
        String[] ret = params.getAll(name + "[]");
        //add all params with the format "name[0]", "name[1]", ...
        int i = 0;
        while (params._contains(name + "[" + i + "]")) {
            String[] additionalValues = params.getAll(name + "[" + i + "]");
            if (!(additionalValues.length == 1 && additionalValues[0] == null)) {
                ret = (String[]) ArrayUtils.addAll(ret, additionalValues);
            }
            i++;
        }
        return ret;
    }

    /**
     * Answers to a autocomplete request for a station
     * //@param provider the ID of the provider
     *
     * @param term the term to complete to a stations name
     */
    @SuppressWarnings("unused")
    public static void autocompleteStation(final String term) {
        String provider = "sbb";
        renderJSON(Autocomplete.stations(provider, term));
    }

    /**
     * Returns some stations near the given coordinates
     *
     * @param lat latitude
     * @param lon longitude
     */
    @SuppressWarnings("unused")
    public static void getStation(float lat, float lon) {
        String provider_string = "sbb";
        NearbyStationsResult stations;
        NetworkProvider provider = InputChecker.getAndValidateProvider(provider_string);
        try {
            if (provider instanceof OpenDataProvider) {
                stations = ((OpenDataProvider) provider).queryNearbyStations(lat, lon, 0, 0);
            } else {
                stations = provider.queryNearbyStations(new Location(LocationType.ANY, (int) lat, (int) lon), 0, 0);
            }
        } catch (IOException e) {
            System.err.println("Could not find nearby stations at " + lat + "/" + lon);
            e.printStackTrace();
            renderJSON(new ArrayList(0));
            return;
        }
        renderJSON(stations.stations);
    }

    /**
     * Shows a page to answer (all) questions a User of this page might have (frequently asked questions).
     */
    @SuppressWarnings("unused")
    public static void showFAQ() {
        render("Application/faq.html");
    }

    @SuppressWarnings("unused")
    @Catch(Exception.class)
    public static void catchAllExceptions(Throwable throwable) throws EmailException {
        SimpleEmail mail = new SimpleEmail();
        String mailAddress = "ymfhaad6n9" + "@" + "laurinmurer.ch";
        mail.setFrom(mailAddress);
        mail.addTo(mailAddress);
        mail.setSubject("Exception thrown: " + throwable.getMessage());
        StringBuilder mailContent = new StringBuilder("Request was: http://");
        mailContent.append(request.domain).append(request.url).append("\n\nStacktrace:\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            mailContent.append(element).append("\n");
        }
        mail.setMsg(mailContent.toString());
        Mail.send(mail);
        if (Math.random() > 0.03) { //show almost always, but prevent from a loop
            if (!Validation.hasErrors()) {
                Validation.addError("general", "An internal error occurred. It could not be recovered, please try again later.<br><span style=\"font-size:smaller\"><b>Error:</b> " + throwable.getLocalizedMessage() + "</span>");
            }
            showInputPageAgain();
        }
    }
}