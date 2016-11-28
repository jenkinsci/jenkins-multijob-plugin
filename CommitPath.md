# Commit Path

 ## Applicable

 The commit path feature is applicable if:
 - You have the SCM defined in your main MultiJob configuration.
 - You want to exclude most path jobs from execution if commits touch only certain paths.

 ## Details
 Activate the feature by clicking the checkbox _Use 'Commit Path' in phase jobs._ in section _Multijob specific configuration_

 If checked, patterns defined in phase jobs of the MultiJob will be considered when determining which jobs to be executed.

 This is only valid if ALL paths in a commit are covered by patterns.

 The same path may be present in more than one job.

 ##Example:
 We have a large number of jobs in our first phase. Job-One and Job-Two runs integration tests
   and will be run on any change. But there is no need to run anything else if ONLY the integration
   tests themselves are updated.
 We configure the projects to use commit path:

    Job-One: project1/src/integTest/java/.*
    Job-Two: project2/src/integTest/java/.*

 When a commit affects paths...

    project1/src/integTest/java/com/acme/test/TestOne.java
    project2/src/integTest/java/com/acme/test/TestSomethingElse.java

 ...then only the jobs Job-One and Job-Two will be executed.

 If another change (project1/src/main/java/com/acme/product/Main.java) would be present
 that is not covered in a phase job config, then ALL jobs will be executed for the whole change set.
