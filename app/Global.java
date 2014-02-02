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

import actions.MailAction;
import controllers.Application;
import play.GlobalSettings;
import play.api.templates.Html;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.SimpleResult;

import javax.annotation.Nullable;

import static play.mvc.Results.*;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 30.12.12
 * Time: 20:46
 */
public class Global extends GlobalSettings {

    @Override
    public Promise<SimpleResult> onError(Http.RequestHeader requestHeader, Throwable throwable) {
        Html html = workaroundProblem(requestHeader, "Error: " + throwable.getLocalizedMessage(), null);
        if (html == null) {
            return Promise.<SimpleResult>pure(internalServerError());
        }
        return Promise.<SimpleResult>pure(internalServerError(html));
    }

    @Override
    public Promise<SimpleResult> onHandlerNotFound(Http.RequestHeader requestHeader) {
        Html html = workaroundProblem(requestHeader, "HandlerNotFound", null);
        if (html == null) {
            return Promise.<SimpleResult>pure(notFound());
        }
        return Promise.<SimpleResult>pure(notFound(html));
    }

    @Override
    public Promise<SimpleResult> onBadRequest(Http.RequestHeader requestHeader, String s) {
        Html html = workaroundProblem(requestHeader, "BadRequest: " + s, null);
        if (html == null) {
            return Promise.<SimpleResult>pure(badRequest());
        }
        return Promise.<SimpleResult>pure(badRequest(html));
    }

    private static Html workaroundProblem(Http.RequestHeader requestHeader, String problem, @Nullable Throwable throwable) {
        if (throwable == null) {
            throwable = new Exception(problem);
        }
        MailAction.sendErrorMail(throwable, requestHeader);
        return Application.getErrorPage(throwable);
    }
}
