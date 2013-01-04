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

import de.schildbach.pte.dto.Location;
import play.Logger;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: 08.11.11
 * Time: 12:08
 */
public class PopularLocation implements Comparable<PopularLocation> {
    //    private String provider;
    //The popularity this location has (the higher the better)
    private int popularity = 0;
    //The location this represents
    private final Location location;
    //all popular Locations by Provider and by ID (First key: ID of the Provider; second Key: ID of the location
    private static final HashMap<String, Map<Integer, PopularLocation>> byID = new HashMap<>();

    /**
     * @param provider The ID of the provider this location belongs to
     * @param location A location to represent with a popularity
     */
    private PopularLocation(String provider, Location location) {
        this.location = location;
//        this.provider = provider;
        if (location.hasId()) {
            Map<Integer, PopularLocation> providers_list = getProvidersList(provider);
            providers_list.put(location.id, this);
        }
    }

    /**
     * Increase the popularity for this location by one
     */
    private void increasePopularity() {
        if (popularity < Integer.MAX_VALUE) {
            popularity++;
        }
    }

    /**
     * Returns a popular location object (a new one or one already counting for)
     *
     * @param provider a provider for whom it is
     * @param location a location to find the PopularLocation-Object for
     * @return a new or an already generated PopularLocation-Object for the given location
     */
    private static PopularLocation get(@NotNull String provider, @NotNull Location location) {
        Map<Integer, PopularLocation> providers_list = getProvidersList(provider);
        PopularLocation pl = providers_list.get(location.id);
        if (pl == null) {
            pl = new PopularLocation(provider, location);
        }
        if (!pl.location.name.equals(location.name)) {
            Logger.warn("Multiple locations with id " + location.id + " (" + pl.location.name + " and " + location.name + ")");
        }
        return pl;
    }

    /**
     * Returns the list of the already generated PopularLocations for a given provider
     *
     * @param provider the provider for whom the list is requested
     * @return A map (Location-Key -> PopularLocation) for the given provider
     */
    private static Map<Integer, PopularLocation> getProvidersList(String provider) {
        Map<Integer, PopularLocation> providers_list = byID.get(provider);
        if (providers_list == null) {
            providers_list = new HashMap<>();
            byID.put(provider, providers_list);
        }
        return providers_list;
    }

    /**
     * Returns a list of Locations (their name for the user) for a given search term
     *
     * @param provider For which the request is done
     * @param term     The term that is looked up
     * @param maximum  A maximum amount of return values
     * @return popular locations (max the amount given) for the given term
     */
    public static List<String> getMostPopularLike(String provider, String term, int maximum) {
        if (term == null) {
            return new ArrayList<>(0);
        }
        TreeSet<PopularLocation> top = new TreeSet<>();
        for (PopularLocation pl : getProvidersList(provider).values()) {
            if (pl.location.name.contains(term)) {
                top.add(pl);
            }
        }
        int values = Math.min(maximum, top.size());
        List<String> ret = new ArrayList<>(values);
        Iterator<PopularLocation> popularLocationIterator = top.iterator();
        while (values-- > 0) {
            ret.add(popularLocationIterator.next().location.name);
        }
        return ret;
    }

    /**
     * Compares the popularity of two locations
     *
     * @param popularLocation the other PopularLocation Object
     * @return a positive number if this is more popular
     */
    public int compareTo(PopularLocation popularLocation) {
        int popularityDifference = popularLocation.popularity - popularity;
        if (popularityDifference != 0) {
            return popularityDifference;
        } else {
            return hashCode() - popularLocation.hashCode();
        }
    }

    /**
     * Increases the popularity for some locations by one
     *
     * @param provider the provider to whom the locations belong
     * @param ls       the locations for which the popularity will be increased
     */
    public static void increasePopularities(String provider, Collection<Location> ls) {
        for (Location l : ls) {
            if (l != null) {
                get(provider, l).increasePopularity();
            }
        }
    }
}
