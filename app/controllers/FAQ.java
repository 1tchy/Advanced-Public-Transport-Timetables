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

package controllers;

import actions.MailAction;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 05.01.13
 * Time: 12:05
 */
public class FAQ extends Controller {

    /**
     * Shows a page to answer (all) questions a User of this page might have (frequently asked questions).
     */
    @With(MailAction.class)
    public static Result show() {
        return ok(views.html.Application.faq.render());
    }

}
