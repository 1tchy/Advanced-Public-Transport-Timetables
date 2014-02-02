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
import de.schildbach.pte.dto.Connection;
import org.junit.Test;

import java.util.*;

import static org.fest.assertions.Assertions.assertThat;

public class ComplexRequestsTest {

    /**
     * Tests all possible complex combinations
     */
    @Test
    public void theComplexRequestTest() throws ComplexRequests.ServerNotReachableException {
        //Preparations
        Set<String> oneFromLocation = new HashSet<>(1);
        Set<String> oneToLocation = new HashSet<>(1);
        Set<String> threeFromLocations = new HashSet<>(3);
        Set<String> threeToLocations = new HashSet<>(3);
        oneFromLocation.add("Arth-Goldau");
        oneToLocation.add("Zug");
        threeFromLocations.add("Luzern");
        threeFromLocations.add("Basel");
        threeFromLocations.add("Zürich HB");
        threeToLocations.add("Chur");
        threeToLocations.add("Olten");
        threeToLocations.add("Lugano");
        final boolean CROSSOVER = true;
        final boolean ONE_TO_ONE = false;
        final boolean AS_ARRIVAL = false;
        final boolean AS_DEPARTURE = true;
        //Tests
        doTest(CROSSOVER, AS_ARRIVAL, oneFromLocation, oneToLocation);
        doTest(ONE_TO_ONE, AS_ARRIVAL, oneFromLocation, oneToLocation);
        doTest(CROSSOVER, AS_DEPARTURE, threeFromLocations, threeToLocations);
        doTest(ONE_TO_ONE, AS_DEPARTURE, threeFromLocations, threeToLocations);
        doTest(CROSSOVER, AS_DEPARTURE, oneFromLocation, threeToLocations);
    }

    private static void doTest(boolean crossover, boolean asDeparture, Set<String> from, Set<String> to) throws ComplexRequests.ServerNotReachableException {
        //Start the request for the given parameters
        final List<Connection> cs = ComplexRequests.getMultipleTimetables(prepareRequest(from, crossover, to, asDeparture), new ArrayList<String>());
        //Validate the request for the given parameters
        validateRequest(from, crossover, to, asDeparture, cs);
    }

    private static void validateRequest(Set<String> froms, boolean crossover, Set<String> tos, boolean asDeparture, List<Connection> result) {
        final int amountOfOneConnectionMin = 3;
        final int amountOfOneConnectionMax = 6;
        int expected;
        if (crossover) {
            expected = froms.size() * tos.size();
        } else {
            assert froms.size() == tos.size();
            expected = froms.size();
        }
        assertThat(result.size()).describedAs("Ammount of connections (crossover=true, asDeparture=" + (asDeparture ? "true" : "false") + ", from=" + froms + ", to=" + tos + ")").isGreaterThanOrEqualTo(expected * amountOfOneConnectionMin).isLessThanOrEqualTo(expected * amountOfOneConnectionMax);
        long differenceFirstDep = Math.abs(result.iterator().next().getFirstTripDepartureTime().getTime() - requestTime());
        long differenceLastArr = 0;
        for (Connection connection : result) {
            differenceLastArr = Math.abs(connection.getFirstTripDepartureTime().getTime() - requestTime());
        }
        if (asDeparture) {
            assertThat(differenceFirstDep).describedAs("The requested time should be nearer to the first departure time than to the last arrival time").isLessThan(differenceLastArr);
        } else {
            assertThat(differenceLastArr).describedAs("The requested time should be nearer to the last arrival time than to the first departure time").isLessThan(differenceFirstDep);
        }
    }

    private static long requestTime() {
        Calendar calendar = new GregorianCalendar();
        if (calendar.get(Calendar.HOUR_OF_DAY) > 15) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static Request prepareRequest(Set<String> from, boolean crossover, Set<String> to, boolean asDeparture) {
        Request request = new Request();
        for (String aFrom : from) {
            request.addFrom(aFrom);
        }
        NetworkProvider provider = KnownProvider.get("sbb");
        request.from.calculateLocations(provider, "test-froms");
        request.setCrossover(crossover ? "1" : "0");
        for (String aTo : to) {
            request.addTo(aTo);
        }
        request.to.calculateLocations(provider, "test-tos");
        request.addDirect("0");
        request.addDirect("0");
        request.addDirect("0");
        request.setTime("14:00");
        request.setTimeAsDeparture(asDeparture ? "1" : "0");
        request.setFlexible("1");
        request.setProvider("sbb");
        return request;
    }

}
