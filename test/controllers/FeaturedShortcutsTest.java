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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;

import java.util.HashMap;
import java.util.Map;

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

    private static FakeApplication app;

    @BeforeClass
    public static void startApp() throws Throwable {
        Map<String, String> config = new HashMap<>();
        config.put("smtp.mock", "true");
        app = Helpers.fakeApplication(config);
        Helpers.start(app);
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @Test
    public void onlyOneStationGiven_crossover() {
        Result result = route(fakeRequest("GET", "/x/Bern"));
        assertThat(status(result)).isEqualTo(MOVED_PERMANENTLY);
        assertThat(header("Location", result)).endsWith("/timetable?crossover=true&from[]=Bern&to[]=Luzern");
    }

    @Test
    public void onlyOneStationGiven_1to1_retour() {
        Result result = route(fakeRequest("GET", "/1-1/Bern/Olten?retour=1"));
        assertThat(status(result)).isEqualTo(MOVED_PERMANENTLY);
        assertThat(header("Location", result)).endsWith("/timetable?crossover=false&from[]=Olten&to[]=Bern");
    }

    @Test
    public void severalStationsGiven_crossover() {
        Result result = route(fakeRequest("GET", "/x/Mühlau&Mettmenstetten&Knonau/Luzern"));
        assertThat(status(result)).isEqualTo(MOVED_PERMANENTLY);
        assertThat(header("Location", result)).endsWith("/timetable?crossover=true&from[]=Mühlau&from[]=Mettmenstetten&from[]=Knonau&to[]=Luzern");
    }

    @Test
    public void severalStationsGiven_1to1() {
        Result result = route(fakeRequest("GET", "/1-1/Z%C3%BCrich%20HB&Z%C3%BCrich,%20BahnhofquaiHB/Z%C3%BCrich%20Oerlikon&Z%C3%BCrich,%20Regensbergbr%C3%BCcke"));
        assertThat(status(result)).isEqualTo(MOVED_PERMANENTLY);
        assertThat(header("Location", result)).endsWith("/timetable?crossover=false&from[]=Zürich HB&from[]=Zürich, BahnhofquaiHB&to[]=Zürich Oerlikon&to[]=Zürich, Regensbergbrücke");
    }

    @Test
    public void checkForwardTargetExists() {
        Result forwarding = route(fakeRequest("GET", "/x/Luzern&Luzern,%20Steghof/Z%C3%BCrich%20Oerlikon&Z%C3%BCrich,%20Regensbergbr%C3%BCcke?retour=1"));
        assertThat(status(forwarding)).isEqualTo(MOVED_PERMANENTLY);
        String targetURL = header("Location", forwarding).replaceFirst("http://", "");
        assertThat(targetURL).endsWith("/timetable?crossover=true&from[]=Zürich Oerlikon&from[]=Zürich, Regensbergbrücke&to[]=Luzern&to[]=Luzern, Steghof");
        System.out.println("targetURL = " + targetURL);
        Result target = routeAndCall(fakeRequest("GET", targetURL));
        assertThat(status(target)).isEqualTo(OK);
        assertThat(contentAsString(target)).contains("Verbindungen von Zürich Oerlikon, Zürich Regensbergbrücke nach Luzern, Luzern Steghof");
    }
}
