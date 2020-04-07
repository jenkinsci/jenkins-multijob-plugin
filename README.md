## When to use MultiJob (tikal-multijob-plugin) plugin ?
- If you'd like to stop the mess with downstream / upstream jobs chains definitions
- When you want to add full hierarchy of Jenkins jobs that will be executed in sequence or in parallel
- Add context to your buildflow implementing parameter inheritance from the MultiJob to all its Phases and Jobs, Phases are sequential whilst jobs inside each Phase are parallel

### More info on wiki page @: https://wiki.jenkins-ci.org/display/JENKINS/Multijob+Plugin

### Found a bug ? require a new feature ?
#### Feel free to open an issue: https://github.com/jenkinsci/tikal-multijob-plugin/issues
****
### Plugin CI job: https://jenkins.ci.cloudbees.com/job/plugins/job/tikal-multijob-plugin/
