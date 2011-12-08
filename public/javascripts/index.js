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

function changeStraight(toStraight) {
    if (toStraight) {
        document.getElementById("crossoverNo").checked = true;
    } else {
        document.getElementById("crossoverYes").checked = true;
        check("stop");
    }
    check("start");
    document.focus();
}
function changeImage(hover) {
    var anzahl_start = document.getElementById("start").getElementsByTagName("input").length;
    var anzahl_stop = document.getElementById("stop").getElementsByTagName("input").length;
    var maximum = 4;
    anzahl_start = Math.min(anzahl_start, maximum);
    anzahl_stop = Math.min(anzahl_stop, maximum);
    var straight = document.getElementById("crossoverNo").checked;
    if (hover) straight = !straight;
    document.getElementById("mixstraight").src = "/public/images/" + (straight ? "straight" : "mix") + "/" + anzahl_start + (straight ? "" : anzahl_stop) + ".png";
}
function addField(ofType) {
    field = document.createElement("input");
    field.setAttribute("class", "autocompleteStation");
    field.setAttribute("type", "text");
    field.setAttribute("name", ofType + "[]");
    field.setAttribute("onkeyup", "check('" + ofType + "');");
    field.setAttribute("data-url", "/autocomplete");
    field.setAttribute("style", "margin-right:5px;");
    document.getElementById(ofType).appendChild(field);
    autocomplete();
}

function check(whatToCheck) {
    var needAlignement = document.getElementById("crossoverNo").checked;
    if (needAlignement) {
        var starts = document.getElementById("start").getElementsByTagName("input");
        var stops = document.getElementById("stop").getElementsByTagName("input");
        var doubleEmptyFieldPresent = true;
        if (starts.length < stops.length) {
            for (var j = stops.length - starts.length; j > 0; j--) {
                addField("start");
            }
        } else if (starts.length > stops.length) {
            for (var jj = starts.length - stops.length; jj > 0; jj--) {
                addField("stop");
            }
        } else {
            doubleEmptyFieldPresent = starts[starts.length - 1].value == "" && stops[stops.length - 1].value == "";
            for (var ii = starts.length - 2; ii >= 0; ii--) {
                if (starts[ii].value == "" && stops[ii].value == "" && starts[ii + 1].value == "" && stops[ii + 1].value == "" && document.getElementById("start").getElementsByTagName("input").length > 2) {
                    document.getElementById("start").removeChild(starts[ii]);
                    document.getElementById("stop").removeChild(stops[ii]);
                    doubleEmptyFieldPresent = true;
                }
            }
            if (!doubleEmptyFieldPresent) {
                addField("start");
                addField("stop");
            }
        }
    } else {
        var inputs = document.getElementById(whatToCheck).getElementsByTagName("input");
        var emptyField = -1;
        for (var i = inputs.length - 1; i >= 0; i--) {
            if (inputs[i].value == "") {
                if (emptyField == i + 1 && document.getElementById(whatToCheck).getElementsByTagName("input").length > 2) {
                    document.getElementById(whatToCheck).removeChild(inputs[emptyField]);
                }
                emptyField = i;
            }
        }
        if (emptyField == -1) {
            addField(whatToCheck);
        }
    }
    changeImage(false);
}

function autocomplete() {
    $('input.autocompleteStation').each(function () {
        var $input = $(this);
        var serverUrl = $input.data('url');
        $input.autocomplete({ source:serverUrl });
    });
}