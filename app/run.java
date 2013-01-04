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

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: Laurin
 * Date: 19.12.12
 * Time: 09:29
 */
public class run {

    private static final String PLAY_RUNTIME = "/Applications/play-2.0.4/play ";

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        System.out.println("Started compilation...");
        Runtime.getRuntime().exec(PLAY_RUNTIME + "compile").waitFor();

        System.out.println("Compilation finished, starting now the play server...");
        Process play = Runtime.getRuntime().exec(PLAY_RUNTIME + "debug run");

        BufferedReader input = new BufferedReader(new InputStreamReader(play.getInputStream()));
        String line;
        while ((line = input.readLine()) != null) {
            if (line.contains("Listening for HTTP on port 9000")) {
                Desktop.getDesktop().browse(new URI("http://localhost:9000/"));
                break;
            }
        }

        System.out.println("Play server is running, if you want to terminate it: Use your favorite process manager.");

        System.out.print("Suggestion, start now the run configuration 'start debuger'.");
    }

}
