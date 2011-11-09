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
import de.schildbach.pte.SbbProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: Oct 11, 2011
 * Time: 9:35:52 AM
 */
public class KnownProvider {
    /**
     * the real provider object
     */
    public final NetworkProvider provider;
    /**
     * the name of the provider to print out in HTML
     */
    public final String humanName;
    /**
     * the id of the provider to use in urls and similar places (lowercase chars only)
     */
    public final String id;
    /**
     * the timezone to get the correct times for this provider
     */
    public final TimeZone timeZone;
    /**
     * if the list of known providers already got loaded
     */
    private static boolean initialized = false;
    /**
     * the list of all known providers by its id
     */
    private static Map<String, KnownProvider> all = new HashMap<String, KnownProvider>();

    private KnownProvider(NetworkProvider provider, String humanName, String id, TimeZone timeZone) {
        this.provider = provider;
        this.humanName = humanName;
        this.id = id.toLowerCase();
        this.timeZone = timeZone;
        all.put(this.id, this);
    }

    /**
     * Load the list of all known providers
     */
    private static void initialize() {
        if (initialized) return;
        initialized = true;
        new KnownProvider(new SbbProvider("MJXZ841ZfsmqqmSymWhBPy5dMNoqoGsHInHbWJQ5PTUZOJ1rLTkn8vVZOZDFfSe"), "SBB", "sbb", TimeZone.getTimeZone("Europe/Zurich"));
    }

    /**
     * Gets a list of all known providers
     *
     * @return list of all known providers
     */
    public static Collection<KnownProvider> all() {
        initialize();
        return all.values();
    }

    /**
     * Gets one of the known providers by its ID
     *
     * @param id The ID of a provider (must be lowercase)
     * @return one of the known providers or null
     */
    public static NetworkProvider get(String id) {
        assert id.toLowerCase().equals(id) : "The ID of a provider must be lowercase!";
        initialize();
        KnownProvider knownProvider = all.get(id);
        if (knownProvider == null) return null;
        return knownProvider.provider;
    }

    /**
     * Gets a known providers by its ID
     *
     * @param id The ID of a provider (must be lowercase)
     * @return a known providers or null
     */
    public static KnownProvider getById(String id) {
        assert id.toLowerCase().equals(id) : "The ID of a provider must be lowercase!";
        initialize();
        return all.get(id);
    }

    /**
     * Gets the name of a known provider by its object
     *
     * @param provider The object for the provider
     * @return the human readable name of this provider
     */
    public static String get(NetworkProvider provider) {
        initialize();
        for (KnownProvider kp : all.values()) {
            if (kp.provider.equals(provider)) {
                return kp.humanName;
            }
        }
        return "unknown";
    }

    /**
     * Gets the id of a known provider by its object
     *
     * @param provider The object for the provider
     * @return the human readable name of this provider
     */
    public static String getId(NetworkProvider provider) {
        initialize();
        for (KnownProvider kp : all.values()) {
            if (kp.provider.equals(provider)) {
                return kp.id;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + provider.toString());
    }

    /**
     * Gets the timezone of a known provider by its object
     *
     * @param provider The object for the provider
     * @return the timezone of this provider
     */
    public static TimeZone getTimeZone(NetworkProvider provider) {
        initialize();
        for (KnownProvider kp : all.values()) {
            if (kp.provider.equals(provider)) {
                return kp.timeZone;
            }
        }
        return null;
    }
}
