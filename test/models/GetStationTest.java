/*
 * Copyright 2012, L. Murer.
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

import de.schildbach.pte.OpenDataProvider;
import de.schildbach.pte.dto.NearbyStationsResult;
import org.junit.Before;
import org.junit.Test;
import play.test.UnitTest;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 18.05.12
 * Time: 20:57
 */
public class GetStationTest extends UnitTest {
    OpenDataProvider provider;

    @Before
    public void setUp() {
        provider = (OpenDataProvider) KnownProvider.get("sbb");
    }

    @Test
    public void getOneStation() throws IOException {
        NearbyStationsResult result = provider.queryNearbyStations(47.235364f, 8.449759f, 300, 1);
        assert result.stations.size() == 1;
    }

    @Test
    public void getMühlau() throws IOException {
        NearbyStationsResult result = provider.queryNearbyStations(47.230089f, 8.389174f, 300, 0);
        assert result.stations.get(0).name.equals("Mühlau");
    }

    @Test
    public void getMany() throws IOException {
        NearbyStationsResult result = provider.queryNearbyStations(47.372896f, 8.536863f, 300, 10);
        int size = result.stations.size();
        assert size > 3 : "Returned really few stations. Only " + size;
        assert size <= 10 : "Returned more than the maximum limit: " + size;
    }

}
