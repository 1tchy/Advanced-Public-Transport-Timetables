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

package de.schildbach.pte;

import de.schildbach.pte.dto.*;
import de.schildbach.pte.dto.Connection.Part;
import de.schildbach.pte.dto.Connection.Trip;
import play.Logger;
import play.api.libs.json.*;
import scala.collection.Iterator;
import scala.collection.Seq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 25.04.12
 * Time: 18:52
 */
public class OpenDataProvider extends AbstractNetworkProvider {

    private final static String SERVER_PRODUCT = "opendata";
    private final static int MAX_CACHED_CONNECTION_DETAILS = 100;
    private final static char CONNECTION_NUMBER_SEPARATOR = '#';
    private final Cache<String, GetConnectionDetailsResult> cachedConnectionDetails = new Cache<>(MAX_CACHED_CONNECTION_DETAILS);
    private final static int MAX_CACHED_REQUESTS = 500;
    private final static Cache<String, JsValue> cachedRequests = new Cache<>(MAX_CACHED_REQUESTS);
    private final String url;
    private final NetworkId networkId;
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private final TimeZone timeZone;

    public OpenDataProvider(String url, NetworkId networkId, TimeZone timeZone) {
        this.url = url;
        this.networkId = networkId;
        this.timeZone = timeZone;
    }

    public NetworkId id() {
        return networkId;
    }

    public boolean hasCapabilities(Capability... capabilities) {
        for (Capability capability : capabilities) {
            switch (capability) {
                case AUTOCOMPLETE_ONE_LINE:
                    //yes
                    break;
                case CONNECTIONS:
                    //yes
                    break;
                case DEPARTURES:
                    //yes
                    break;
                case NEARBY_STATIONS:
                    //no
                    return false;
            }
        }
        return true;
    }

    /**
     * Determine stations near to given location. At least one of stationId or lat/lon pair must be present.
     *
     * @param location    location to determine nearby stations (optional)
     * @param maxDistance maximum distance in meters, or {@code 0}
     * @param maxStations maximum number of stations, or {@code 0}
     * @return nearby stations
     * @throws IOException
     */
    public NearbyStationsResult queryNearbyStations(Location location, int maxDistance, int maxStations) throws IOException {
        return queryNearbyStations(location.lat, location.lon, maxDistance, maxStations);
    }

    /**
     * Determine stations near to given location.
     *
     * @param lat         latitude from where to determine nearby stations
     * @param lon         longitude from where to determine nearby stations
     * @param maxDistance maximum distance in meters, or {@code 0}
     * @param maxStations maximum number of stations, or {@code 0}
     * @return nearby stations
     * @throws IOException
     */
    public NearbyStationsResult queryNearbyStations(double lat, double lon, int maxDistance, int maxStations) throws IOException {
        JsValue request = performRequest("locations?x=" + lat + "&y=" + lon);
        request = ((JsObject) request).value().get("stations").get();
        List<Location> stations = new LinkedList<>();
        if (request instanceof JsArray) {
            Iterator<JsValue> stationsIterator = ((JsArray) request).value().iterator();
            while (stationsIterator.hasNext()) {
                JsValue a_station = stationsIterator.next();
                if (a_station instanceof JsObject) {
                    Location foundLocation = parseLocation((JsObject) a_station);
                    double lonDiff = lon - foundLocation.lon;
                    double latDiff = lat - foundLocation.lat;
                    if (maxDistance == 0 || latDiff * latDiff + lonDiff * lonDiff <= maxDistance * maxDistance) {
                        stations.add(foundLocation);
                        if (maxStations == 0 || stations.size() >= maxStations) {
                            break;
                        }
                    }
                }
            }
        }
        return new NearbyStationsResult(new ResultHeader(SERVER_PRODUCT), stations);
    }

