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
import org.junit.Test;
import play.test.UnitTest;

/**
 * Created by IntelliJ IDEA.
 * User: Laurin
 * Date: 23.11.11
 * Time: 11:02
 */
public class KnownProviderTest extends UnitTest {
    @Test
    public void all() {
        assert KnownProvider.all().size() > 0 : "There must be at least one provider in the list of all KnownProviders.";
    }

    @Test
    public void get() {
        NetworkProvider networkProvider = KnownProvider.get("sbb");
        assert networkProvider != null : "KnownProvider.get(\"sbb\") should return a NetworkProvider.";
        String id = KnownProvider.getName(networkProvider);
        assert id.equals("SBB") : "KnownProvider.get(networkProvider of the sbb) should return 'SBB' but returned " + id + ".";
    }

    @Test
    public void getById() {
        KnownProvider kp = KnownProvider.getById("sbb");
        assert kp != null : "KnownProvider.getById(\"sbb\") should not return null.";
        assert kp.humanName.equals("SBB");
        assert kp.id.equals("sbb");
        assert kp.timeZone.getID().equals("Europe/Zurich");
    }
}
