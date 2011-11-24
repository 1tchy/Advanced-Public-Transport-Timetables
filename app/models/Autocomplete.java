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

import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.dto.Location;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: 08.11.11
 * Time: 07:52
 */
public class Autocomplete {
    //How many items should be returned for an autocomplete request
    public final static int AUTOCOMPLETE_MAX = 5;
    //Cached data for an autocomplete request (Key of first map: Providers ID; Key of second map: Term to autocomplete
    public static Map<String, Map<String, List<String>>> autocompleteCache = new HashMap<String, Map<String, List<String>>>();
    //With which items in the popular location database the result for this cached autocomplete list was generated (Keys are the same as in autocompleteCache)
    public static Map<String, Map<String, String>> autocompleteCacheLevel = new HashMap<String, Map<String, String>>();

    /**
     * Autocompletes a station
     *
     * @param provider ID of the provider
     * @param term     to autocomplete
     * @return a list of stations
     */
    public static List<String> stations(final String provider, final String term) {
        //get the autocomplete list for the given provider from the cache
        Map<String, List<String>> providers_autocomplete = autocompleteCache.get(provider);
        Map<String, String> providers_autocomplete_level = autocompleteCacheLevel.get(provider);
        //create a list if none exists
        if (providers_autocomplete == null || providers_autocomplete_level == null) {
            if (KnownProvider.get(provider) == null) {
                System.out.println("Autocompleteanfrage konnte für Provider '" + provider + "' nicht durchgeführt werden. Grund: Unbekannter Provider.");
                return new ArrayList<String>(0);
            }
            providers_autocomplete = new HashMap<String, List<String>>();
            providers_autocomplete_level = new HashMap<String, String>();
            autocompleteCache.put(provider, providers_autocomplete);
            autocompleteCacheLevel.put(provider, providers_autocomplete_level);
        }
        List<String> mostPopular = PopularLocation.getMostPopularLike(provider, term, AUTOCOMPLETE_MAX);
        //get the station list for the term from the cache
        List<String> autocomplete_list = null;
        if (providers_autocomplete_level.get(term) != null && providers_autocomplete_level.get(term).equals(join(mostPopular))) {
            autocomplete_list = providers_autocomplete.get(term);
        }
        //create the station list, if none exists
        if (autocomplete_list == null) {
            //get the provider object
            final NetworkProvider p = KnownProvider.get(provider);
            autocomplete_list = new ArrayList<String>(AUTOCOMPLETE_MAX);
            autocomplete_list.addAll(mostPopular);
            if (autocomplete_list.size() < AUTOCOMPLETE_MAX) {
                try {
                    //get the list of the requested stations from the provider
                    List<Location> locations = p.autocompleteStations(term);
                    String simplifiedTerm = simplify(term);
                    boolean firstSuggestion = true;
                    for (Location location : locations) {
                        if (firstSuggestion || simplify(location.name).contains(simplifiedTerm)) {
                            autocomplete_list.add(location.name);
                            if (autocomplete_list.size() == AUTOCOMPLETE_MAX) {
                                break;
                            }
                        }
                        firstSuggestion = false;
                    }
                    providers_autocomplete.put(term, autocomplete_list);
                    providers_autocomplete_level.put(term, join(mostPopular));
                } catch (IOException e) {
                    System.out.println("Konnte vom Provider " + p.toString() + " keine Autoverfollständigung von Station (" + term + ") abfragen.");
                    e.printStackTrace();
                }
            } else {
                providers_autocomplete_level.put(term, join(mostPopular));
            }
        }
        return autocomplete_list;
    }

    /**
     * Simplifies a string heavily (removes everything that's not a normal letter and returns it in lowercase)
     *
     * @param string to simplify
     * @return simplified version of the string
     */
    private static String simplify(String string) {
        return Pattern.compile("[^a-z]").matcher(string.toLowerCase()).replaceAll("");
    }

    /**
     * Joins a string by ";"
     *
     * @param items some Strings to be put together
     * @return A string containing all inputstrings connected with a ";"
     */
    private static String join(Collection<String> items) {
        if (items.isEmpty()) {
            return "";
        }
        final String joinBy = ";";
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append(joinBy);
            sb.append(item);
        }
        return sb.substring(joinBy.length());
    }
}
