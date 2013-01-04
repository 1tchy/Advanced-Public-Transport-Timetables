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

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 26.09.12
 * Time: 13:14
 */
//Although the IDE doesn't recognise the usages, they are used by the automatic binding system
@SuppressWarnings({"SameParameterValue", "UnusedDeclaration", "WeakerAccess"})
public class Request {
    public final Stations from = new Stations();
    public boolean crossover = true;
    public final Stations to = new Stations();
    public final Directs direct = new Directs(false);
    public String time;
    public boolean timeAsDeparture = true;
    public boolean flexible = false;
    public String provider;
    public String requestString;

    public Set<String> validate() {
        Set<String> errors = new HashSet<>();
        NetworkProvider networkProvider = getProvider();
        if (networkProvider == null) {
            errors.add("Unbekannter Bahnanbieter (" + provider + ")");
            return errors;
        }
        Map<String, Stations> fromtos = new HashMap<>(2);
        fromtos.put("Abfahrtshaltestelle", from);
        fromtos.put("Zielhaltestelle", to);
        for (String fromto_name : fromtos.keySet()) {
            Stations fromto = fromtos.get(fromto_name);
            Set<String> fromto_errors = fromto.calculateLocations(networkProvider, fromto_name);
            errors.addAll(fromto_errors);
            ArrayList<Location> locations = fromto.getLocations();
            if (fromto_errors.isEmpty() && locations.isEmpty()) {
                errors.add("Sie müssen mindestens eine " + fromto_name + " angeben.");
            }
        }
        if (!crossover && (from.size() != to.size())) {
            errors.add("Wenn nicht alle Verbindungen von <b>allen</b> Abfahrtshaltestellen zu <b>allen</b> Zielhaltestellen gesucht werden, müssen gleich viele Abfahrts- wie Zielhaltestellen angegeben werden.");
        }
        return errors;
    }

    @Nullable
    public NetworkProvider getProvider() {
        return KnownProvider.get(provider);
    }

    public Date getTime() {
        TimeZone timeZone;
        NetworkProvider networkProvider = getProvider();
        if (networkProvider == null) {
            System.err.println("Took the default timezone for calculation.");
            timeZone = TimeZone.getDefault();
        } else {
            timeZone = KnownProvider.getTimeZone(networkProvider);
        }
        try {
            return InputChecker.getAndValidateTime(time, timeZone);
        } catch (WrongParameterException e) {
            System.err.println("Took current time for calculation.");
            return new Date();
        }
    }


    public void addFrom(String value) {
        from.add(value);
    }

    public void setFrom(String value) {
        addFrom(value);
    }

    public void setCrossover(String value) {
        crossover = makeBoolean(value, crossover);
    }

    private boolean makeBoolean(String value, boolean defaultValue) {
        if ("true".equals(value) || "t".equals(value) || "1".equals(value)) {
            return true;
        } else if ("false".equals(value) || "f".equals(value) || "0".equals(value)) {
            return false;
        }
        return defaultValue;
    }

    public void addTo(String value) {
        to.add(value);
    }

    public void setTo(String value) {
        addFrom(value);
    }

    public void addDirect(String value) {
        if (value.matches("^\\d+$")) {
            direct.add(Integer.parseInt(value), true);
        } else {
            direct.add(true);
        }
    }

    public void setDirect(String value) {
        addDirect(value);
    }

    public void setTime(String value) {
        time = value;
    }

    public void setTimeAsDeparture(String value) {
        timeAsDeparture = makeBoolean(value, timeAsDeparture);
    }

    public void setFlexible(String value) {
        flexible = makeBoolean(value, flexible);
    }

    public void setProvider(String value) {
        provider = value;
    }

    /**
     * Renders the time for display
     *
     * @param date Which time to print
     * @return The time to display
     */
    public String printDate(Date date) {
        TimeZone timeZone = KnownProvider.getTimeZone(getProvider());
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(date);
    }

    public static Request getEmpty() {
        Request ret = new Request();
        ret.from.add("");
        ret.from.add("");
        ret.to.add("");
        ret.to.add("");
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("crossover=").append(crossover);
        for (String f : from) {
            sb.append("&from[]=").append(f);
        }
        for (String t : to) {
            sb.append("&to[]=").append(t);
        }
        for (Boolean d : direct) {
            sb.append("&direct[]=").append(d);
        }
        sb.append("&time=").append(time);
        sb.append("&timeAsDeparture=").append(timeAsDeparture);
        sb.append("&flexible=").append(flexible);
        return sb.toString();
    }
}
