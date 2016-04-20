"use strict";

var hoverNotification2 = (function() {
    var msgBox;
    var body;

    // animation effect that automatically hide the message box
    var effect = function(overlay, dur) {
        var o = YAHOO.widget.ContainerEffect.FADE(overlay, dur);
        o.animateInCompleteEvent.subscribe(function() {
            window.setTimeout(function() {
                msgBox.hide()
            }, 1500);
        });
        return o;
    }

    function init() {
        if(msgBox!=null)  return;   // already initialized

        var div = document.createElement("DIV");
        document.body.appendChild(div);
        div.innerHTML = "<div id=hoverNotification><div class=bd></div></div>";
        body = $('hoverNotification');

        msgBox = new YAHOO.widget.Overlay(body, {
            visible:false,
            width:"10em",
            zIndex:1000,
            effect:{
                effect:effect,
                duration:0.25
            }
        });
        msgBox.render();
    }

    return function(title, anchor, offsetH, offsetW) {
        if (typeof offsetH === 'undefined') {
            offsetH = 48;
        }
        if (typeof offsetW === 'undefined') {
            offsetW = 20;
        }
        init();
        body.innerHTML = title;
        var xy = YAHOO.util.Dom.getXY(anchor);
        xy[0] += offsetH;
        xy[1] += offsetW;
        //xy[0] += offset;
        //xy[1] += anchor.offsetHeight;
        msgBox.cfg.setProperty("xy",xy);
        msgBox.show();
    };
})();


function updateTableColumns() {
    it.getColumnProps(function (t) {
        var m = t.responseObject();
        Object.keys(m).forEach(function (v) {
            var key = '.job-' + v;
            var value = m[v];
            if (value) {
                Q(key).show();
            } else {
                Q(key).hide();
            }
        })
    });
}

function configureColumns() {
    var isGlobal = Q('#isGlobal')[0].checked;
    it.getColumnProps(function(t) {
        var m = t.responseObject();
        Object.keys(m).forEach(function(v) {
            var key = '#is-' + v;
            var klass = '.job-' + v;
            var value = Q(key)[0].checked;
            if (value) {
                Q(klass).show();
            } else {
                Q(klass).hide();
            }
            it.setColumnState(v, value, isGlobal);
        });
    });
    Q('#tablePropertyDialog').dialog('close');
}

jQuery(document).ready(function() {
    Q('#multiJobTable').treetable({
        expandable: true,
        expanderTemplate: "<i class='fa'></i>"
    });
    var nodes = Q('#multiJobTable').data('treetable').nodes;
    nodes.each(function(v) {
        v.expand();
    });
    var count = 0;

    it.canUserManageView(function(t) {
        var h = t.responseObject();
        if (h) {
            Q('#isGlobal')[0].enable();
        }
    });

    function statusIntervalTrigger() {
        return window.setInterval(function () {
            reloadStatus();
            count++;
            if (60 == count) {
                count = 0;
            }
        }, 2000);
    }

    statusIntervalTrigger();

    function reloadStatus() {
        updateTableColumns();
        var isBuilding = false;
        it.isBuilding(function(t) {
            isBuilding = t.responseObject();
        });
        it.getHierarchy(function(t) {
            var h = t.responseObject();
            var imgBaseUrl = resURL + '/images/32x32/';
            h.each(function(v) {
                var query = '.multi-job[data-tt-id=' + v.itemId + '] ';
                var obj = Q(query);
                obj.find('.job-status img')[0].setAttribute('src', imgBaseUrl + v.statusIconColor);
                obj.find('.job-weather img')[0].setAttribute('src', imgBaseUrl + v.weatherIconUrl);
                if (v.project) {
                    obj.find('.job-last-duration')[0].textContent = v.lastDuration;
                    if (v.build) {
                        obj.find('.job-build')[0].innerHTML = '<a href="' + rootURL + v.buildUrl + '">' + v.buildName + '</a>';
                        if ("NOT_BUILT" == v.status) {
                            obj.find('.job-console a')[0].addClassName('not-active');
                        } else {
                            var aobj = obj.find('.job-console a');
                            aobj[0].removeClassName('not-active');
                            aobj[0].setAttribute('href', rootURL + v.buildUrl + '/console');
                            aobj.mouseover(function() {
                                hoverNotification2("#" + v.buildNumber, this, -36, 30);
                            });
                        }
                    }
                    if (0 == count) {
                        obj.find('.job-last-success')[0].textContent = v.lastSuccess;
                        obj.find('.job-last-failure')[0].textContent = v.lastFailure;
                    }
                }
            });
        });
    }

    reloadStatus();
});

Q(function() {
    var dialog = Q('#tablePropertyDialog').dialog({
        height: 250,
        width: 200,
        modal: true,
        resizable: false,
        autoOpen: false,
        dialogClass: 'no-close table-property-dialog',
        open: function() {
            it.getColumnProps(function(t) {
                var m = t.responseObject();
                Object.keys(m).forEach(function(v) {
                    var key = '#is-' + v;
                    Q(key)[0].checked = m[v];
                });
            });
        }
    });
});
