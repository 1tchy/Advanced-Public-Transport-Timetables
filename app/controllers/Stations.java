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

package controllers;

import actions.MailAction;
import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.OpenDataProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import models.Autocomplete;
import models.InputChecker;
import models.WrongParameterException;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 05.01.13
 * Time: 12:12
 */
public class Stations extends Controller {

    /**
     * Returns some stations near the given coordinates
     */
    @With(MailAction.class)
    public static Result getByPosition(String lat, String lon) throws WrongParameterException {
        //todo Play 2.1: Make parameters real floats
        float lat_f = Float.parseFloat(lat);
        float lon_f = Float.parseFloat(lon);
        NearbyStationsResult stations;
        NetworkProvider provider = InputChecker.getAndValidateProvider("sbb");
        try {
            if (provider instanceof OpenDataProvider) {
                stations = ((OpenDataProvider) provider).queryNearbyStations(lat_f, lon_f, 0, 0);
            } else {
                stations = provider.queryNearbyStations(new Location(LocationType.ANY, (int) lat_f, (int) lon_f), 0, 0);
            }
        } catch (IOException e) {
            Logger.warn("Could not find nearby stations at " + lat + "/" + lon, e);
            return ok(Json.toJson(new ArrayList(0)));
        }
        return ok(Json.toJson(stations.stations));
    }

    /**
     * Answers to a autocomplete request for a station
     */
    @With(MailAction.class)
    public static Result autocomplete() {
        String[] values = request().queryString().get("term");
        Result re;
        if (values == null) {
            re = noContent();
        } else {
            re = ok(Json.toJson(Autocomplete.stations("sbb", values[0])));
        }
        return re;
    }

}
