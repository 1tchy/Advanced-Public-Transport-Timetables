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

package models;

import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.Location;
import org.junit.*;
import play.test.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeSet;

public class ComplexRequestsTest extends UnitTest {
    /**
     * cached return values for getMultipleTimetables
     * access with the extra-method (multipleConnection(...))
     */
    private ArrayList<TreeSet<Connection>> multipleConnections;

    /**
     * Contains exception errors that happened during setup
     */
    private Exception setUpExceptions = null;

    /**
     * Prepares some multipleConnections to perform tests with
     */
    @Before
    public void setUp() {
        multipleConnections = new ArrayList<TreeSet<Connection>>();
        final NetworkProvider sbb = KnownProvider.get("sbb");
        HashSet<Location> oneLocation1 = new HashSet<Location>(1);
        HashSet<Location> oneLocation2 = new HashSet<Location>(1);
        HashSet<Location> threeLocations1 = new HashSet<Location>(3);
        HashSet<Location> threeLocations2 = new HashSet<Location>(3);
        try {
            oneLocation1.add(sbb.autocompleteStations("Bern").get(0));
            oneLocation2.add(sbb.autocompleteStations("Zug").get(0));
            threeLocations1.add(sbb.autocompleteStations("Luzern").get(0));
            threeLocations1.add(sbb.autocompleteStations("Basel").get(0));
            threeLocations1.add(sbb.autocompleteStations("Z�rich HB").get(0));
            threeLocations2.add(sbb.autocompleteStations("Chur").get(0));
            threeLocations2.add(sbb.autocompleteStations("Olten").get(0));
            threeLocations2.add(sbb.autocompleteStations("Lugano").get(0));
        } catch (IOException e) {
            setUpExceptions = e;
        }
        ArrayList<HashSet<Location>> froms = new ArrayList<HashSet<Location>>(2);
        froms.add(oneLocation1);
        froms.add(threeLocations1);
        ArrayList<HashSet<Location>> tos = new ArrayList<HashSet<Location>>(2);
        tos.add(oneLocation2);
        tos.add(threeLocations2);
        for (boolean crossover : new boolean[]{true, false}) {
            for (boolean asDeparture : new boolean[]{true, false}) {
                for (HashSet<Location> from : froms) {
                    for (HashSet<Location> to : tos) {
                        multipleConnections.add(ComplexRequests.getMultipleTimetables(sbb, from, crossover, to, new Date(1319178289), asDeparture));
                    }
                }
            }
        }
    }

    /**
     * Gets one of the set up connections by parameters
     *
     * @param crossover   by crossover connection?
     * @param asDeparture by time as departure?
     * @param fromOne     with a single start location?
     * @param toOne       with a single destination location?
     * @return the return of getMultipleTimetables with items with these parameters
     */
    private TreeSet<Connection> multipleConnection(boolean crossover, boolean asDeparture, boolean fromOne, boolean toOne) {
        final int total = multipleConnections.size();
        int i = 0;
        if (!crossover) i += total / 2;
        if (!asDeparture) i += total / 4;
        if (!fromOne) i += 2;
        if (!toOne) i += 1;
        return multipleConnections.get(i);
    }

    /**
     * Verifies that the setup worked
     */
    @Test
    public void setUpOk() {
        assertNull(setUpExceptions);
    }

    @Test
    public void test1() {
        for (boolean crossover : new boolean[]{true, false}) {
            for (boolean asDeparture : new boolean[]{true, false}) {
                for (boolean fromOne : new boolean[]{true, false}) {
                    for (boolean toOne : new boolean[]{true, false}) {

                    }
                }
            }
        }
    }

}