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

package controllers;

import actions.MailAction;
import de.schildbach.pte.dto.Connection;
import models.ComplexRequests;
import models.Request;
import play.Logger;
import play.api.templates.Html;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import tags.FastTags;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Application extends Controller {

    /**
     * Shows a webpage to the user, that displays some input fields to start a request. There may be some predefined values
     */
    @With(MailAction.class)
    public static Result index() {
        Collection<String> errors = new HashSet<>();
        Request request = getGetRequest(errors);
        Result redirectOldRequests = redirectOldRequests();
        if (redirectOldRequests != null) return redirectOldRequests;
        return index(request, errors);
    }

    /**
     * Shows a webpage to the user, that displays some input fields to start a request. There may be some predefined values
     * //@param provider the ID of the provider to get the timetable from
     */
    private static Status index(@NotNull Request request, @NotNull Collection<String> errors) {
        return ok(views.html.Application.index.render(request, errors));
    }

    /**
     * Gets the current GET-Request with corrected Array-Numbers
     *
     * @return The current GET-Request
     */
    private static Request getGetRequest(Collection<String> errors) {
        final Http.Request httpRequest = request();
        final Map<String, String[]> gets = new TreeMap<>(httpRequest.queryString());
        final Map<String, String> get = new TreeMap<>();
        final Map<String, Integer> occurrences = new HashMap<>(gets.size());
        final Pattern array_delimiter = Pattern.compile("(.*)\\[\\]");
        //Add fix values
        get.put("provider", "sbb");
        final Matcher httpRequestMatcher = Pattern.compile("[^\\?]*\\?(.*)").matcher(httpRequest.uri());
        if (httpRequestMatcher.matches()) {
            get.put("requestString", httpRequestMatcher.group(1));
        }
        //prepare each GET with the correct [\d]
        for (String key : gets.keySet()) {
            final String[] value = gets.get(key);
            final Matcher key_match = array_delimiter.matcher(key);
            if (key_match.matches()) {
                key = key_match.group(1);
                if (value == null || value.length == 0) {
                    get.put(key, null);
                } else {
                    for (String v : value) {
                        int occurrence;
                        if (occurrences.containsKey(key)) {
                            occurrence = occurrences.get(key) + 1;
                        } else {
                            occurrence = 0;
                        }
                        final String key_array_name = key + "[" + occurrence + "]";
                        while (gets.keySet().contains(key_array_name)) {
                            occurrence++;
                        }
                        occurrences.put(key, occurrence);
                        get.put(key_array_name, v);
                    }
                }
            } else {
                String value_to_put = null;
                if (value != null && value.length > 0) {
                    value_to_put = value[0];
                }
                get.put(key, value_to_put);
            }
        }
        return processGETvalues(get, errors);
    }

    protected static Request processGETvalues(Map<String, String> get, Collection<String> errors) {
        final Request request = myBind(get, errors);
        while (request.from.size() < 2) {
            request.from.add("");
        }
        while (request.to.size() < 2) {
            request.to.add("");
        }
        if (request().uri().length() > 1) {
            errors.addAll(request.validate());
        }
        return request;
    }

    private static Request myBind(Map<String, String> get, Collection<String> errors) {
        final Request ret = new Request();
        for (String key : get.keySet()) {
            try {
                final String value = get.get(key);
                final Matcher matcher = Pattern.compile("(.)(.*)\\[\\d*\\]").matcher(key);
                if (matcher.matches()) {
                    Request.class.getDeclaredMethod("add" + matcher.group(1).toUpperCase() + matcher.group(2), String.class).invoke(ret, value);
                } else {
                    Method method = null;
                    for (Method m : Request.class.getDeclaredMethods()) {
                        if (m.getParameterTypes().length == 1 &&
                                m.getParameterTypes()[0] == String.class &&
                                ("set" + key).toLowerCase().equals(m.getName().toLowerCase())) {
                            method = m;
                            break;
                        }
                    }
                    if (method == null) {
                        Request.class.getDeclaredField(key).set(ret, value);
                    } else {
                        method.invoke(ret, value);
                    }
                }
            } catch (Exception e) {
                Logger.error("Unbekannter Parameter '" + key + "'.", e);
                errors.add("Unbekannter Parameter '" + key + "'.");
            }
        }
        return ret;
    }

    private static Result redirectOldRequests() {
        String uri = request().uri();
        if (uri.contains("start[") || uri.contains("stop[") || uri.contains("direkt[") || uri.contains("start%5B") || uri.contains("stop%5B") || uri.contains("direkt%5B")) {
            String newUri = uri.replace("start", "from").replace("stop", "to").replace("direkt", "direct");
            Logger.warn("Forwarded " + uri);
            Logger.warn("       to " + newUri);
            return movedPermanently(newUri);
        } else {
            return null;
        }
    }


    /**
     * Shows a webpage with some timetables to the user, depending on the given parameters
     */
    @With(MailAction.class)
    public static Result showTimetable() {
        Result redirectOldRequests = redirectOldRequests();
        if (redirectOldRequests != null) return redirectOldRequests;
        Set<String> errors = new HashSet<>();
        Request request = getGetRequest(errors);
        if (!errors.isEmpty()) {
            return index(request, errors);
        }
        return getTimetable(request, errors);
    }

    protected static Result getTimetable(Request request, Set<String> errors) {
        try {
            List<String> warnings = new ArrayList<>();
            List<Connection> connections = ComplexRequests.getMultipleTimetables(request, warnings);
            return ok(views.html.Application.showTimetable.render(request, connections, warnings, new FastTags.IteratorExtender()));
        } catch (ComplexRequests.ServerNotReachableException e) {
            errors.add(e.getLocalizedMessage());
            return index(request, errors);
        }
    }

    /**
     * "Moves permanently" the GET into a request without trailing slash
     *
     * @param path String
     * @return Result
     */
    @With(MailAction.class)
    public static Result redirectUntrailed(String path) {
        String uri = request().uri();
        if (uri.contains("/?")) {
            uri = uri.replaceFirst("/?", "?");
        } else {
            uri = path;
        }
        return movedPermanently(uri);
    }

    @Nullable
    public static Html getErrorPage(@NotNull Throwable e) {
        Html result;
        if (Math.random() > 0.03) { //show almost always, but prevent from a loop
            Collection<String> errors = new HashSet<>();
            errors.add("Ein internet Fehler ist aufgetreten. Bitte mit anderen Daten oder später nochmals versuchen. <br><span style=\"font-size:smaller\">(<b>Error:</b> " + e.getLocalizedMessage() + ")</span>");
            result = views.html.Application.index.render(new Request(), errors);
        } else {
            result = null;
        }
        return result;
    }
}