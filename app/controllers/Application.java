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
import models.ComplexRequests;
import models.InputChecker;
import models.KnownProvider;
import play.data.validation.Validation;
import play.mvc.Controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;

public class Application extends Controller {

    /**
     * Shows s webpage to the user to welcome him and ask him for the transport company he wants to use
     */
    public static void index() {
        renderArgs.put("provider", KnownProvider.all());
        render("Application/index.html");
    }

    /**
     * Shows a webpage to the user, that displays some input fields to start a request. There may be some predefined values
     *
     * @param provider the ID of the provider to get the timetable from
     */
    public static void selectTimetable(String provider) {
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
        render("Application/selectTimetable.html");
    }


    /**
     * Shows a webpage with some timetables to the user, depending on the given parameters
     *
     * @param provider the ID of the provider to get the timetable from
     */
    public static void showTimetable(String provider) {
        Validation.clear();
        NetworkProvider provider_object = InputChecker.getAndValidateProvider(provider);
        if (provider_object == null) {
            return;
        }
        //checks the given parameters and creates correct Objects. On error, create error output.
        HashSet<String> shownErrors = new HashSet<String>(1);
        HashSet<Location> starts = InputChecker.getAndValidateStations(provider_object, params.getAll("start[]"), "Abfahrtshaltestelle", "start", shownErrors);
        boolean isCrossover = InputChecker.getAndValidateBoolean(params.get("crossover"), true);
        HashSet<Location> stops = InputChecker.getAndValidateStations(provider_object, params.getAll("stop[]"), "Zielhaltestelle", "stop", shownErrors);
        if (!isCrossover && (starts.size() != stops.size())) {
            Validation.addError("crossover", "Wenn nicht alle Verbindungen von <b>allen</b> Abfahrtshaltestellen zu <b>allen</b> Zielhaltestellen gesucht werden, müssen gleich viele Abfahrts- wie Zielhaltestellen angegeben werden.");
        }
        Date datetime = InputChecker.getAndValidateTime(params.get("time"), "time");
        if (datetime == null) {
            datetime = new GregorianCalendar(KnownProvider.getTimeZone(provider_object)).getTime();
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
//            for (int i = 0; i < Validation.errors().size(); i++) {
//                Error error = Validation.errors().get(i);
//                flash.error(error.message());
//            }
            render("Application/selectTimetable.html");
        } else {
            //If everything is fine, create the desired timetables and print them out
            renderArgs.put("connections", ComplexRequests.getMultipleTimetables(provider_object, starts, isCrossover, stops, datetime, isTimeAsDeparture));
            render("Application/showTimetable.html");
        }
    }

}