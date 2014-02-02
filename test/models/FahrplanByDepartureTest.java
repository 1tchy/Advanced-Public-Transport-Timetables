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

import de.schildbach.pte.dto.Connection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: 23.11.11
 * Time: 09:44
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Connection.class)
public class FahrplanByDepartureTest {
    @Test
    public void test1() {
        Connection c1 = mock(Connection.class);
        when(c1.getFirstTripDepartureTime()).thenReturn(new Date(1322038500));
        Connection c2a = mock(Connection.class);
        when(c2a.getFirstTripDepartureTime()).thenReturn(new Date(1322039999));
        Connection c2b = mock(Connection.class);
        when(c2b.getFirstTripDepartureTime()).thenReturn(new Date(1322039999));
        FahrplanByDeparture comparator = new FahrplanByDeparture();
        assertThat(comparator.compare(c1, c2a)).describedAs("FahrplanByDeparture should be able to compare two Connections only by their departureTime. comparator.compare(c1, c2a) returned " + comparator.compare(c1, c2a)).isLessThan(0);
        assertThat(comparator.compare(c2a, c1)).describedAs("FahrplanByDeparture should be able to compare two Connections only by their departureTime. comparator.compare(c2a,c1) returned " + comparator.compare(c2a, c1)).isGreaterThan(0);
        assertThat(comparator.compare(c2a, c2b)).describedAs("FahrplanByDeparture should be able to compare two Connections only by their departureTime. comparator.compare(c2a,c2b) returned " + comparator.compare(c2a, c2b)).isEqualTo(0);
    }

    @Test
    public void testFirstIsNull() {
        //setup
        Connection c1 = mock(Connection.class);
        when(c1.getFirstTripDepartureTime()).thenReturn(null);
        Connection c2 = mock(Connection.class);
        when(c2.getFirstTripDepartureTime()).thenReturn(new Date(132200000));
        FahrplanByDeparture comparator = new FahrplanByDeparture();
        //test
        assertThat(comparator.compare(c1, c2)).describedAs("The first is not set").isLessThan(0);
    }
}
