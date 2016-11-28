![picture alt](http://www.tikalk.com/assets/logo-1bb9a466c9d195c148557434c3437446.png "Tikal Community") Jenkins MultiJob Plugin
====

## When to use MultiJob plugin ?
- If you'd like to stop the mess with downstream / upstream jobs chains definitions
- When you want to add full hierarchy of Jenkins jobs that will be executed in sequence or in parallel
- Add context to your buildflow implementing parameter inheritance from the MultiJob to all its Phases and Jobs, Phases are sequential whilst jobs inside each Phase are parallel

## News
In version 1.24 there is support for skipping phase jobs based on paths touched in the commit.
You can read the details [here](CommitPath.md).

In version 1.17 we inject new variables to use them in conditions, powering this feature.
You can read the details (variable names, values, ...) [here](AboutNewVariables.md).

### More info on wiki page @: https://wiki.jenkins-ci.org/display/JENKINS/Multijob+Plugin

### Found a bug ? require a new feature ?
#### Feel free to open an issue: https://issues.jenkins-ci.org/secure/CreateIssue!default.jspa
****
### Plugin CI job @: https://jenkins.ci.cloudbees.com/job/plugins/job/tikal-multijob-plugin/ [![Build Status](https://jenkins.ci.cloudbees.com/job/plugins/job/tikal-multijob-plugin/badge/icon)](https://jenkins.ci.cloudbees.com/job/plugins/job/tikal-multijob-plugin/)
