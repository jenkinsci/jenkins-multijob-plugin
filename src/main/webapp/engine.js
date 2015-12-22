"use strict";

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

    function statusIntervalTrigger() {
        return window.setInterval(function () {
            reloadStatus();
            count++;
            if (60 == count) {
                count = 0;
            }
        }, 1000);
    }

    statusIntervalTrigger();

    function reloadStatus() {
        var isBuilding = false;
        it.isBuilding(function(t) {
            isBuilding = t.responseObject();
        });
        it.getHierarchy(function(t) {
            var h = t.responseObject();
            h.each(function(v) {
                var query = '.multi-job[data-tt-id=' + v.itemId + '] ';
                Q(query + '.job-status img')[0].setAttribute('src', resURL + '/images/32x32/' + v.statusIconColor);
                Q(query + '.job-weather img')[0].setAttribute('src', resURL + '/images/32x32/' + v.weatherIconUrl);
                if (v.project) {
                    Q(query + '.job-last-duration')[0].textContent = v.lastDuration;
                    if (v.build) {
                        Q(query + '.job-build')[0].innerHTML = '<a href="' + rootURL + v.buildUrl + '">' + v.buildName + '</a>';
                        Q(query + '.job-console a')[0].setAttribute('href', rootURL + v.buildUrl + 'console');
                    }
                    if (0 == count) {
                        Q(query + '.job-last-success')[0].textContent = v.lastSuccess;
                        Q(query + '.job-last-failure')[0].textContent = v.lastFailure;
                    }
                }
            });
        });
    }

    reloadStatus();
});

function scheduleBuild(a) {
    new Ajax.Request(a.href);
    hoverNotification('Build scheduled', a.parentNode);
    return false;
}