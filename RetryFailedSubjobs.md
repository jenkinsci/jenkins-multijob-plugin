# Retrying failed subjobs #

The MultiJob plugin provides a retry functionality for subjobs.

1. On the Jenkins master server: store a text file with the content from the Jenkins output to parse, e.g. in `/etc/jenkins/retryrules/myrule`. If the defined text has been found and the subjob's status is `UNSTABLE`: restart that subjob.

  * Example: Retry when a subjob is failed, but not unstable (pre plugin version 1.25 behaviour):

    ```Finished: FAILURE```

  * Example: Retry when some action in the job fails due to an exception (in this case some sort of timeout):

    ```java.util.concurrent.TimeoutException```

  * Example: Retry anyway when the status is `FAILURE`, regardless of output:

    ```.*```

2. Go to Jenkins -> *Manage Jenkins* -> *Configure System* -> *MultiJob Retry Rules*
  * Add a new *Parsing Rule*
  * Pick a descriptive *Name*, e.g. `myrule`
  * Point *Parsing Rules File* to the file on the Jenkins master, e.g. `/etc/jenkins/retryrules/myrule`
  * *Save* the configuration

3. Go to your MultiJob Jenkins job -> *MultiJob Phase* -> *Phase Jobs* -> *[YOUR_SUBJOB]*
  * Tick *Enable Retry*
  * Select the appropriate strategy, e.g. `myrule`
  * Set the amount of *retries*. Note that 3 retries means that a job will be executed at most 4 times.

4. In the *Console log* of your MultiJob  you can see the retries, e.g:
  ```
  Finished Build : #1 of Job : Y with status : FAILURE
  Scanning failed job console output using parsing rule file /etc/jenkins/retryrules/myrule.
  Known failure detected, retrying this build. Try 1 of 2.

  Finished Build : #2 of Job : Y with status : FAILURE
  Scanning failed job console output using parsing rule file /etc/jenkins/retryrules/myrule.
  Known failure detected, retrying this build. Try 2 of 2.

  Finished Build : #3 of Job : Y with status : FAILURE
  Known failure detected, max retries (2) exceeded.

  Build step 'MultiJob Phase' marked build as failure
  ```
