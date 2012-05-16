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

import de.schildbach.pte.dto.Point;

/**
 * @author Andreas Schildbach
 */
public class VvsProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.VVS;
	public static final String OLD_NETWORK_ID = "mobil.vvs.de";
	private static final String API_BASE = "http://mobil.vvs.de/mobile/"; // http://www2.vvs.de/vvs/

	public VvsProvider()
	{
		super(API_BASE, null, true);
	}

	public VvsProvider(final String apiBase)
	{
		super(apiBase, null, true);
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

	@Override
	public Point[] getArea()
	{
		return new Point[] { new Point(48.784068f, 9.181713f) };
	}
}
