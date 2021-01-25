## When to use MultiJob (tikal-multijob-plugin) plugin ?
- If you'd like to stop the mess with downstream / upstream jobs chains definitions
- When you want to add full hierarchy of Jenkins jobs that will be executed in sequence or in parallel
- Add context to your buildflow implementing parameter inheritance from the MultiJob to all its Phases and Jobs, Phases are sequential whilst jobs inside each Phase are parallel

### More info on wiki page @: https://wiki.jenkins.io/display/JENKINS/Multijob+Plugin

### Found a bug ? require a new feature ?
#### Feel free to open an issue: https://github.com/jenkinsci/tikal-multijob-plugin/issues
****
### Plugin CI job: https://jenkins.ci.cloudbees.com/job/plugins/job/tikal-multijob-plugin/

========
# Does anyone want to become a maintainer of this plugin???
This plugin was very popular for many years, but since most of Jenkins usage now is based on Jenkins Pipeline, and the 'parallel' step along with 'BlueOcean' can basically do whatever this plugin does, it is no longer in [Tikal's radar](https://fullstackradar.tikalk.com/tikal/radar/index.html).
Also, we published the [parallelPhase library class](https://github.com/TikalCI/tci-library/blob/master/src/tci/pipeline/parallelPhase.groovy) that can do in pipelines what the plugin does.
Still, there are many users of the plugin that asked for fixes and new features, so we'll appreciate anyone willing to become a maintainer of the plugin.
