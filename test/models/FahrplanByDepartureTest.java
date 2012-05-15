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

import de.schildbach.pte.dto.Connection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import play.test.UnitTest;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: 23.11.11
 * Time: 09:44
 */
@RunWith(PowerMockRunner.class)
public class FahrplanByDepartureTest extends UnitTest {
    @Test
    public void test1() {
        Connection c1 = mock(Connection.class);
        when(c1.getFirstDepartureTime()).thenReturn(new Date(1322038500));
        Connection c2a = mock(Connection.class);
        when(c2a.getFirstDepartureTime()).thenReturn(new Date(1322039999));
        Connection c2b = mock(Connection.class);
        when(c2b.getFirstDepartureTime()).thenReturn(new Date(1322039999));
        FahrplanByDeparture comparator = new FahrplanByDeparture();
        assert comparator.compare(c1, c2a) < 0 : "FahrplanByDeparture should be able to compare two Connections only by their departureTime. comparator.compare(c1, c2a) returned " + comparator.compare(c1, c2a);
        assert comparator.compare(c2a, c1) > 0 : "FahrplanByDeparture should be able to compare two Connections only by their departureTime. comparator.compare(c2a,c1) returned " + comparator.compare(c2a, c1);
        assert comparator.compare(c2a, c2b) == 0 : "FahrplanByDeparture should be able to compare two Connections only by their departureTime. comparator.compare(c2a,c2b) returned " + comparator.compare(c2a, c2b);
    }
}
