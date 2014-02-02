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

package actions;

import com.typesafe.plugin.MailerAPI;
import com.typesafe.plugin.MailerPlugin;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.SimpleResult;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 29.12.12
 * Time: 23:00
 */
public class MailAction extends Action.Simple {
    @Override
    public F.Promise<SimpleResult> call(Http.Context context) throws Throwable {
        F.Promise<SimpleResult> result;
//        try {
        result = delegate.call(context);
        /*} catch (Throwable e) {
            //Send mail
            sendErrorMail(e, context.request());
            //Show error message
            Html html = Application.getErrorPage(e);
            if (html == null) {
                result = internalServerError();
            } else {
                result = internalServerError(html);
            }
        }*/
        return result;
    }

    public static void sendErrorMail(Throwable e, Http.RequestHeader request) {
        MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
        mail.setSubject("Exception thrown: " + e.getMessage());
        String mailAddress = "ymfhaad6n9" + "@" + "laurinmurer.ch";
        mail.addRecipient(mailAddress);
        mail.addFrom(mailAddress);
        StringBuilder mailContent = new StringBuilder("Request was: http://");
        mailContent.append(request.host()).append(request.uri());
        mailContent.append("\nFrom: ").append(request.remoteAddress());
        mailContent.append("\n\nWith headers:");
        Map<String, String[]> headers = request.headers();
        if (headers != null) {
            for (String headerKey : headers.keySet()) {
                mailContent.append("\n    ").append(headerKey).append(":");
                for (String headerValue : headers.get(headerKey)) {
                    mailContent.append("\n      - ").append(headerValue);
                }
            }
        } else {
            mailContent.append(" no headers found.");
        }
        mailContent.append("\n\nStacktrace:\n");
        for (StackTraceElement element : e.getStackTrace()) {
            mailContent.append(element).append("\n");
        }
        mail.send(mailContent.toString());
    }

}