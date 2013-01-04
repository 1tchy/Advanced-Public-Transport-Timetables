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

package test;

import org.junit.Test;
import play.libs.F;
import play.mvc.Result;
import play.test.TestBrowser;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

public class ApplicationTest {

    @Test
    public void testThatIndexPageWorks() {
        final Result index = routeAndCall(fakeRequest(GET, "/"));
        assertThat(status(index)).isEqualTo(OK);
        assertThat(contentType(index)).isEqualTo("text/html");
        assertThat(charset(index)).isEqualTo("utf-8");
        final String indexAsString = contentAsString(index);
        assertThat(indexAsString).contains("Neue Abfrage");
        assertThat(indexAsString).contains("FAQ");
        assertThat(indexAsString).contains("Der effiziente Fahrplan");
        assertThat(indexAsString).contains("Von");
        assertThat(indexAsString).contains("Verbindung");
        assertThat(indexAsString).contains("Nach");
        assertThat(indexAsString).contains("Ohne&nbsp;Umsteigen");
        assertThat(indexAsString).contains("Zeit");
        assertThat(indexAsString).contains("als Abfahrtszeit");
        assertThat(indexAsString).contains("als Ankunftszeit");
        assertThat(indexAsString).matches("[\\w\\W]*Verschiedene Haltestellenbezeichnungen an einem Ort als eine einzige[\\w\\W]*Haltestelle betrachten[\\w\\W]*");
    }

    @Test
    public void testIndexPageFromBrowser() {
        Map<String, String> config = new HashMap<>();
        config.put("smtp.mock", "true");
        running(testServer(3333, fakeApplication(config)), HTMLUNIT, new F.Callback<TestBrowser>() {
            @Override
            public void invoke(TestBrowser browser) throws Throwable {
                browser.goTo("http://localhost:3333/?from[]=Luzern");
                assertThat(browser.title()).isEqualTo("Der effiziente Fahrplan");
                browser.$(".faq").click();
                assertThat(browser.url()).isEqualTo("http://localhost:3333/faq");
                assertThat(browser.title()).isEqualTo("Der effiziente Fahrplan: FAQ");
            }
        });


    }
}