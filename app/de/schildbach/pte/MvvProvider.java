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

import java.util.HashMap;
import java.util.Map;

import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class MvvProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.MVV;
	public static final String OLD_NETWORK_ID = "efa.mvv-muenchen.de";
	private static final String API_BASE = "http://efa.mvv-muenchen.de/mobile/";

	public MvvProvider()
	{
		super(API_BASE, null, false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	public boolean hasCapabilities(final Capability... capabilities)
	{
		for (final Capability capability : capabilities)
			if (capability == Capability.AUTOCOMPLETE_ONE_LINE || capability == Capability.DEPARTURES || capability == Capability.CONNECTIONS)
				return true;

		return false;
	}

	private static final Map<String, Style> LINES = new HashMap<String, Style>();

	static
	{
		LINES.put("SS1", new Style(Style.parseColor("#00ccff"), Style.WHITE));
		LINES.put("SS2", new Style(Style.parseColor("#66cc00"), Style.WHITE));
		LINES.put("SS3", new Style(Style.parseColor("#880099"), Style.WHITE));
		LINES.put("SS4", new Style(Style.parseColor("#ff0033"), Style.WHITE));
		LINES.put("SS6", new Style(Style.parseColor("#00aa66"), Style.WHITE));
		LINES.put("SS7", new Style(Style.parseColor("#993333"), Style.WHITE));
		LINES.put("SS8", new Style(Style.BLACK, Style.parseColor("#ffcc00")));
		LINES.put("SS20", new Style(Style.BLACK, Style.parseColor("#ffaaaa")));
		LINES.put("SS27", new Style(Style.parseColor("#ffaaaa"), Style.WHITE));
		LINES.put("SA", new Style(Style.parseColor("#231f20"), Style.WHITE));

		LINES.put("T12", new Style(Style.parseColor("#883388"), Style.WHITE));
		LINES.put("T15", new Style(Style.parseColor("#3366CC"), Style.WHITE));
		LINES.put("T16", new Style(Style.parseColor("#CC8833"), Style.WHITE));
		LINES.put("T17", new Style(Style.parseColor("#993333"), Style.WHITE));
		LINES.put("T18", new Style(Style.parseColor("#66bb33"), Style.WHITE));
		LINES.put("T19", new Style(Style.parseColor("#cc0000"), Style.WHITE));
		LINES.put("T20", new Style(Style.parseColor("#00bbee"), Style.WHITE));
		LINES.put("T21", new Style(Style.parseColor("#33aa99"), Style.WHITE));
		LINES.put("T23", new Style(Style.parseColor("#fff000"), Style.WHITE));
		LINES.put("T25", new Style(Style.parseColor("#ff9999"), Style.WHITE));
		LINES.put("T27", new Style(Style.parseColor("#ff6600"), Style.WHITE));
		LINES.put("TN17", new Style(Style.parseColor("#999999"), Style.parseColor("#ffff00")));
		LINES.put("TN19", new Style(Style.parseColor("#999999"), Style.parseColor("#ffff00")));
		LINES.put("TN20", new Style(Style.parseColor("#999999"), Style.parseColor("#ffff00")));
		LINES.put("TN27", new Style(Style.parseColor("#999999"), Style.parseColor("#ffff00")));

		LINES.put("UU1", new Style(Style.parseColor("#227700"), Style.WHITE));
		LINES.put("UU2", new Style(Style.parseColor("#bb0000"), Style.WHITE));
		LINES.put("UU2E", new Style(Style.parseColor("#bb0000"), Style.WHITE));
		LINES.put("UU3", new Style(Style.parseColor("#ee8800"), Style.WHITE));
		LINES.put("UU4", new Style(Style.parseColor("#00ccaa"), Style.WHITE));
		LINES.put("UU5", new Style(Style.parseColor("#bb7700"), Style.WHITE));
		LINES.put("UU6", new Style(Style.parseColor("#0000cc"), Style.WHITE));
	}

	@Override
	public Style lineStyle(final String line)
	{
		final Style style = LINES.get(line);
		if (style != null)
			return style;
		else
			return super.lineStyle(line);
	}

	@Override
	public Point[] getArea()
	{
		return new Point[] { new Point(48.140377f, 11.560643f) };
	}
}
