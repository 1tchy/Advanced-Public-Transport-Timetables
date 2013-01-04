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

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Iterator;

public class Directs extends ArrayList<Boolean> {
    private final boolean defaultValue;

    public Directs(@SuppressWarnings("SameParameterValue") boolean defaultValue) {
        super();
        this.defaultValue = defaultValue;
    }

    @Override
    public Boolean get(int i) {
        if (contains(i)) {
            return super.get(i);
        } else return defaultValue;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    @SuppressWarnings("NullableProblems")
    @Override
    @NotNull
    public Iterator<Boolean> iterator() {
        return new Iterator<Boolean>() {
            private int position = 0;

            @Override
            public boolean hasNext() {
                throw new UnsupportedOperationException("There is always a next!");
            }

            @Override
            public Boolean next() {
                return get(position++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
//        return super.iterator();
    }
}
