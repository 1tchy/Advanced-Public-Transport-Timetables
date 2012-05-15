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

import de.schildbach.pte.dto.Connection;

import java.util.Comparator;
import java.util.Date;

/**
 * Compares two connections based on their departing time
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: Oct 10, 2011
 * Time: 6:45:38 PM
 */
public class FahrplanByDeparture implements Comparator<Connection> {
    public int compare(Connection c1, Connection c2) {
        Date c2start = c2.getFirstTripDepartureTime();
        Date c1start = c1.getFirstTripDepartureTime();
        if (c1start == null || c2start == null || c1start.equals(c2start)) {
            return c1.hashCode() - c2.hashCode();
        }
        return c1start.compareTo(c2start);
    }
}
