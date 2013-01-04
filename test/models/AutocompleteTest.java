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

package test.models;/*
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

import de.schildbach.pte.dto.Location;
import models.Autocomplete;
import models.InputChecker;
import models.KnownProvider;
import models.PopularLocation;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class AutocompleteTest {

    @Test
    public void generalTest() {
        List<String> l_stations = Autocomplete.stations("sbb", "Lu");
        assertThat(l_stations.size()).describedAs("An autocompleted station containing 'Lu' should be available from SBB.").isEqualTo(5);
        for (String station : l_stations) {
            assert station.toLowerCase().contains("lu") : "The autocompleted station " + station + " does not contain the expected 'lu'.";
        }
        List<String> singleStation = Autocomplete.stations("sbb", "Zürich, Regensb");
        assertThat(singleStation.size()).describedAs("SBB is only expected to give one result for 'Zürich, Regensb' But gives " + singleStation.size() + ".").isEqualTo(1);
        assert singleStation.get(0).equals("Zürich, Regensbergbrücke") : "The result of the provider SBB for the request 'Zürich, Regensb' should not be '" + singleStation.get(0) + "'.";
        singleStation = Autocomplete.stations("sbb", "Luzern, Tiefe");
        assertThat(singleStation.size()).describedAs("SBB is only expected to give one result for 'Luzern, Tiefe' But gives " + singleStation + ".").isEqualTo(1);
        assert singleStation.get(0).equals("Luzern, Tiefe") : "The result of the provider SBB for the request 'Luzern, Tiefe' should not be '" + singleStation.get(0) + "'.";
    }

    @Test
    public void includPopularStationsTest() {
        //rise popularity
        String[] popular = {"Luzern, Brünigstrasse", "Luzern, Bundesplatz", "Luzern, Tiefe", "Luzern, Kantonalbank", "Luzern, Hubelmatt"};
        List<Location> popularL = InputChecker.getAndValidateStations(KnownProvider.get("sbb"), popular, "description", new HashSet<String>(), new ArrayList<String>());
        for (int i = 500; i > 0; i--) {
            PopularLocation.increasePopularities("sbb", popularL);
        }
        //check result
        List<String> autocompleted = Autocomplete.stations("sbb", "Luzern");
        for (String pop : popular) {
            boolean contained = false;
            for (String auto : autocompleted) {
                if (auto.contains(pop)) {
                    contained = true;
                    break;
                }
            }
            assert contained : pop + " should have been made popular, but it's not in the populated autocomplete list: " + autocompleted.toString();
        }
    }

}
