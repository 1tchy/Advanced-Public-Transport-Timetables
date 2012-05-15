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
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import play.data.validation.Validation;
import play.libs.Mail;
import play.mvc.Catch;
import play.mvc.Controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Application extends Controller {

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
        //if no field is already set, make sure at the beginning two fields are displayed
        if (allParams.size() <= 1) {
            allParams.put("start[]", new String[]{null});
            allParams.put("stop[]", new String[]{null});
        }
        //display all fields
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
        List<Location> starts = InputChecker.getAndValidateStations(provider_object, params.getAll("start[]"), "Abfahrtshaltestelle", "start", shownErrors);
        boolean isCrossover = InputChecker.getAndValidateBoolean(params.get("crossover"), true);
        List<Location> stops = InputChecker.getAndValidateStations(provider_object, params.getAll("stop[]"), "Zielhaltestelle", "stop", shownErrors);
        if (!isCrossover && (starts.size() != stops.size())) {
            Validation.addError("crossover", "Wenn nicht alle Verbindungen von <b>allen</b> Abfahrtshaltestellen zu <b>allen</b> Zielhaltestellen gesucht werden, müssen gleich viele Abfahrts- wie Zielhaltestellen angegeben werden.");
        }
        Date datetime = InputChecker.getAndValidateTime(params.get("time"), "time", KnownProvider.getTimeZone(provider_object));
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
                renderArgs.put("connections", ComplexRequests.getMultipleTimetables(provider_object, starts, isCrossover, stops, datetime, isTimeAsDeparture));
            } catch (ComplexRequests.ServerNotReachableException e) {
                Validation.addError("general", e.getLocalizedMessage());
                showInputPageAgain();
                return;
            }
            renderArgs.put("starts", starts);
            renderArgs.put("stops", stops);
            render("Application/showTimetable.html");
        }
    }

    private static void showInputPageAgain() {
        Map<String, String[]> allParams = params.all();
        for (String param : allParams.keySet()) {
            renderArgs.put(param.replace("[]", ""), (param.endsWith("[]") ? allParams.get(param) : allParams.get(param)[0]));
        }
        Validation.keep();
        render("Application/index.html");
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
            showInputPageAgain();
        }
    }
}