/*
 * Copyright 2010, 2011 the original author or authors.
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

package de.schildbach.pte;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.QueryDeparturesResult;

/**
 * @author Andreas Schildbach
 */
public class ZvvProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.ZVV;
	private static final String API_BASE = "http://onlinev2.fahrplan.zvv.ch/bin/"; // http://online.fahrplan.zvv.ch/bin/

	public ZvvProvider()
	{
		super(API_BASE + "query.exe/dn", 10, null, "UTF-8", "UTF-8");
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1)
			return 'I';
		if (value == 2)
			return 'I';
		if (value == 4)
			return 'R';
		if (value == 8)
			return 'R';
		if (value == 16)
			return 'F';
		if (value == 32)
			return 'S';
		if (value == 64)
			return 'B';
		if (value == 128)
			return 'C';
		if (value == 256)
			return 'U';
		if (value == 512)
			return 'T';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final char product)
	{
		if (product == 'I')
		{
			productBits.setCharAt(0, '1'); // ICE/EN/CNL/CIS/ES/MET/NZ/PEN/TGV/THA/X2
			productBits.setCharAt(1, '1'); // EuroCity/InterCity/InterCityNight/SuperCity
		}
		else if (product == 'R')
		{
			productBits.setCharAt(2, '1'); // InterRegio
			productBits.setCharAt(3, '1'); // Schnellzug/RegioExpress
		}
		else if (product == 'S')
		{
			productBits.setCharAt(5, '1'); // S-Bahn/StadtExpress/Eilzug/Regionalzug
		}
		else if (product == 'U')
		{
		}
		else if (product == 'T')
		{
			productBits.setCharAt(9, '1'); // Tram
		}
		else if (product == 'B' || product == 'P')
		{
			productBits.setCharAt(6, '1'); // Postauto/Bus
		}
		else if (product == 'F')
		{
			productBits.setCharAt(4, '1'); // Schiff/Fähre/Dampfschiff
		}
		else if (product == 'C')
		{
			productBits.setCharAt(7, '1'); // Luftseilbahn/Standseilbahn/Bergbahn
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Zürich" };

	@Override
	protected String[] splitPlaceAndName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.startsWith(place + ", "))
				return new String[] { place, name.substring(place.length() + 2) };
		}

		return super.splitPlaceAndName(name);
	}

	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		final StringBuilder uri = new StringBuilder(API_BASE);

		if (location.hasLocation())
		{
			uri.append("query.exe/dny");
			uri.append("?performLocating=2&tpl=stop2json");
			uri.append("&look_maxno=").append(maxStations != 0 ? maxStations : 150);
			uri.append("&look_maxdist=").append(maxDistance != 0 ? maxDistance : 5000);
			uri.append("&look_stopclass=").append(allProductsInt());
			uri.append("&look_x=").append(location.lon);
			uri.append("&look_y=").append(location.lat);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			uri.append("stboard.exe/dn");
			uri.append("?productsFilter=").append(allProductsString());
			uri.append("&boardType=dep");
			uri.append("&input=").append(location.id);
			uri.append("&sTI=1&start=yes&hcount=0");
			uri.append("&L=vs_java3");

			return xmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location.toDebugString());
		}
	}

	public QueryDeparturesResult queryDepartures(final int stationId, final int maxDepartures, final boolean equivs) throws IOException
	{
		final StringBuilder uri = new StringBuilder();
		uri.append(API_BASE).append("stboard.exe/dn");
		uri.append("?productsFilter=").append(allProductsString());
		uri.append("&boardType=dep");
		uri.append("&disableEquivs=yes"); // don't use nearby stations
		uri.append("&maxJourneys=50"); // ignore maxDepartures because result contains other stations
		uri.append("&start=yes");
		uri.append("&L=vs_java3");
		uri.append("&input=").append(stationId);

		return xmlQueryDepartures(uri.toString(), stationId);
	}

	public List<Location> autocompleteStations(final CharSequence constraint) throws IOException
	{
		return xmlMLcReq(constraint);
	}

	@Override
	protected Line parseLineAndType(final String lineAndType)
	{
		final Matcher m = P_NORMALIZE_LINE_AND_TYPE.matcher(lineAndType);
		if (m.matches())
		{
			final String number = m.group(1).replaceAll("\\s+", " ");
			final String type = m.group(2);

			if ("Bus-NF".equals(type))
				return newLine('B' + number, Line.Attr.WHEEL_CHAIR_ACCESS);
			if ("Tro-NF".equals(type))
				return newLine('B' + number, Line.Attr.WHEEL_CHAIR_ACCESS);
			if ("Trm-NF".equals(type))
				return newLine('T' + number, Line.Attr.WHEEL_CHAIR_ACCESS);

			if (type.length() > 0)
			{
				final char normalizedType = normalizeType(type);
				if (normalizedType != 0)
					return newLine(normalizedType + number);
			}

			throw new IllegalStateException("cannot normalize type " + type + " number " + number + " line#type " + lineAndType);
		}

		throw new IllegalStateException("cannot normalize line#type " + lineAndType);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		// E-Bus: Bus, Tram oder Zug?

		if ("S-BAHN".equals(ucType))
			return 'S';

		if ("T".equals(ucType))
			return 'T';
		if ("TRM".equals(ucType))
			return 'T';
		if ("TRM-NF".equals(ucType)) // Niederflur
			return 'T';

		if ("BUS-NF".equals(ucType)) // Niederflur
			return 'B';
		if ("TRO-NF".equals(ucType)) // Niederflur
			return 'B';
		if ("N".equals(ucType)) // Nachtbus
			return 'B';
		if ("BUXI".equals(ucType))
			return 'B';
		if ("TX".equals(ucType))
			return 'B';
		if ("E-BUS".equals(ucType))
			return 'B';
		if ("TROLLEY".equals(ucType))
			return 'B';

		if ("D-SCHIFF".equals(ucType))
			return 'F';

		if ("BERGBAHN".equals(ucType))
			return 'C';

		if ("UNB".equals(ucType))
			return '?';
		if ("???".equals(ucType))
			return '?';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
