#The new variables available in conditions
## Variables related to job and used only at job condition evaluation
We injected the following variables previous to a job condition evaluation.
These variables are the following:

|NAME                   | Type   | Description |
|-----------------------|--------|-------------|
|**JOB_IS_BUILDABLE**   |Boolean |If the status of the job allows the building ```true```, otherwise ```false```.|
|**JOB_STATUS**         |String  |The name of the type of [StatusJob](#about-statusjob-enumeration) enumeration. |

## Variables related to the previous phase
We injected the following variables at the end of the phase execution, which are related to the previous phase.
These variables are the following:

|NAME                            | Type   | Description                                                                |
|--------------------------------|--------|----------------------------------------------------------------------------|
|**PHASE_RESULT**                |String  |Represents the result of the previous phase as a text. The result of the phase is the worse of the job results triggered.|
|**PHASE_SUCCESSFUL**            |Numeric |The number of triggered jobs in previous phase with **UNSTABLE** or **SUCCESS** results.|
|**PHASE_STABLE**                |Numeric |The number of triggered jobs in previous phase with **SUCCESS** results.    |
|**PHASE_UNSTABLE**              |Numeric |The number of triggered in previous phase jobs with **UNSTABLE** results.   |
|**PHASE_FAILED**                |Numeric |The number of triggered jobs in previous phase with **FAILED** results.     |
|**PHASE_ABORTED**               |Numeric |The number of triggered jobs in previous phase with **ABORTED** results.    |
|**PHASE_SKIPPED**               |Numeric |The number of skipped jobs in previous phase.                               |

## Variables related to a concrete phase
We injected the following variables at the end of the phase execution, which are related to the phase.
These variables are the following:

|NAME                            | Type   | Description |
|--------------------------------|--------|-------------|
|**SAFETY_PHASE_NAME_RESULT**    |String  |Represents the result of the phase as a text. The result of the phase is the worse of the job results triggered.|
|**SAFETY_PHASE_NAME_SUCCESSFUL**|Numeric |A Number variable. The number of triggered jobs in **SAFETY_PHASE_NAME** with **UNSTABLE** or **SUCCESS** results.|
|**SAFETY_PHASE_NAME_STABLE**    |Numeric |The number of triggered jobs in **SAFETY_PHASE_NAME** with **SUCCESS** results.|
|**SAFETY_PHASE_NAME_UNSTABLE**  |Numeric |The number of triggered jobs in **SAFETY_PHASE_NAME** with **UNSTABLE** results.|
|**SAFETY_PHASE_NAME_FAILED**    |Numeric |The number of triggered jobs in **SAFETY_PHASE_NAME** with **FAILED** results.|
|**SAFETY_PHASE_NAME_ABORTED**   |Numeric |The number of triggered jobs in **SAFETY_PHASE_NAME** with **ABORTED** results.|
|**SAFETY_PHASE_NAME_SKIPPED**   |Numeric |The number of skipped jobs in **SAFETY_PHASE_NAME**.|

Where **SAFETY_PHASE_NAME** is the safe name of the phase (see [The safe name of a phase](#the-safe-name-of-a-phase)).

## Variables related to the multijob execution
We increase the following variables at the end of the phase execution, which are related to the multijob.
These variables are increased after the phase execution, by adding the similar **PHASE_** variable.

|NAME                            | Type   | Description |
|--------------------------------|--------|-------------|
|**MULTIJOB_SUCCESSFUL**         |Numeric |The number of triggered jobs with **UNSTABLE** or **SUCCESS**. results.|
|**MULTIJOB_STABLE**             |Numeric |The total number of triggered jobs with **SUCCESS** results.|
|**MULTIJOB_UNSTABLE**           |Numeric |The total number of triggered jobs with **UNSTABLE** results.|
|**MULTIJOB_FAILED**             |Numeric |The total number of triggered jobs with **FAILED** results.|
|**MULTIJOB_ABORTED**            |Numeric |The total number of triggered jobs with **ABORTED** results.|
|**MULTIJOB_SKIPPED**            |Numeric |The total number of aborted jobs.|

#Appendix
##The safe name of a phase
The safe name of a phase is a name that can be used at conditions. It is created from the **Phase name**,
converted to uppercase and every character not in **[A-Za-z0-9]** is replaced with an underscore (**_**).

Some examples of this conversion:

|Phase name                   |Safe phase name                   |
|-----------------------------|----------------------------------|
|Phase 1                      |PHASE_1                           |
|My phase: name               |MY_PHASE__NAME                    |
|Build and deploy at server #1|BUILD_AND_DEPLOY_AT_SERVER__1     |
|Too much<>extrange @Chars    |TOO_MUCH\_\_EXTRANGE\_\_CHARS     |

##About StatusJob enumeration
**StatusJob** (```com.tikal.jenkins.plugins.multijob.StatusJob.java```) is a Java enumeration that holds
different values about the status of a job to use it in conditions. This status is evaluated to decide if the job is
buildable, getting one of the following values (the **JOB_IS_BUILDABLE** column shows the value that will be assigned
to the variable according the status of the job):

|Status job                                 |Description                                              |JOB_IS_BUILDABLE|
|-------------------------------------------|------------------------------------------------------------|-------------|
|**IS_DISABLED**                            |The job is disabled.                                        |NO           |
|**IS_DISABLED_AT_PHASECONFIG**             |The job is disabled in the phase.                           |NO           |
|**BUILD_ONLY_IF_SCM_CHANGES_DISABLED**     |The **Build only if SCM changes** feature has been disabled.|YES          |
|**BUILD_ALWAYS_IS_ENABLED**                |The **Build always** feature has been enabled.              |YES          |
|**DOESNT_CONTAINS_LASTBUILD**              |Job doesn't contains last build.                            |YES          |
|**LASTBUILD_RESULT_IS_WORSE_THAN_UNSTABLE**|The result of the last build is worse than unstable.        |YES          |
|**WORKSPACE_IS_EMPTY**                     |The workspace of the job is empty.                          |YES          |
|**CHANGED_SINCE_LAST_BUILD**               |SCM has changed since last build.                           |YES          |
|**NOT_CHANGED_SINCE_LAST_BUILD**           |SCM hasn't changed since last build.                        |NO           |
|**UNKNOWN_STATUS**                         |It has been impossible to determine the status of the job.  |NO           |

##Warnings
You must to keep in mind that:

* If you don't write a condition the **Build Only if SCM Changes** works as previous version.
* If you write a condition and you want the **Build Only If SCM Changes**,
you must include the expression **${JOB_IS_BUILDABLE}** in the condition.

##Types of the variables
In previous tables we has showed a column with the type of the variable. This is useful to use it at conditions.
Here a few tips on their use:

|Type   |How to use                                 |Examples                                            |
|-------|-------------------------------------------|----------------------------------------------------|
|Boolean|You can use it directly.                   |```${VARIABLE} || !${VARIABLE}```                   |
|Numeric|You can use it directly.                   |```${VARIABLE} == 5 || ${VARIABLE} < 27```          |
|String |You must enclosed it between double quotes.|```"${VARIABLE}" == "Hello world!"```               |

#About this new variables
##Why this new variables?
Without them we can not write conditions that includes:
* The **Build Only If SCM Changes** value.
* The number of **ABORTED** jobs in previous phase.
* The number of stable jobs triggered in previous phase.
* The number of stable jobs triggered till now.
* And so on.

##And this variables are useful for... what scenery?
Imagine a hypotetical scenery, we have a multijob project that:
* **Phase 1** builds the following jobs:
    * **job_1** (when scm changes).
    * **job_2** (when scm changes).
    * **job_3** (when scm changes).
* **Phase 2** builds the following jobs:
    * **job_4** every time multijob is built. This job packs artifacts of previous jobs in a zip file.

We want to optimize the condition that triggers the **job_4**: only when scm changes or one of the following conditions
is met (all are equivalent in this scenery):
* Number of (stable + unstable) results of jobs in phase 1 (or in this case of multijob) is greater than 0.
* Number of (stable + unstable) results in multijob build is greater than 0.
* Previous phase result is **UNSTABLE** or **STABLE**.

How do you write the condition for **job_4**? Now is possible, as one of following (**PHASE_1** is the
[safe name](#the-safe-name-of-a-phase) of **Phase 1**):
```
${JOB_IS_BUILDABLE} || ${PHASE_SUCCESSFUL} > 0

${JOB_IS_BUILDABLE} || ${MULTIJOB_SUCCESSFUL} > 0

${JOB_IS_BUILDABLE} || "${PHASE_1_RESULT}" == "UNSTABLE" || "${PHASE_1_RESULT}" == "STABLE"
```
