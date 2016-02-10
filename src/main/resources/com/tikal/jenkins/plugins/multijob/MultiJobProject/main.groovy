package com.tikal.jenkins.plugins.multijob.MultiJobProject

import lib.LayoutTagLib

l = namespace(LayoutTagLib)
t = namespace("/lib/hudson")
p = namespace("/lib/hudson/project")
st = namespace("jelly:stapler")
f = namespace("lib/form")

st.bind(var: "it", value: my)

script(type: "text/javascript", src: "${rootURL}/plugin/jenkins-multijob-plugin/engine.js")
script(type: "text/javascript", src: "${rootURL}/plugin/jenkins-multijob-plugin/jquery.treetable.js")
link(rel: "stylesheet", type: "text/css", href: "${rootURL}/plugin/jenkins-multijob-plugin/jquery.treetable.css")
link(rel: "stylesheet", type: "text/css", href: "${rootURL}/plugin/jenkins-multijob-plugin/style.css")
link(rel: "stylesheet", type: "text/css", href:
        "${rootURL}/plugin/jenkins-multijob-plugin/font-awesome/css/font-awesome.min.css")

if (my.supportsMakeDisabled()) {
    st.include(page: "makeDisabled.jelly")
}


div(id: "statusTable") {
    table(id: "multiJobTable", class: "table") {
        thead {
            tr(class: "job-row") {
                th(class: "job-job") {
                    i(class: "fa") {
                    }
                    text("Job")
                }
                th(class: "job-status") {
                    text("S")
                }
                th(class: "job-weather") {
                    text("W")
                }
                th(class: "job-build") {
                    text("Build")
                }
                th(class: "job-last") {
                    text("Last Success")
                }
                th(class: "job-last") {
                    text("Last Failure")
                }
                th(class: "job-last") {
                    text("Last Duration")
                }
                th(class: "job-console") {
                    text("Console")
                }
                th(class: "job-run") {
                    text("Run")
                }
            }
        }

        int count = 0;

        for (v in my.getHierarchy()) {
            count++
            String jobRowClass = count % 2 ? "job-rowA" : "job-rowB"
            jobRowClass += " multi-job"
            tr('data-tt-id': v.getItemId(), 'data-tt-parent-id': v.getParentItemId(), class: jobRowClass) {
                if (v.isProject()) {
                    td(class: "job-project") {
                        a(href: "${rootURL}" + v.getUrl()) {
                            text(v.getName())
                        }
                    }
                } else {
                    td(class: "job-phase") {
                        text(v.getName())
                    }
                }
                td(class: "job-status") {
                    img(src: "${imagesURL}/32x32/" + v.getStatusIconColor(),
                            title: v.getStatus(), alt: v.getStatus())
                }
                td(class: "job-weather") {
                    img(src: "${imagesURL}/32x32/" + v.getWeatherIconUrl(), alt: v.getWeather())
                }
                td(class: "job-build") {
                    if (v.isProject()) {
                        if (v.isBuild()) {
                            a(href: "${rootURL}" + v.getBuildUrl()) {
                                text(v.getBuildName())
                            }
                        } else {
                            text("N/A")
                        }
                    }
                }
                td(class: "job-last job-last-success") {
                    text(v.getLastSuccess())
                }
                td(class: "job-last job-last-failure") {
                    text(v.getLastFailure())
                }
                td(class: "job-last job-last-duration") {
                    text(v.getLastDuration())
                }
                td(class: "job-console") {
                    if (v.isProject() && v.isBuild()) {
                        a(href: "${rootURL}" + v.getBuildUrl() + "console") {
                            img(class: "largeIcon", src: "${imagesURL}/24x24/terminal.png",
                                    title: "Console output") {
                            }
                        }
                    }
                }
                td(class: "job-run") {
                    if (v.isProject()) {
                        a(href: "${rootURL}/" + v.getUrl() + "build?delay=0sec", onclick: "return scheduleBuild(this)") {
                            img(src: "${imagesURL}/24x24/clock.png") {
                            }
                        }
                    }
                }
            }
        }
    }

    t.iconSize() {
    }
}

p.projectActionFloatingBox() {

}

table(style: "margin-top: 1em; margin-left:1em;") {
    for (v in my.getProminentActions()) {
        t.summary(icon: v.getIconFileName(), href: v.getUrlName()) {
            text(v.getDisplayName())
        }
    }

    t.summary(icon: "folder.gif", href: "ws/", permission: my.WORKSPACE) {
        text("Workspace")
    }

    if (null != my.getLastSuccessfulBuild()) {
        t.artifactList(caption: "Last Successful Artifacts", build: my.getLastSuccessfulBuild(),
                baseURL: my.getLastSuccessfulBuild(), permission: my.getLastSuccessfulBuild().ARTIFACTS) {
        }
    }

    t.summary(icon: "notepad.gif", href: "changes") {
        text("Recent Changes")
    }
}

if (null != my.getTestResultAction()) {
    t.summary(icon: "clipboard.png") {
        a(href: "lastCompletedBuild/testReport/") {
            text("Latest Test Result")
        }
        text(" ")
        t.test-result(it: my.getTestResultAction()) {
        }
    }
}

st.include(page: "updownprojects.jelly")