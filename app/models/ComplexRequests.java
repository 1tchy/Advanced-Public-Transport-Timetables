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
     * Runs a request to get timetables from multiple starts/stops and mix them together (ordered by time of departure)
     *
     * @param provider        a provider to ask for the timetables
     * @param starts          some starting locations (or just one)
     * @param crossover       if the timetables should be looked up all starts to all stops (and not only one start to the corresponding stop
     * @param stops           some stopping locations (or just one)
     * @param datetime        the time to for when to look up
     * @param timeAsDeparture is this time assumed as the departure-time (and not the arrival)
     * @return a list of connections for the given request arguments (will not be null)
     */
    public static Set<Connection> getMultipleTimetables(NetworkProvider provider, Collection<Location> starts, boolean crossover, Collection<Location> stops, Date datetime, boolean timeAsDeparture) {
        //assures, that none-crossover-requests have the same amount of starts and stops
        assert crossover || starts.size() == stops.size() : "Anzahl Abfahrtshaltestellen und Zielhaltestellen bei der Nicht-Crossover Verbindung m�ssen gleich sein.";

        //creates the new connection set with an ordering by departure
        Set<Connection> connections = new TreeSet<Connection>(new FahrplanByDeparture());

        //update statistics
        PopularLocation.increasePopularities(KnownProvider.getId(provider), starts);
        PopularLocation.increasePopularities(KnownProvider.getId(provider), stops);

        //iterator for all stops (will be reset, if crossover
        Iterator<Location> stopsIterator = stops.iterator();
        //for each starting location, find the connections
        for (Location aStart : starts) {
            do { //for each or (depending on "crossover" or not) just one stop location, find the connections
                //get the first/next stop
                Location aStop = stopsIterator.next();
                try {
                    //try to get a set of connections for one combination of starts/stops
                    QueryConnectionsResult result = provider.queryConnections(aStart, null, aStop, datetime, timeAsDeparture, null, NetworkProvider.WalkSpeed.FAST);

                    //if there is at least one connection returned, add it to the list to return
                    if (result != null && result.connections != null && result.connections.size() > 0) {
                        connections.addAll(result.connections);
                    }
                } catch (IOException e) {
                    e.printStackTrace(); //is the providers server online?
                } catch (Exception e) {
                    e.printStackTrace(); //an error in the underlying application occurred todo: better Error output
                }
            }
            while (crossover && stopsIterator.hasNext()); //loop over all stops (if crossover connections are requested and there is a next stopping location)
            //if we want connections crossover, refill the iterator of the stopping locations
            if (crossover) {
                stopsIterator = stops.iterator();
            }
        }
        return connections;
    }
}