    /**
     * Get departures at a given station, probably live
     *
     * @param stationId     id of the station
     * @param maxDepartures maximum number of departures to get or {@code 0}
     * @param equivs        also query equivalent stations?
     * @return result object containing the departures
     * @throws IOException
     */
    public QueryDeparturesResult queryDepartures(int stationId, int maxDepartures, boolean equivs) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Meant for auto-completion of station names
     *
     * @param constraint input by user so far
     * @return auto-complete suggestions
     * @throws IOException
     */
    public List<Location> autocompleteStations(CharSequence constraint) throws IOException {
        JsValue response = performRequest("locations?type=any&query=" + constraint);
        if (!(response instanceof JsObject)) {
            throw new IOException("Bahnanbieter konnte nicht angefragt werden.");
        }
        JsObject response_o = (JsObject) response;
        JsValue responseStationsElement = response_o.value().get("stations").get();
        if (!(responseStationsElement instanceof JsArray)) {
            return new ArrayList<>(0);
        }
        Iterator<JsValue> responseArray = ((JsArray) responseStationsElement).value().iterator();
        List<Location> ret = new ArrayList<>(((JsArray) responseStationsElement).value().length());
        while (responseArray.hasNext()) {
            JsValue onePossibleResponse = responseArray.next();
            if (!(onePossibleResponse instanceof JsObject)) {
                continue;
            }
            JsObject onePossibleResponse_o = (JsObject) onePossibleResponse;

            final int MINIMUM_SCORE = 90;
            JsValue score = retrieveSubElement(onePossibleResponse_o, "score");
            if (score instanceof JsString) {
                String score_string = ((JsString) score).value();
                if (score_string.matches("\\d+")) {
                    if (Integer.parseInt(score_string) < MINIMUM_SCORE) {
                        continue;
                    }
                }
            }

            JsValue name_js = retrieveSubElement(onePossibleResponse_o, "name");
            if (!(name_js instanceof JsString)) {
                continue;
            }
            String name = ((JsString) name_js).value();

            JsValue id_js = retrieveSubElement(onePossibleResponse_o, "id");
            int id = 0;
            if (id_js instanceof JsString) {
                String id_string = ((JsString) id_js).value();
                if (id_string.matches("\\d+")) {
                    id = Integer.parseInt(id_string);
                } else {
                    throw new IllegalStateException("ID (" + id_string + ") of '" + name + "' is not a number.");
                }
            }
            ret.add(new Location(LocationType.STATION, id, name, name));
        }
        return ret;
    }

    private JsValue retrieveSubElement(JsValue element, String key) {
        if (element instanceof JsObject) {
            scala.collection.Map<String, JsValue> value = ((JsObject) element).value();
            if (value.contains(key)) {
                return value.get(key).get();
            } else {
                return null;
            }
        } else if (element instanceof JsArray) {
            throw new UnsupportedOperationException();
        } else {
            return null;
        }
    }

    /**
     * Query connections, asking for any ambiguousnesses
     *
     * @param from          location to route from, mandatory
     * @param via           location to route via, may be {@code null}
     * @param to            location to route to, mandatory
     * @param date          desired date for departing, mandatory
     * @param dep           date is departure date? {@code true} for departure, {@code false} for arrival
     * @param products      products to take into account
     * @param walkSpeed     how fast can you walk?
     * @param accessibility how accessible do you need the route to be?
     * @return result object that can contain alternatives to clear up ambiguousnesses, or contains possible connections
     * @throws IOException
     */
    public QueryConnectionsResult queryConnections(Location from, Location via, Location to, Date date, boolean dep, String products, WalkSpeed walkSpeed, Accessibility accessibility) throws IOException {
        return queryConnections(from, via, to, date, dep, products, false, true, new ArrayList<String>());
    }

