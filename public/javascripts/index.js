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

function changeStraight(toStraight) {
    if (toStraight) {
        document.getElementById("crossoverNo").checked = true;
    } else {
        document.getElementById("crossoverYes").checked = true;
        check("to");
    }
    check("from");
    document.focus();
}
function changeImage(hover) {
    var anzahl_from = document.getElementById("from").getElementsByTagName("input").length;
    var anzahl_to = document.getElementById("to").getElementsByTagName("input").length;
    var maximum = 4;
    anzahl_from = Math.min(anzahl_from, maximum);
    anzahl_to = Math.min(anzahl_to, maximum);
    var straight = document.getElementById("crossoverNo").checked;
    if (hover) straight = !straight;
    document.getElementById("mixstraight").src = "/public/images/" + (straight ? "straight" : "mix") + "/" + anzahl_from + (straight ? "" : anzahl_to) + ".png";
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
function addDirectCheck(number) {
    field = document.createElement("input");
    field.setAttribute("class", "direct");
    field.setAttribute("name", "direct[]");
    field.setAttribute("type", "checkbox");
    field.setAttribute("value",number);
    document.getElementById("direct").appendChild(field);
}

function check(whatToCheck) {
    var needAlignement = document.getElementById("crossoverNo").checked;
    var direct = document.getElementById("direct").getElementsByTagName("input");
    if (needAlignement) {
        var froms = document.getElementById("from").getElementsByTagName("input");
        var tos = document.getElementById("to").getElementsByTagName("input");
        var doubleEmptyFieldPresent = true;
        if (froms.length < tos.length) {
            for (var j = tos.length - froms.length; j > 0; j--) {
                addField("from");
            }
        } else if (froms.length > tos.length) {
            for (var jj = froms.length - tos.length; jj > 0; jj--) {
                addField("to");
            }
        } else {
            doubleEmptyFieldPresent = froms[froms.length - 1].value == "" && tos[tos.length - 1].value == "";
            for (var ii = froms.length - 2; ii >= 0; ii--) {
                if (froms[ii].value == "" && tos[ii].value == "" && froms[ii + 1].value == "" && tos[ii + 1].value == "" && document.getElementById("from").getElementsByTagName("input").length > 2) {
                    document.getElementById("from").removeChild(froms[ii]);
                    document.getElementById("to").removeChild(tos[ii]);
                    document.getElementById("direct").removeChild(direct[ii]);
                    doubleEmptyFieldPresent = true;
                }
            }
            if (!doubleEmptyFieldPresent) {
                addField("from");
                addField("to");
            }
        }
        if(direct.length<tos.length) {
            for(var d=direct.length;d<tos.length;d++) {
                addDirectCheck(d);
            }
        }
        for(d=1;d<direct.length;d++) {
            direct[d].style.visibility='visible';
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
        for(d=1;d<direct.length;d++) {
            direct[d].style.visibility='hidden';
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

function localize() {
    if($("input").get(0).value=="") {
        navigator.geolocation.getCurrentPosition(localize_by_position);
    }
}

function localize_by_position(position) {
    if(position != undefined) {
        $.ajax({
            type: "GET",
            url: "/station?lat="+position.coords.latitude+"&lon="+position.coords.longitude,
            async: true,
            beforeSend: function(x) {
                if(x && x.overrideMimeType) {
                    x.overrideMimeType("application/j-son;charset=UTF-8");
                }
            },
            dataType: "json",
            success: function(data){
                if(data.length>0) {
                    $("input").get(0).value=data[0].name;
                }
            }
        });
    }
}