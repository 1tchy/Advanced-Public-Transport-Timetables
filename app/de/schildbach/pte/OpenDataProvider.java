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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.sun.istack.internal.NotNull;
import de.schildbach.pte.dto.*;
import de.schildbach.pte.dto.Connection.Part;
import de.schildbach.pte.dto.Connection.Trip;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
    private final Cache<String, GetConnectionDetailsResult> cachedConnectionDetails = new Cache<String, GetConnectionDetailsResult>(MAX_CACHED_CONNECTION_DETAILS);
    private final static int MAX_CACHED_REQUESTS = 500;
    private final static Cache<String, JsonObject> cachedRequests = new Cache<String, JsonObject>(MAX_CACHED_REQUESTS);
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
    public NearbyStationsResult queryNearbyStations(float lat, float lon, int maxDistance, int maxStations) throws IOException {
        JsonElement request = performRequest("locations?x=" + lat + "&y=" + lon).get("stations");
        List<Location> stations = new LinkedList<Location>();
        if ((request.isJsonArray())) {
            JsonArray request_arr = (JsonArray) request;
            for (JsonElement aStation : request_arr) {
                if (aStation.isJsonObject()) {
                    Location foundLocation = parseLocation((JsonObject) aStation);
                    float lonDiff = lon - foundLocation.lon;
                    float latDiff = lat - foundLocation.lat;
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
        throw new NotImplementedException();
    }

    /**
     * Meant for auto-completion of station names
     *
     * @param constraint input by user so far
     * @return auto-complete suggestions
     * @throws IOException
     */
    public List<Location> autocompleteStations(CharSequence constraint) throws IOException {
        JsonObject response = performRequest("locations?type=station&query=" + constraint);
        JsonArray responseArray = response.getAsJsonArray("stations");
        List<Location> ret = new ArrayList<Location>(responseArray.size());
        for (JsonElement onePossibleResponse : responseArray) {
            if (!(onePossibleResponse instanceof JsonObject)) continue;
            JsonObject onePossibleResponse_o = (JsonObject) onePossibleResponse;
            final int MINIMUM_SCORE = 90;
            JsonElement score = onePossibleResponse_o.get("score");
            if (score.isJsonPrimitive() && score.getAsInt() < MINIMUM_SCORE) continue;
            int id = onePossibleResponse_o.getAsJsonPrimitive("id").getAsInt();
            String name = onePossibleResponse_o.get("name").getAsString();
            ret.add(new Location(LocationType.STATION, id, name, name));
        }
        return ret;
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
        assert products == null : "Parameter 'Product' is not implemented yet.";
        StringBuilder request = new StringBuilder("connections?from=");
        request.append(from.id).append("&to=").append(to.id);
        if (via != null) request.append("&via[]=").append(via.id);
        if (date != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            timeFormat.setTimeZone(timeZone);
            request.append("&date=").append(new SimpleDateFormat("yyyy-MM-dd").format(date)).append("&time=").append(timeFormat.format(date));
        }
        if (!dep) request.append("&isArrivalTime=1");
        return queryConnections(request.toString());
    }

    private QueryConnectionsResult queryConnections(String request) throws IOException {
        JsonObject response = performRequest(request);
        ResultHeader header = new ResultHeader(SERVER_PRODUCT);
        List<Connection> connections = new ArrayList<Connection>();
        Location fromx = null, tox = null;
        int connectionNumber = 0;
        for (JsonElement connection_elem : response.getAsJsonArray("connections")) {
            if (!(connection_elem instanceof JsonObject)) continue;
            JsonObject connection = (JsonObject) connection_elem;
            JsonObject from_response = connection.getAsJsonObject("from").getAsJsonObject("station");
            JsonObject to_response = connection.getAsJsonObject("to").getAsJsonObject("station");
            JsonArray section_response = connection.getAsJsonArray("sections");
            int from_id = from_response.getAsJsonPrimitive("id").getAsInt();
            String from_name = from_response.getAsJsonPrimitive("name").getAsString();
            int to_id = to_response.getAsJsonPrimitive("id").getAsInt();
            String to_name = to_response.getAsJsonPrimitive("name").getAsString();
            fromx = new Location(LocationType.STATION, from_id, from_name, from_name);
            tox = new Location(LocationType.STATION, to_id, to_name, to_name);
            List<Part> parts = new ArrayList<Part>(section_response.size());
            for (JsonElement section_part_element : section_response) {
                Part trip = parsePart(section_part_element);
                if (trip == null) continue;
                parts.add(trip);
            }
            String connection_id = request + CONNECTION_NUMBER_SEPARATOR + connectionNumber++;
            Connection c = new Connection(connection_id, connection_id, fromx, tox, parts, null, null);
            cachedConnectionDetails.put(connection_id, new GetConnectionDetailsResult(new Date(), c));
            connections.add(c);
        }
        return new QueryConnectionsResult(header, request, fromx, null, tox, "", connections);
    }

    private Part parsePart(JsonElement section_part_element) {
        if (!(section_part_element instanceof JsonObject)) return null;
        JsonObject section_part = (JsonObject) section_part_element;
        JsonObject section_part_departure = section_part.getAsJsonObject("departure");
        Location departure = parseLocation(section_part_departure.getAsJsonObject("station"));
        JsonObject section_part_arrival = section_part.getAsJsonObject("arrival");
        Location arrival = parseLocation(section_part_arrival.getAsJsonObject("station"));
        int section_part_departure_id = section_part_departure.getAsJsonObject("station").getAsJsonPrimitive("id").getAsInt();
        String section_part_departure_name = section_part_departure.getAsJsonObject("station").getAsJsonPrimitive("name").getAsString();
        int section_part_arrival_id = section_part_arrival.getAsJsonObject("station").getAsJsonPrimitive("id").getAsInt();
        String section_part_arrival_name = section_part_arrival.getAsJsonObject("station").getAsJsonPrimitive("name").getAsString();
        Date departureTime;
        Date arrivalTime;
        try {
            departureTime = dateFormat.parse(section_part_departure.getAsJsonPrimitive("departure").getAsString());
            arrivalTime = dateFormat.parse(section_part_arrival.getAsJsonPrimitive("arrival").getAsString());
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not parse date: " + e);
        }
        boolean isWalk = section_part.has("walk");
        if (isWalk) {
            return new Connection.Footway(parseWalktime(section_part.getAsJsonObject("walk")), departure, arrival, new ArrayList<Point>(0));
        } else {
            JsonObject section_part_journey = section_part.getAsJsonObject("journey");
            Style style = new Style(0, 0xFFFFFF);
            if (section_part_journey == null) {
                return new Trip(new Line("", "?", style), null, departureTime, departureTime, section_part_departure.getAsJsonPrimitive("platform").getAsString(), new Location(LocationType.STATION, section_part_departure_id, section_part_departure_name, section_part_departure_name), arrivalTime, arrivalTime, section_part_arrival.getAsJsonPrimitive("platform").getAsString(), new Location(LocationType.STATION, section_part_arrival_id, section_part_arrival_name, section_part_arrival_name), new ArrayList<Stop>(0), new ArrayList<Point>(0));
            } else {
                Location destination = null;
                Line line = new Line(section_part_journey.getAsJsonPrimitive("name").getAsString(), parseCategory(section_part_journey), style);
                if (!section_part_journey.get("to").isJsonNull()) {
                    String destination_name = section_part_journey.getAsJsonPrimitive("to").toString();
                    destination = new Location(LocationType.STATION, -1, destination_name, destination_name);
                }
                JsonArray passList = section_part_journey.getAsJsonArray("passList");
                List<Stop> stops = new ArrayList<Stop>(passList.size());
                for (JsonElement passedStation : passList) {
                    if (!(passedStation instanceof JsonObject)) continue;
                    JsonObject passedStation_o = (JsonObject) passedStation;
                    String position = passedStation_o.getAsJsonPrimitive("platform").getAsString();
                    Date timeOfPass;
                    try {
                        JsonElement passTime = passedStation_o.get("departure");
                        if (passTime.isJsonNull()) {
                            timeOfPass = null;
                        } else {
                            timeOfPass = dateFormat.parse(passTime.getAsString());
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Could not parse date '" + passedStation_o.getAsJsonPrimitive("departure").getAsString() + "'");
                    }
                    stops.add(new Stop(parseLocation(passedStation_o.getAsJsonObject("station")), position, timeOfPass));
                }
                return new Trip(line, destination, departureTime, departureTime, section_part_departure.getAsJsonPrimitive("platform").getAsString(), new Location(LocationType.STATION, section_part_departure_id, section_part_departure_name, section_part_departure_name), arrivalTime, arrivalTime, section_part_arrival.getAsJsonPrimitive("platform").getAsString(), new Location(LocationType.STATION, section_part_arrival_id, section_part_arrival_name, section_part_arrival_name), stops, new ArrayList<Point>(passList.size()));
            }
        }
    }

    private int parseWalktime(JsonObject walk) {
        final DateFormat walktimeformat = new SimpleDateFormat("HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(walktimeformat.parse(walk.getAsJsonPrimitive("duration").getAsString()));
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return calendar.get(Calendar.MINUTE) + 60 * calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Transforms a JSonObject containing a Location to a Location-Object
     *
     * @param location Object to convert
     * @return Converted object
     */
    private Location parseLocation(JsonObject location) {
        String station_name = location.getAsJsonPrimitive("name").getAsString();
        return new Location(LocationType.STATION, location.getAsJsonPrimitive("id").getAsInt(), station_name, station_name);
    }

    private String parseCategory(JsonObject category) {
        if (category == null) return "";
        String cat = category.getAsJsonPrimitive("category").getAsString().toUpperCase();
        String prefix;
        final String[] schnellzugStrings = {"EC", "IC", "ICN", "ICE", "CNL", "EN", "THA", "TGV"};
        final String[] busString = {"NBU", "TRO", "BUS", "NTO", "NFB"};
        if (cat.matches("S\\d*")) { //S-Bahn
            prefix = "S";
        } else if (cat.equals("IR") | cat.startsWith("R")) { //Regionalzug
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
        String name = category.getAsJsonPrimitive("name").getAsString();
        JsonElement to = category.get("to");
        if (to.isJsonPrimitive()) {
            return prefix + name + " (nach " + to.getAsString() + ")";
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

    private JsonObject performRequest(String request) throws IOException {
        JsonObject response = cachedRequests.get(request);
        if (response == null) {
            response = performRequest(request, 5);
            if (response != null) {
                cachedRequests.put(request, response);
            }
        }
        return response;
    }

    @NotNull
    private JsonObject performRequest(String request, int retries) throws IOException {
        request = request.replace(" ", "+");
        try {
            return new JsonParser().parse(new JsonReader(new InputStreamReader(new URL(url + request).openStream()))).getAsJsonObject();
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
