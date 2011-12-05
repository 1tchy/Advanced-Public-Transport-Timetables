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
import de.schildbach.pte.dto.Location;
import models.Autocomplete;
import models.ComplexRequests;
import models.InputChecker;
import models.KnownProvider;
import play.data.validation.Validation;
import play.mvc.Controller;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Application extends Controller {

    /**
     * Shows s webpage to the user to welcome him and ask him for the transport company he wants to use
     */
    public static void index_old() {
        renderArgs.put("provider", KnownProvider.all());
        render("Application/index_old.html");
    }

    /**
     * Shows a webpage to the user, that displays some input fields to start a request. There may be some predefined values
     * //@param provider the ID of the provider to get the timetable from
     */
    public static void index() {
        String provider = "sbb";
        Validation.clear();
        NetworkProvider networkProvider = InputChecker.getAndValidateProvider(provider);
        if (networkProvider == null) return;
        //prepare all input for the template. No verification done.
        Map<String, String[]> allParams = params.all();
        for (String param : allParams.keySet()) {
            renderArgs.put(param.replace("[]", ""), (param.endsWith("[]") ? allParams.get(param) : allParams.get(param)[0]));
        }
        DateFormat now = new SimpleDateFormat("HH:mm");
        now.setTimeZone(KnownProvider.getTimeZone(networkProvider));
        renderArgs.put("now", now.format(new Date()));
        render("Application/index.html");
    }


    /**
     * Shows a webpage with some timetables to the user, depending on the given parameters
     * //@param provider the ID of the provider to get the timetable from
     */
    public static void showTimetable() {
        String provider = "sbb";
        Validation.clear();
        NetworkProvider provider_object = InputChecker.getAndValidateProvider(provider);
        if (provider_object == null) {
            return;
        }
        //checks the given parameters and creates correct Objects. On error, create error output.
        Set<String> shownErrors = new HashSet<String>(1);
        List<Location> starts = InputChecker.getAndValidateStations(provider_object, params.getAll("start[]"), "Abfahrtshaltestelle", "start", shownErrors);
        boolean isCrossover = InputChecker.getAndValidateBoolean(params.get("crossover"), true);
        List<Location> stops = InputChecker.getAndValidateStations(provider_object, params.getAll("stop[]"), "Zielhaltestelle", "stop", shownErrors);
        if (!isCrossover && (starts.size() != stops.size())) {
            Validation.addError("crossover", "Wenn nicht alle Verbindungen von <b>allen</b> Abfahrtshaltestellen zu <b>allen</b> Zielhaltestellen gesucht werden, müssen gleich viele Abfahrts- wie Zielhaltestellen angegeben werden.");
        }
        Date datetime = InputChecker.getAndValidateTime(params.get("time"), "time");
        if (datetime == null) {
            // get the providers timezone
            TimeZone providerTimezone = KnownProvider.getTimeZone(provider_object);
            // get the string of the timezone of the provider
            SimpleDateFormat timezoneFormatter = new SimpleDateFormat("zzz");
            timezoneFormatter.setTimeZone(providerTimezone);
            String providerTimezoneString = timezoneFormatter.format(new Date());

            String formatString = "EEE MMM d HH:mm:ss zzz yyyy";
            // get the string of the local time
            DateFormat df = new SimpleDateFormat(formatString);
            df.setTimeZone(providerTimezone);
            String localeDate = df.format(new Date());
            // fake the timezone, so that a Date can be generated accepted by provider
            localeDate = localeDate.replace(providerTimezoneString, "GMT");
            // convert the local time to the Date accepted by the provider
            try {
                datetime = df.parse(localeDate);
            } catch (ParseException e) {
                datetime = new Date();
            }
        }
        boolean isTimeAsDeparture = InputChecker.getAndValidateBoolean(params.get("timeAsDeparture"), true);
        //now, all parameters got handled. Did there occur an error?
        if (Validation.hasErrors()) {
            //On any error, show the input page again.
            Map<String, String[]> allParams = params.all();
            for (String param : allParams.keySet()) {
                renderArgs.put(param.replace("[]", ""), (param.endsWith("[]") ? allParams.get(param) : allParams.get(param)[0]));
            }
            Validation.keep();
            render("Application/index.html");
        } else {
            //If everything is fine, create the desired timetables and print them out
            renderArgs.put("connections", ComplexRequests.getMultipleTimetables(provider_object, starts, isCrossover, stops, datetime, isTimeAsDeparture));
            renderArgs.put("starts", starts);
            renderArgs.put("stops", stops);
            render("Application/showTimetable.html");
        }
    }

    /**
     * Answers to a autocomplete request for a station
     * //@param provider the ID of the provider
     *
     * @param term the term to complete to a stations name
     */
    public static void autocompleteStation(final String term) {
        String provider = "sbb";
        renderJSON(Autocomplete.stations(provider, term));
    }

    /**
     * Shows a page to answer (all) questions a User of this page might have (frequently asked questions).
     */
    public static void showFAQ() {
        render("Application/faq.html");
    }
}