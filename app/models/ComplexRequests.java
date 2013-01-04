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
import de.schildbach.pte.OpenDataProvider;
import de.schildbach.pte.dto.Connection;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.QueryConnectionsResult;

import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: Oct 10, 2011
 * Time: 6:47:21 PM
 */
public class ComplexRequests {
    /**
     * Runs a request to get timetables from multiple froms/tos and mix them together (ordered by time of departure)
     * <p/>
     * //     * @param provider        a provider to ask for the timetables
     * //     * @param froms           some starting locations (or just one)
     * //     * @param crossover       if the timetables should be looked up all starts to all stops (and not only one start to the corresponding stop
     * //     * @param tos             some stopping locations (or just one)
     * //     * @param directs         if the connection between start and stop must be direct
     * //     * @param time            the time to for when to look up
     * //     * @param timeAsDeparture is this time assumed as the departure-time (and not the arrival)
     * //     * @param flexible        if the match of the stations can be flexible or has to be exact (Luzern matches also Luzern, Bahnhof)
     *
     * @param warnings A list of none critical error message (the critical are thrown in an exception)
     * @return a list of connections for the given request arguments (will not be null)
     */
    public static List<Connection> getMultipleTimetables(Request request, List<String> warnings) throws ServerNotReachableException {
        NetworkProvider provider = request.getProvider();
        Collection<Location> froms = request.from.getLocations();
        boolean isCrossover = request.crossover;
        Collection<Location> tos = request.to.getLocations();
        List<Boolean> directs = request.direct;
        Date datetime = request.getTime();
        boolean isTimeAsDeparture = request.timeAsDeparture;
        boolean flexible = request.flexible;
        //assures, that none-crossover-requests have the same amount of froms and tos
        assert isCrossover || froms.size() == tos.size() : "Anzahl Abfahrtshaltestellen und Zielhaltestellen bei der Nicht-Crossover Verbindung müssen gleich sein.";
        assert provider != null : "Could not find Provider \"" + request.provider + "\".";

        //creates the new connection set with an ordering by departure
        TreeSet<Connection> connections = new TreeSet<>(new FahrplanByDeparture());

        //update statistics
        PopularLocation.increasePopularities(KnownProvider.getId(provider), froms);
        PopularLocation.increasePopularities(KnownProvider.getId(provider), tos);

        //iterator for all tos (will be reset, if crossover
        Iterator<Location> stopsIterator = tos.iterator();
        Iterator<Boolean> directIterator = directs.iterator();
        assert tos.size() <= directs.size();
        //for each starting location, find the connections
        for (Location aFrom : froms) {
            do { //for each or (depending on "crossover" or not) just one stop location, find the connections
                //get the first/next stop
                Location aTo = stopsIterator.next();
                boolean direct = (isCrossover ? directs.get(0) : directIterator.next());
                try {
                    //try to get a set of connections for one combination of froms/tos
                    QueryConnectionsResult result;
                    if (provider instanceof OpenDataProvider) {
                        result = ((OpenDataProvider) provider).queryConnections(aFrom, null, aTo, datetime, isTimeAsDeparture, null, direct, flexible, warnings);
                    } else {
                        result = provider.queryConnections(aFrom, null, aTo, datetime, isTimeAsDeparture, null, NetworkProvider.WalkSpeed.FAST, NetworkProvider.Accessibility.NEUTRAL);
                    }

                    //if there is at least one connection returned, add it to the list to return
                    if (result != null && result.connections != null && result.connections.size() > 0) {
                        connections.addAll(result.connections);
                    }
                } catch (IOException e) {
                    throw new ServerNotReachableException(e);
                }
            }
            while (isCrossover && stopsIterator.hasNext()); //loop over all tos (if crossover connections are requested and there is a next stopping location)
            //if we want connections crossover, refill the iterator of the stopping locations
            if (isCrossover) {
                stopsIterator = tos.iterator();
                directIterator = directs.iterator();
            }
        }
        return new ArrayList<>(connections);
    }

    public static class ServerNotReachableException extends IOException {
        private static final String exceptionText = "Konnte nicht zum Fahrplananbieter verbinden";

        public ServerNotReachableException(IOException ex) {
            super(ex);
        }

        @Override
        public String getMessage() {
            return exceptionText + " (" + super.getMessage() + ")";
        }

        @Override
        public String getLocalizedMessage() {
            return exceptionText + " (" + super.getLocalizedMessage() + ")";
        }
    }
}
