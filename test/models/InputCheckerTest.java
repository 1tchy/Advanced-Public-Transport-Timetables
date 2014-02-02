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

package models;

import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.Location;
import org.junit.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: 23.11.11
 * Time: 10:13
 */
public class InputCheckerTest {
    @Test
    public void getAndValidateBoolean() {
        assert InputChecker.getAndValidateBoolean("true", false) : "InputChecker.getAndValidateBoolean(\"true\", false) should return true.";
        assert InputChecker.getAndValidateBoolean("TRUE", false) : "InputChecker.getAndValidateBoolean(\"TRUE\", false) should return true.";
        assert InputChecker.getAndValidateBoolean("TRUE", true) : "InputChecker.getAndValidateBoolean(\"TRUE\", true) should return true.";
        assert InputChecker.getAndValidateBoolean("XYZ", true) : "InputChecker.getAndValidateBoolean(\"XYZ\", true) should return true.";
        assert !InputChecker.getAndValidateBoolean("XYZ", false) : "InputChecker.getAndValidateBoolean(\"XYZ\", false) should return false.";
        assert !InputChecker.getAndValidateBoolean("false", true) : "InputChecker.getAndValidateBoolean(\"false\", true) should return false.";
        assert !InputChecker.getAndValidateBoolean("FALSE", true) : "InputChecker.getAndValidateBoolean(\"FALSE\", true) should return false.";
        assert !InputChecker.getAndValidateBoolean("false", false) : "InputChecker.getAndValidateBoolean(\"false\", false) should return false.";
    }

    @Test
    public void getAndValidateTime() throws WrongParameterException {
        assertThat(Math.abs(InputChecker.getAndValidateTime(new SimpleDateFormat("HH:mm").format(new Date()), TimeZone.getDefault()).getTime() - new Date().getTime())).describedAs("InputChecker.getAndValidateTime of the current time in HH:mm should return again the current time.").isLessThanOrEqualTo(1);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(InputChecker.getAndValidateTime("08:30", TimeZone.getTimeZone("Europe/Zurich")));
        assertThat(calendar.get(Calendar.HOUR_OF_DAY)).describedAs("The hour of InputChecker.getAndValidateTime(\"08:30\", \"test\") should be 8 but is " + calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(8);
        assertThat(calendar.get(Calendar.MINUTE)).describedAs("The minutes of InputChecker.getAndValidateTime(\"08:30\", \"test\") should be 30 but is " + calendar.get(Calendar.MINUTE)).isEqualTo(30);
    }

    @Test
    public void getAndValidateTime_withTimeZones() throws WrongParameterException {
        TimeZone inputTimezone = TimeZone.getTimeZone("GMT-5");
        TimeZone outputTimezone = TimeZone.getTimeZone("GMT-7");
        Date time = InputChecker.getAndValidateTime("08:30", inputTimezone);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(outputTimezone);
        calendar.setTime(time);
        assertThat(calendar.get(Calendar.HOUR_OF_DAY)).describedAs("Should return 6 but returned " + calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(6);
    }

    @Test
    public void getAndValidateStations() {
        NetworkProvider sbb = KnownProvider.get("sbb");
        List<String> errors = new ArrayList<>();
        List<Location> validatedStations = InputChecker.getAndValidateStations(sbb, new String[]{"Olten", "Bern", "Zürich HB"}, "desc.", new HashSet<String>(), errors);
        assert errors.isEmpty() : "InputChecker.getAndValidateStations(sbb, new String[]{\"Olten\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) shouldn't return an error. But returned " + errors.toString();
        assertThat(validatedStations.size()).describedAs("InputChecker.getAndValidateStations(sbb, new String[]{\"Olten\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) should return three stations but returned " + validatedStations.size() + ".").isEqualTo(3);
        boolean o = false;
        boolean b = false;
        boolean z = false;
        for (Location l : validatedStations) {
            String l_name = l.name;
            if (l_name.contains("Olten")) {
                o = true;
            }
            if (l_name.contains("Bern")) {
                b = true;
            }
            if (l_name.contains("Zürich")) {
                z = true;
            }
        }
        assert o : "InputChecker.getAndValidateStations(sbb, new String[]{\"Oerlikon\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) didn't return Oerlikon.";
        assert b : "InputChecker.getAndValidateStations(sbb, new String[]{\"Oerlikon\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) didn't return Bern.";
        assert z : "InputChecker.getAndValidateStations(sbb, new String[]{\"Oerlikon\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) didn't return Zürich.";
    }

    @Test
    public void getAndValidateProvider() throws WrongParameterException {
//        Validation.clear();
        try {
            assertThat(InputChecker.getAndValidateProvider("sbb").autocompleteStations("A").size()).describedAs("Provider sbb should be able to return any station containing an A. Probably the Provider is not properly configured in KnownProvider.").isGreaterThan(0);
        } catch (IOException e) {
            e.printStackTrace();
            assert false : "Provider sbb should not throw an exception on lookup of a station conatining an 'A'.";
        }
        assert /*!Validation.hasErrors() */true : "InputChecker.getAndValidateProvider(\"sbb\").autocompleteStations(\"A\").size() should not result in an error.";
    }
}
