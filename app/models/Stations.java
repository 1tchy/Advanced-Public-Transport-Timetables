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
import de.schildbach.pte.dto.Location;
import play.api.templates.Html;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

public class Stations extends ArrayList<String> {
    @Nullable
    private ArrayList<Location> locations;

    @NotNull
    public Set<String> calculateLocations(NetworkProvider networkProvider, String description) {
        Set<String> parseErrors = new HashSet<>();
        locations = new ArrayList<>(this.size());
        for (String station : this) {
            Location location = calculateLocation(networkProvider, station, description, parseErrors);
            if (location != null) {
                locations.add(location);
            }
        }
        return parseErrors;
    }

    @NotNull
    public ArrayList<Location> getLocations() {
        assert locations != null : "You need to call calculateLocations() first. It also gives you a list of parse Errors.";
        return locations;
    }

    /**
     * Strings to Locations
     *
     * @param provider    the provider of the timetable/the station names
     * @param station     The name of the station that is looked for
     * @param description The human readable name of the field that's evaluated (used for error output)
     * @param errors      A list to add error messages to
     * @return a list of stations (might have 0 elements)
     */
    @Nullable
    private static Location calculateLocation(@NotNull NetworkProvider provider, @NotNull String station, String description, @NotNull Set<String> errors) {
        //will be true, if an error occurred
        if (station.equals("")) {
            return null;
        } else {
            List<Location> locationList;
            try {
                //get the list of Locations from the DataProvider
                locationList = provider.autocompleteStations(station);
            } catch (IOException e) {
                if (e instanceof UnknownHostException) {
                    errors.add("Verbindung zur Fahrplandatenbank (" + KnownProvider.getName(provider) + ") konnte nicht hergestellt werden. Bitte später nochmals versuchen. (" + e.getLocalizedMessage() + ")");
                    return null;
                } else {
                    errors.add("Fehler beim Verbinden zur Datenquelle. (" + e.getLocalizedMessage() + ")");
                    return null;
                }
            }
            if (locationList == null) {
                errors.add("Fehler beim Finden der " + description + " '" + station + "'.");
                return null;
            }
            if (locationList.size() == 1) { //There is only one station possible -> choose it
                return locationList.get(0);
            } else if (locationList.size() == 0) { //There is no station possible -> print error
                errors.add(description + " '" + station + "' nicht gefunden.");
                return null;
            } else { //There are multiple stations possible -> check all and add the correct matching to the list (or print error)
                for (Location location : locationList) {
                    if (Autocomplete.simplify(station).equals(Autocomplete.simplify(location.uniqueShortName()))) {
                        return location;
                    }
                }
                errors.add(description + " '" + station + "' ist nicht eindeutig.");
                return null;
            }
        }
    }


    // If the Information changes, reset the list of the locations

    @Override
    public String set(int i, String s) {
        locations = null;
        return super.set(i, s);
    }

    @Override
    public boolean add(String s) {
        locations = null;
        return super.add(s);
    }

    @Override
    public void add(int i, String s) {
        locations = null;
        super.add(i, s);
    }

    @Override
    public String remove(int i) {
        locations = null;
        return super.remove(i);
    }

    @Override
    public boolean remove(Object o) {
        locations = null;
        return super.remove(o);
    }

    @Override
    public void clear() {
        locations = null;
        super.clear();
    }

    @Override
    public boolean addAll(Collection<? extends String> strings) {
        locations = null;
        return super.addAll(strings);
    }

    @Override
    public boolean addAll(int i, Collection<? extends String> strings) {
        locations = null;
        return super.addAll(i, strings);
    }

    @Override
    protected void removeRange(int i, int i1) {
        locations = null;
        super.removeRange(i, i1);
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        locations = null;
        return super.removeAll(objects);
    }

    public Html join(String joiner) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Location station : this.getLocations()) {
            if (first) {
                first = false;
            } else {
                sb.append(joiner);
            }
            sb.append(station.name.replace(",", ""));
        }
        return new Html(sb.toString());
    }

}
