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

package test.controllers;

import org.junit.Test;
import play.mvc.Result;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.MOVED_PERMANENTLY;
import static play.test.Helpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 19.01.13
 * Time: 14:42
 */
public class FeaturedShortcutsTest {

    @Test
    public void onlyOneStationGiven_crossover() {
        Result result = routeAndCall(fakeRequest("GET", "/x/Bern"));
        assertThat(status(result)).isEqualTo(MOVED_PERMANENTLY);
        assertThat(header("Location", result)).endsWith("/timetable?crossover=true&from[]=Bern&to[]=Luzern");
    }

    @Test
    public void onlyOneStationGiven_1to1_retour() {
        Result result = routeAndCall(fakeRequest("GET", "/1-1/Bern/Olten?retour=1"));
        assertThat(status(result)).isEqualTo(MOVED_PERMANENTLY);
        assertThat(header("Location", result)).endsWith("/timetable?crossover=false&from[]=Olten&to[]=Bern");
    }

    @Test
    public void severalStationsGiven_crossover() {
        Result result = routeAndCall(fakeRequest("GET", "/x/Mühlau&Mettmenstetten&Knonau/Luzern"));
        assertThat(status(result)).isEqualTo(MOVED_PERMANENTLY);
        assertThat(header("Location", result)).endsWith("/timetable?crossover=true&from[]=Mühlau&from[]=Mettmenstetten&from[]=Knonau&to[]=Luzern");
    }

    @Test
    public void severalStationsGiven_1to1() {
        Result result = routeAndCall(fakeRequest("GET", "/1-1/Zürich HB&Zürich, BahnhofquaiHB/Zürich Oerlikon&Zürich, Regensbergbrücke"));
        assertThat(status(result)).isEqualTo(MOVED_PERMANENTLY);
        assertThat(header("Location", result)).endsWith("/timetable?crossover=false&from[]=Zürich HB&from[]=Zürich, BahnhofquaiHB&to[]=Zürich Oerlikon&to[]=Zürich, Regensbergbrücke");
    }

    @Test
    public void checkForwardTargetExists() {
        Result forwarding = routeAndCall(fakeRequest("GET", "/x/Luzern&Luzern, Steghof/Zürich Oerlikon&Zürich, Regensbergbrücke?retour=1"));
        assertThat(status(forwarding)).isEqualTo(MOVED_PERMANENTLY);
        String targetURL = header("Location", forwarding).replaceFirst("http://", "");
        assertThat(targetURL).endsWith("/timetable?crossover=true&from[]=Zürich Oerlikon&from[]=Zürich, Regensbergbrücke&to[]=Luzern&to[]=Luzern, Steghof");
        System.out.println("targetURL = " + targetURL);
        Result target = routeAndCall(fakeRequest("GET", targetURL));
        assertThat(status(target)).isEqualTo(OK);
        assertThat(contentAsString(target)).contains("Verbindungen von Zürich Oerlikon, Zürich Regensbergbrücke nach Luzern, Luzern Steghof");
    }
}