    /**
     * Query connections, asking for any ambiguousnesses
     *
     * @param from     location to route from, mandatory
     * @param via      location to route via, may be {@code null}
     * @param to       location to route to, mandatory
     * @param date     desired date for departing, mandatory
     * @param dep      date is departure date? {@code true} for departure, {@code false} for arrival
     * @param products products to take into account
     * @param direct   If the connection has to be direct
     * @param flexible if the match of the stations can be flexible or has to be exact (Luzern matches also Luzern, Bahnhof)
     * @param warnings A list of none critical error message (the critical are thrown in an exception)
     * @return result object that can contain alternatives to clear up ambiguousnesses, or contains possible connections
     * @throws IOException
     */
    public QueryConnectionsResult queryConnections(Location from, Location via, Location to, Date date, boolean dep, String products, boolean direct, boolean flexible, List<String> warnings) throws IOException {
        assert products == null : "Parameter 'Product' is not implemented yet.";
        StringBuilder request = new StringBuilder("connections?from=");
        try {
            request.append(from.id > 0 ? from.id : from.name).append("&to=").append(to.id > 0 ? to.id : to.name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (via != null) {
            request.append("&via[]=").append(via.id > 0 ? via.id : via.name);
        }
        if (date != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            timeFormat.setTimeZone(timeZone);
            request.append("&date=").append(new SimpleDateFormat("yyyy-MM-dd").format(date)).append("&time=").append(timeFormat.format(date));
        }
        if (!dep) {
            request.append("&isArrivalTime=1");
        }
        if (direct) {
            request.append("&direct=1");
        }
        QueryConnectionsResult connections = queryConnections(request.toString(), from.name, to.name, !flexible, warnings);
        if (direct && connections.connections.size() == 0) {
            warnings.add("Keine direkte Verbindung von " + from + " nach " + to + " gefunden.");
        }
        return connections;
    }

    private QueryConnectionsResult queryConnections(String request, String from, String to, boolean strict, Collection<String> warnings) throws IOException {
        JsObject response = (JsObject) performRequest(request);
        List<Connection> connections = new ArrayList<>();
        Location fromx = null, tox = null;
        int connectionNumber = 0;

        JsValue connections_json = response.value().get("connections").get();
        if (connections_json instanceof JsArray) {
            Iterator<JsValue> connectionsArray = ((JsArray) connections_json).value().iterator();
            while (connectionsArray.hasNext()) {
                JsValue connection_value = connectionsArray.next();
                if (!(connection_value instanceof JsObject)) {
                    continue;
                }
                JsObject connection = (JsObject) connection_value;

                Set<String> ignored_from = new HashSet<>();
                Set<String> ignored_to = new HashSet<>();

                JsObject from_response = (JsObject) ((JsObject) connection.value().get("from").get()).value().get("station").get();
                int from_id = Integer.parseInt(((JsString) from_response.value().get("id").get()).value());
                String from_name = ((JsString) from_response.value().get("name").get()).value();

                if (strict && !from_name.equals(from) && !ignored_from.contains(from_name)) {
                    ignored_from.add(from_name);
                    warnings.add("Bei der Abfrage von " + from + " nach " + to + " wurden Verbindungen ab '" + from_name + "' ignoriert.");
                    continue;
                }
                fromx = new Location(LocationType.STATION, from_id, from_name, from_name);

                JsObject to_response = (JsObject) ((JsObject) connection.value().get("to").get()).value().get("station").get();
                int to_id = Integer.parseInt(((JsString) to_response.value().get("id").get()).value());
                String to_name = ((JsString) to_response.value().get("name").get()).value();
                if (strict && !to_name.equals(to) && !ignored_to.contains(to_name)) {
                    ignored_to.add(to_name);
                    warnings.add("Bei der Abfrage von " + from + " nach " + to + " wurden Verbindungen nach '" + to_name + "' ignoriert.");
                    continue;
                }
                Seq<JsValue> section_response = ((JsArray) connection.value().get("sections").get()).value();
                tox = new Location(LocationType.STATION, to_id, to_name, to_name);
                List<Part> parts = new ArrayList<>(section_response.length());
                Iterator<JsValue> parts_iterator = section_response.iterator();
                while (parts_iterator.hasNext()) {
                    JsValue section_part_element = parts_iterator.next();
                    Part trip = parsePart(section_part_element);
                    if (trip == null) {
                        continue;
                    }
                    parts.add(trip);
                }
                String connection_id = request + CONNECTION_NUMBER_SEPARATOR + connectionNumber++;
                Connection c = new Connection(connection_id, connection_id, fromx, tox, parts, null, null);
                cachedConnectionDetails.put(connection_id, new GetConnectionDetailsResult(new Date(), c));
                connections.add(c);
                addErrorsForIgnoredConnections(from, to, ignored_from, ignored_to, warnings);
            }
        }
        return new QueryConnectionsResult(new ResultHeader(SERVER_PRODUCT), request, fromx, null, tox, "", connections);
    }

    /**
     * Adds one single error message containing information about all (from and to) ignored stations
     *
     * @param from         The name of the given from-station
     * @param to           The name of the to-station
     * @param ignored_from A list of found but ignored from-stations
     * @param ignored_to   A list of found but ignored to-stations
     * @param warnings     A list of none critical error message (the critical are thrown in an exception)
     */
    private static void addErrorsForIgnoredConnections(String from, String to, Set<String> ignored_from, Set<String> ignored_to, Collection<String> warnings) {
        final String firstPart = "Bei der Abfrage von &quot;" + from + "&quot; nach &quot;" + to + "&quot; wurden Verbindungen ";
        if (ignored_from.size() > 0) {
            if (ignored_to.size() > 0) {
                warnings.add(firstPart + "von &quot;" + join(ignored_from, "&quot;, &quot;", "&quot; und &quot;") + "&quot; sowie nach &quot;" + join(ignored_to, "&quot;, &quot;", "&quot; und &quot;") + "&quot; ignoriert.");
            } else {
                warnings.add(firstPart + "von &quot;" + join(ignored_from, "&quot;, &quot;", "&quot; und &quot;") + "&quot; ignoriert.");
            }
        } else {
            if (ignored_to.size() > 0) {
                warnings.add(firstPart + "nach &quot;" + join(ignored_to, "&quot;, &quot;", "&quot; und &quot;") + "&quot; ignoriert.");
            }
        }
    }

    /**
     * Joins a collection of strings
     *
     * @param items      some Strings to be put together
     * @param joinBy     The String to put between the entries
     * @param lastJoinBy The alternative String to put between the last two entries
     * @return A string containing all inputstrings connected with a ";"
     */
    private static String join(Collection<String> items, String joinBy, String lastJoinBy) {
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        int itemsLeft = items.size();
        for (String item : items) {
            if (isFirst) {
                isFirst = false;
            } else if (itemsLeft > 1) {
                sb.append(joinBy);
            } else {
                sb.append(lastJoinBy);
            }
            sb.append(item);
            itemsLeft--;
        }
        return sb.toString();
    }

    private Part parsePart(JsValue section_part_element) {
        if (!(section_part_element instanceof JsObject)) {
            return null;
        }
        scala.collection.Map<String, JsValue> section_part = ((JsObject) section_part_element).value();
        JsObject section_part_departure = (JsObject) section_part.get("departure").get();
        Location departure = parseLocation((JsObject) section_part_departure.value().get("station").get());
        JsObject section_part_arrival = (JsObject) section_part.get("arrival").get();
        Location arrival = parseLocation((JsObject) section_part_arrival.value().get("station").get());
        int section_part_departure_id = Integer.parseInt(((JsString) ((JsObject) section_part_departure.value().get("station").get()).value().get("id").get()).value());
        String section_part_departure_name = ((JsString) ((JsObject) section_part_departure.value().get("station").get()).value().get("name").get()).value();
        int section_part_arrival_id = Integer.parseInt(((JsString) ((JsObject) section_part_arrival.value().get("station").get()).value().get("id").get()).value());
        String section_part_arrival_name = ((JsString) ((JsObject) section_part_arrival.value().get("station").get()).value().get("name").get()).value();
        Date departureTime;
        Date arrivalTime;
        try {
            departureTime = dateFormat.parse(((JsString) section_part_departure.value().get("departure").get()).value());
            arrivalTime = dateFormat.parse(((JsString) section_part_arrival.value().get("arrival").get()).value());
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not parse date: " + e);
        }
        boolean isWalk = section_part.contains("walk") && (section_part.get("walk").get() instanceof JsObject);
        if (isWalk) {
            return new Connection.Footway(parseWalktime((JsObject) section_part.get("walk").get()), departure, arrival, new ArrayList<Point>(0));
        } else {
            Style style = new Style(0, 0xFFFFFF);
            if (section_part.contains("journey") && section_part.get("journey").get() instanceof JsObject) {
                JsObject section_part_journey = (JsObject) section_part.get("journey").get();
                Location destination = null;
                Line line = new Line(((JsString) section_part_journey.value().get("name").get()).value(), parseCategory(section_part_journey), style);
                if (section_part_journey.value().get("to").get() instanceof JsObject) {
                    String destination_name = ((JsString) section_part_journey.value().get("to").get()).value();
                    destination = new Location(LocationType.STATION, -1, destination_name, destination_name);
                }
                Seq<JsValue> passList = ((JsArray) section_part_journey.value().get("passList").get()).value();
                List<Stop> stops = new ArrayList<>(passList.length());
                Iterator<JsValue> passListIterator = passList.iterator();
                while (passListIterator.hasNext()) {
                    JsValue passedStation = passListIterator.next();
                    if (!(passedStation instanceof JsObject)) {
                        continue;
                    }
                    JsObject passedStation_o = (JsObject) passedStation;
                    String position = ((JsString) passedStation_o.value().get("platform").get()).value();
                    Date timeOfPass;
                    try {
                        JsValue passTime = passedStation_o.value().get("departure").get();
                        if (passTime instanceof JsString) {
                            timeOfPass = dateFormat.parse(((JsString) passTime).value());
                        } else {
                            timeOfPass = null;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Could not parse date '" + ((JsString) passedStation_o.value().get("departure").get()).value() + "'");
                    }
                    stops.add(new Stop(parseLocation((JsObject) passedStation_o.value().get("station").get()), position, timeOfPass));
                }
                return new Trip(line, destination, departureTime, departureTime, ((JsString) section_part_departure.value().get("platform").get()).value(), new Location(LocationType.STATION, section_part_departure_id, section_part_departure_name, section_part_departure_name), arrivalTime, arrivalTime, ((JsString) section_part_arrival.value().get("platform").get()).value(), new Location(LocationType.STATION, section_part_arrival_id, section_part_arrival_name, section_part_arrival_name), stops, new ArrayList<Point>(passList.size()));
            } else {
                return new Trip(new Line("", "?", style), null, departureTime, departureTime, ((JsString) section_part_departure.value().get("platform").get()).value(), new Location(LocationType.STATION, section_part_departure_id, section_part_departure_name, section_part_departure_name), arrivalTime, arrivalTime, ((JsString) section_part_arrival.value().get("platform").get()).value(), new Location(LocationType.STATION, section_part_arrival_id, section_part_arrival_name, section_part_arrival_name), new ArrayList<Stop>(0), new ArrayList<Point>(0));
            }
        }
    }

    private int parseWalktime(JsObject walk) {
        final DateFormat walktimeformat = new SimpleDateFormat("HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(walktimeformat.parse(((JsString) walk.value().get("duration").get()).value()));
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return calendar.get(Calendar.MINUTE) + 60 * calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Transforms a JsObject containing a Location to a Location-Object
     *
     * @param location Object to convert
     * @return Converted object
     */
    private Location parseLocation(JsObject location) {
        String station_name = ((JsString) location.value().get("name").get()).value();
        return new Location(LocationType.STATION, Integer.parseInt(((JsString) location.value().get("id").get()).value()), station_name, station_name);
    }

    private String parseCategory(JsObject category) {
        if (category == null) {
            return "";
        }
        String cat = ((JsString) category.value().get("category").get()).value().toUpperCase();
        String prefix;
        final String[] schnellzugStrings = {"EC", "IC", "ICN", "ICE", "CNL", "EN", "THA", "TGV"};
        final String[] busString = {"NBU", "TRO", "BUS", "NTO", "NFB", "MID", "MIN", "KB", "NB"};
        if (cat.matches("SN?\\d*")) { //S-Bahn
            prefix = "S";
        } else if (cat.startsWith("IR") | cat.startsWith("R") | cat.equals("EXT")) { //Regionalzug
            prefix = "R";
        } else if (cat.equals("NTR") | cat.equals("TRA")) { //Tram
            prefix = "T";
        } else if (Arrays.asList(schnellzugStrings).contains(cat)) { //Schnellzug
            prefix = "I";
        } else if (Arrays.asList(busString).contains(cat)) { //Bus
            prefix = "B";
        } else if (cat.equals("BAT") || cat.equals("BAV")) { //Schiff
            prefix = "F";
        } else if (cat.equals("LB") | cat.equals("GB")) { //Seilbahn
            prefix = "L";
        } else {
            prefix = "?";
            System.err.println("Category '" + cat + "' can't be parsed (" + category.toString() + ").");
        }
        String name = ((JsString) category.value().get("name").get()).value();
        JsValue to = category.value().get("to").get();
        if (to instanceof JsString) {
            return prefix + name + " (nach " + ((JsString) to).value() + ")";
        }
        return prefix + name;
    }

    /**
     * Query more connections (e.g. earlier or later)
     *
     * @param context context to query more connections from
     * @param next    {@code true} for get next connections, {@code false} for get previous connections
     * @return result object that contains possible connections
     * @throws IOException
     */
    public QueryConnectionsResult queryMoreConnections(String context, boolean next) throws IOException {
        assert false : "Not implemented yet.";
        return null;
    }

    /**
     * Get details about a connection
     *
     * @param connectionUri uri returned via {@link NetworkProvider#queryConnections}
     * @return result object containing the details of the connection
     * @throws IOException
     */
    public GetConnectionDetailsResult getConnectionDetails(String connectionUri) throws IOException {
        GetConnectionDetailsResult connectionDetailsResult = cachedConnectionDetails.get(connectionUri);
        assert connectionDetailsResult != null : "Connection details for connection " + connectionUri + " not stored.";
        return connectionDetailsResult;
    }

    private JsValue performRequest(String request) throws IOException {
        JsValue response = cachedRequests.get(request);
        if (response == null) {
            response = performRequest(request, 5);
            if (response != null) {
                cachedRequests.put(request, response);
            }
        }
        return response;
    }

    private JsValue performRequest(String request, int retries) throws IOException {
        request = request.replace(" ", "+");
        try {
            String requestURL = url + request;
            Logger.debug("Performing request to: " + requestURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(requestURL).openStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return Json.parse(response.toString());
        } catch (IOException e) {
            if (retries > 0) {
                return performRequest(request, retries - 1);
            } else {
                throw e;
            }
        }
    }

    private static class Cache<K, V> extends LinkedHashMap<K, V> {
        final int maxSize;

        public Cache(int maxSize) {
            super(maxSize);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> kvEntry) {
            return size() > maxSize;
        }
    }
}
