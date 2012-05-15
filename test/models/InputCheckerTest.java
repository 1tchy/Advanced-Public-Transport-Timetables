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
 * along with this program.  If not, see < http://www.gnu.org/licenses/ >.
 */

package models;

import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.Location;
import org.junit.Test;
import play.data.validation.Validation;
import play.test.UnitTest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: 23.11.11
 * Time: 10:13
 */
public class InputCheckerTest extends UnitTest {
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
    public void getAndValidateTime() {
        assert Math.abs(InputChecker.getAndValidateTime(new SimpleDateFormat("HH:mm").format(new Date()), "test", TimeZone.getDefault()).getTime() - new Date().getTime()) <= 1 : "InputChecker.getAndValidateTime of the current time in HH:mm should return again the current time.";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(InputChecker.getAndValidateTime("08:30", "test", TimeZone.getTimeZone("Europe/Zurich")));
        assert calendar.get(Calendar.HOUR_OF_DAY) == 8 : "The hour of InputChecker.getAndValidateTime(\"08:30\", \"test\") should be 8 but is " + calendar.get(Calendar.HOUR_OF_DAY);
        assert calendar.get(Calendar.MINUTE) == 30 : "The minutes of InputChecker.getAndValidateTime(\"08:30\", \"test\") should be 30 but is " + calendar.get(Calendar.MINUTE);
    }

    @Test
    public void getAndValidateTime_withTimeZones() {
        TimeZone inputTimezone = TimeZone.getTimeZone("GMT-5");
        TimeZone outputTimezone = TimeZone.getTimeZone("GMT-7");
        Date time = InputChecker.getAndValidateTime("08:30", "test", inputTimezone);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(outputTimezone);
        calendar.setTime(time);
        assert calendar.get(Calendar.HOUR_OF_DAY) == 6 : "Should return 6 but returned " + calendar.get(Calendar.HOUR_OF_DAY);
    }

    @Test
    public void getAndValidateStations() {
        NetworkProvider sbb = KnownProvider.get("sbb");
        Validation.clear();
        List<Location> validatedStations = InputChecker.getAndValidateStations(sbb, new String[]{"Olten", "Bern", "Zürich HB"}, "description", "fieldName", new HashSet<String>());
        assert !Validation.hasErrors() : "InputChecker.getAndValidateStations(sbb, new String[]{\"Olten\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) shouldn't return an error. But returned " + Validation.errors().toString();
        assert validatedStations.size() == 3 : "InputChecker.getAndValidateStations(sbb, new String[]{\"Olten\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) should return three stations but returned " + validatedStations.size() + ".";
        boolean o = false;
        boolean b = false;
        boolean z = false;
        for (Location l : validatedStations) {
            String l_name = l.name;
            if (l_name.contains("Olten")) o = true;
            if (l_name.contains("Bern")) b = true;
            if (l_name.contains("Zürich")) z = true;
        }
        assert o : "InputChecker.getAndValidateStations(sbb, new String[]{\"Oerlikon\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) didn't return Oerlikon.";
        assert b : "InputChecker.getAndValidateStations(sbb, new String[]{\"Oerlikon\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) didn't return Bern.";
        assert z : "InputChecker.getAndValidateStations(sbb, new String[]{\"Oerlikon\", \"Bern\", \"Zürich HB\"}, \"description\", \"fieldName\", new HashSet<String>()) didn't return Zürich.";
    }

    @Test
    public void getAndValidateProvider() {
        Validation.clear();
        try {
            assert InputChecker.getAndValidateProvider("sbb").autocompleteStations("A").size() > 0 : "Provider sbb should be able to return any station containing an A. Probably the Provider is not properly configured in KnownProvider.";
        } catch (IOException e) {
            e.printStackTrace();
            assert false : "Provider sbb should not throw an exception on lookup of a station conatining an 'A'.";
        }
        assert !Validation.hasErrors() : "InputChecker.getAndValidateProvider(\"sbb\").autocompleteStations(\"A\").size() should not result in an error.";
    }
}
