package com.tikal.jenkins.plugins.multijob.MultiJobProject

import lib.LayoutTagLib

l = namespace(LayoutTagLib)
t = namespace("/lib/hudson")
p = namespace("/lib/hudson/project")
st = namespace("jelly:stapler")
f = namespace("lib/form")

st.bind(var: "it", value: my)

script(type: "text/javascript", src: "${rootURL}/plugin/jenkins-multijob-plugin/engine.js")
script(type: "text/javascript", src: "//code.jquery.com/ui/1.11.4/jquery-ui.js")
script(type: "text/javascript", src: "${rootURL}/plugin/jenkins-multijob-plugin/jquery.treetable.js")
link(rel: "stylesheet", type: "text/css", href: "${rootURL}/plugin/jenkins-multijob-plugin/jquery.treetable.css")
link(rel: "stylesheet", type: "text/css", href: "//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css")
link(rel: "stylesheet", type: "text/css", href: "${rootURL}/plugin/jenkins-multijob-plugin/style.css")
link(rel: "stylesheet", type: "text/css", href:
        "${rootURL}/plugin/jenkins-multijob-plugin/font-awesome/css/font-awesome.min.css")

if (my.supportsMakeDisabled()) {
    st.include(page: "makeDisabled.jelly")
}

div(class: "link-panel", align: "right") {
    a(id: "configureTable", href: "javascript:;", onclick: "Q('#tablePropertyDialog').dialog('open')", title:
            "Configure table columns", style: "float: right") {
        i(class: "fa fa-wrench") {
        }
        text("Configure table columns")
    }
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
                th(class: "job-last job-last-success") {
                    text("Last Success")
                }
                th(class: "job-last job-last-failure") {
                    text("Last Failure")
                }
                th(class: "job-last job-last-duration") {
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

        for (v in my.getHierarchy()) {
            tr('data-tt-id': v.getItemId(), 'data-tt-parent-id': v.getParentItemId(), class: 'multi-job') {
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
                        a(href: "${rootURL}" + v.getBuildUrl() + '/console') {
                            img(class: "largeIcon", src: "${imagesURL}/24x24/terminal.png",
                                    title: "Console output") {
                            }
                        }
                    }
                }
                td(class: "job-run") {
                    if (v.isProject()) {
                        a(href: "${rootURL}" + v.getUrl() + "build?delay=0sec", onclick: "return buildNow(this)") {
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

div(id: "tablePropertyDialog", title: "Configure table columnns") {
    label("Table columns")
    br()
    input(id: "is-job", type: "checkbox", name: "Job", checked: true) {
        text("Job")
    }
    br()
    input(id: "is-status", type: "checkbox", name: "S", checked: true) {
        text("S")
    }
    br()
    input(id: "is-weather", type: "checkbox", name: "W", checked: true) {
        text("W")
    }
    br()
    input(id: "is-build", type: "checkbox", name: "Build", checked: true) {
        text("Build")
    }
    br()
    input(id: "is-last-success", type: "checkbox", name: "lastSuccess", checked: true) {
        text("Last Success")
    }
    br()
    input(id: "is-last-failure", type: "checkbox", name: "lastFailure", checked: true) {
        text("Last Failure")
    }
    br()
    input(id: "is-last-duration", type: "checkbox", name: "lastDuration", checked: true) {
        text("Last Duration")
    }
    br()
    input(id: "is-console", type: "checkbox", name: "Console", checked: true) {
        text("Console")
    }
    br()
    input(id: "is-run", type: "checkbox", name: "Run", checked: true) {
        text("Run")
    }
    br()
    hr()
    input(id: "isGlobal", type: "checkbox", name: "isGlobal", checked: true, disabled: !my.canUserManageView()) {
        text("Apply settings globally")
    }
    br()
    a(id: "submitTableProperty", href: "javascript:;", class: "submitButton", onclick: "configureColumns()",
            style: "float: right") {
        text("Submit")
    }
}
