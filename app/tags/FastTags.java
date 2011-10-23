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
package tags;

import groovy.lang.Closure;
import play.templates.GroovyTemplate.*;
import play.templates.JavaExtensions;
import play.templates.TagContext;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * User: Laurin
 * Date: Oct 19, 2011
 * Time: 10:41:52 AM
 * To change this template use File | Settings | File Templates.
 */
@FastTags.Namespace("my")
public class FastTags extends play.templates.FastTags {
    public static void _isit(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {

        Object klasse = args.get("arg");
        Object klassenname = args.get("a");
        if (args.containsKey("notnull")) {
            if (args.get("notnull") == null || args.get("notnull").toString() == null || args.get("notnull").toString().length() == 0) {
                return;
            }
        }
        if (klasse == null || klassenname == null) {
            return;
        }
        Class theClass = klasse.getClass();
        if (theClass == null) {
            return;
        }
        klasse = theClass.getSimpleName();
        if (!(klassenname instanceof String)) {
            klassenname = klassenname.getClass();
            if (klassenname == null) {
                return;
            }
            klassenname = klassenname.getClass().getSimpleName();

        }
        if (klasse == null) {
            return;
        }
        if (klasse.equals(klassenname)) {
            out.print(JavaExtensions.toString(body));
        } else {
            for (Class allClasses : theClass.getClasses()) {
                if (allClasses.getSimpleName().equals(klassenname)) {
                    out.print(JavaExtensions.toString(body));
                    return;
                }
            }
        }
    }

}
