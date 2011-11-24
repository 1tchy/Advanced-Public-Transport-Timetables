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

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import org.junit.Test;
import play.test.UnitTest;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: 23.11.11
 * Time: 18:25
 */
public class PopularLocationTest extends UnitTest {
    @Test
    public void increasePopularities() {
        Location l1 = new Location(LocationType.STATION, 12345, "Schöner Ort 1", "Schöner Ort 1");
        Location l2 = new Location(LocationType.STATION, 23456, "Schöner Ort 2", "Schöner Ort 2");
        Location l3 = new Location(LocationType.STATION, 34567, "Schöner Ort 3", "Schöner Ort 3");
        Collection<Location> c = new HashSet<Location>();
        c.add(l1);
        c.add(l2);
        c.add(l3);
        for (int i = 1; i < 500; i++) {
            PopularLocation.increasePopularities("sbb", c);
        }
        c.clear();
        c.add(l2);
        for (int i = 1; i < 500; i++) {
            PopularLocation.increasePopularities("sbb", c);
        }
        List<String> theOneMostPopular = PopularLocation.getMostPopularLike("sbb", "Schöner", 1);
        assert theOneMostPopular.size() == 1 : "The one most popular should be a single one but is " + theOneMostPopular + ".";
        assert theOneMostPopular.get(0).equals(l2.name) : "The one most popular one should be l2 but is " + theOneMostPopular + ".";
        List<String> threePopular = PopularLocation.getMostPopularLike("sbb", "Schöner", 3);
        assert threePopular.size() == 3 : "The three most popular should be three items but are " + theOneMostPopular + ".";
        assert threePopular.get(0).equals(l2.name) : "The first of the three most popular one should be l2 but is " + theOneMostPopular.get(0) + ".";
        assert ((threePopular.get(1).equals(l1.name) && threePopular.get(2).equals(l3.name)) || (threePopular.get(1).equals(l3.name) && threePopular.get(2).equals(l1.name))) : "The second and third of the most popular items should be l1 and l3 but are " + threePopular.get(1) + " and " + threePopular.get(2) + ".";
        assert PopularLocation.getMostPopularLike("sbb", "Schöner", 10).size() == 3 : "I think there should be only three matches but there are " + PopularLocation.getMostPopularLike("sbb", "Schöner", 10) + " = total " + PopularLocation.getMostPopularLike("sbb", "Schöner", 10).size() + ".";
        assert PopularLocation.getMostPopularLike("sbb", " Ort ", 10).size() == 3 : "' Ort ' should also match three times but did " + PopularLocation.getMostPopularLike("sbb", " Ort ", 10) + ".";
        assert PopularLocation.getMostPopularLike("asdf", " Ort ", 10).size() == 0 : "For an unknown provider, there should not be any result. But got: " + PopularLocation.getMostPopularLike("asdf", " Ort ", 10) + ".";
        assert PopularLocation.getMostPopularLike(null, " Ort ", 10).size() == 0 : "For no provider, there should not be any result. But got: " + PopularLocation.getMostPopularLike(null, " Ort ", 10) + ".";
        assert PopularLocation.getMostPopularLike("sbb", null, 10).size() == 0 : "No string to search should not return any result. But returned " + PopularLocation.getMostPopularLike("sbb", null, 10) + ".";
    }
}
