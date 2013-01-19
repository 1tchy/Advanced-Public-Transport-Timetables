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

import org.apache.commons.lang3.StringUtils;
import play.mvc.Call;
import play.mvc.Controller;
import play.mvc.Result;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 05.01.13
 * Time: 12:02
 */
public class FeaturedShortcuts extends Controller {

    private static final String STATION_SEPARATOR = "&";
    private static final String STATION_SEPARATOR_REGEX = "\\s*" + STATION_SEPARATOR + "\\s*";

    /**
     * Forwards the user to the real address based on a featured shortcut.
     *
     * @param stations1     One of the featured stations (see code) or some starting stations (separated with ";")
     * @param stations2     Some to stations (separated with ";")
     * @param crossover_int If the connections should be found crossover
     * @param retour_int    If the connections from and to should be exchanged
     * @return Forwarding to the real address
     */
    public static Result forward(String stations1, String stations2, Integer crossover_int, Integer retour_int) {
        boolean crossover = crossover_int > 0; //todo: Play 2.1: make real Boolean-Parameter
        boolean retour = retour_int > 0; //todo: Play 2.1: make real Boolean-Parameter
        ShortcutRequest request = new ShortcutRequest(stations1, stations2, retour);
        if (!crossover && !request.numberOfFromsSameAsNumberOfTos()) {
            return badRequest("1-1-Requests müssen gleich viele Von-Stationen wie Nach-Stationen haben.");
        }
        return movedPermanently(createForwardUrl(request, crossover));
    }

    private static Call createForwardUrl(ShortcutRequest request, boolean crossover) {
        List<String> params = new ArrayList<>();
        params.add("crossover=" + (crossover ? "true" : "false"));
        for (String station : request.getFroms(crossover)) {
            params.add("from[]=" + station);
        }
        for (String station : request.getTos(crossover)) {
            params.add("to[]=" + station);
        }
        String url = "http://" + request().host() + routes.Application.showTimetable().url() + "?" + StringUtils.join(params, "&");
        return new play.api.mvc.Call("GET", url);
    }

//    private static Map<String, String> createForwardGET(ShortcutRequest shortcutRequest, boolean crossover) {
//        Map<String, String> get = new HashMap<>();
//        get.put("crossover", crossover ? "true" : "false");
//        get.put("provider", "sbb");
//        get.put("timeAsDeparture", "true");
//        StringBuilder request = new StringBuilder();
//        int i = 0;
//        for (String from : shortcutRequest.getFroms(crossover)) {
//            get.put("from[" + (i++) + "]", from);
//            request.append("from[]=").append(from).append("&");
//        }
//        get.put("from[" + (i) + "]", "");
//        request.append("from[]=&");
//        i = 0;
//        for (String to : shortcutRequest.getTos(crossover)) {
//            get.put("to[" + (i++) + "]", to);
//            request.append("to[]=").append(to).append("&");
//        }
//        get.put("to[" + (i) + "]", "");
//        request.append("to[]=&");
//        request.append("crossover=").append(crossover ? "true" : "false");
//        request.append("&timeAsDeparture=true&flexible=true");
//        get.put("requestString", request.toString());
//        return get;
//    }

    private static class ShortcutRequest {
        private ArrayList<String> froms = new ArrayList<>();
        private ArrayList<String> tos = new ArrayList<>();

        private ShortcutRequest(String froms, String tos, boolean retour) {
            setFroms(froms);
            setTos(tos);
            if (retour) {
                makeRetour();
            }
        }

        public Collection<String> getFroms(boolean crossover) {
            if (froms.size() == 0) {
                if (crossover) {
                    setFroms("Maschwanden");
                } else {
                    setFroms("Regensbergbrücke");
                }
            }
            return froms;
        }

        public void setFroms(String froms) {
            processInput(froms, this.froms, this.tos);
        }

        public boolean numberOfFromsSameAsNumberOfTos() {
            return froms.size() == tos.size();
        }

        public Collection<String> getTos(boolean crossover) {
            if (tos.size() == 0) {
                if (crossover) {
                    tos.add("Luzern");
                } else {
                    for (String ignored : froms) {
                        tos.add("Luzern");
                    }
                }
            }
            return tos;
        }

        public void setTos(String tos) {
            processInput(tos, this.tos, this.froms);
        }

        public void makeRetour() {
            ArrayList<String> tmp = froms;
            froms = tos;
            tos = tmp;
        }

        /**
         * Calculates the list of stations based on the input strings
         *
         * @param inputStations    List of stations given by the user
         * @param froms_or_tos     The array list containing the froms or the tos
         * @param not_froms_or_tos The other array list (if the previous was the "tos", then the "froms" or otherwise)
         */
        private void processInput(@Nullable String inputStations, ArrayList<String> froms_or_tos, ArrayList<String> not_froms_or_tos) {
            if (inputStations != null) {
                froms_or_tos.clear();
                for (String station : inputStations.split(STATION_SEPARATOR_REGEX)) {
                    if (station.equals("Maschwanden")) {
                        addAll(froms_or_tos, "Mettmenstetten", "Mühlau");
                        if (not_froms_or_tos.isEmpty()) {
                            not_froms_or_tos.add("Luzern");
                        }
                    } else if (station.startsWith("Regensbergbr")) {
                        addAll(froms_or_tos, "Zürich, Regensbergbrücke", "Zürich Oerlikon");
                        if (not_froms_or_tos.isEmpty()) {
                            addAll(not_froms_or_tos, "Zürich Bahnhofquai/HB", "Zürich HB");
                        }
                    } else if (station.equals("Würzenbach")) {
                        addAll(froms_or_tos, "Luzern, Brüel", "Luzern, Brüelstrasse", "Luzern Verkehrshaus");
                        if (not_froms_or_tos.isEmpty()) {
                            addAll(not_froms_or_tos, "Luzern, Bahnhof", "Luzern, Bahnhof", "Luzern");
                        }
                    } else {
                        froms_or_tos.add(station);
                    }
                }
            }
        }

        private static void addAll(ArrayList<String> to, String... elements) {
            Collections.addAll(to, elements);
        }
    }
}
