package com.tikal.jenkins.plugins.multijob.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder.ContinuationCondition;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.tasks.Shell;

/**
 * @author Joachim Nilsson (JocceNilsson)
 */
public class CommitPathTest {

	private static final String I_LIKE_BEER = "I like good beer!";
	private static final String OTHER_JOB_MESSAGE = "Hello world!";
	public static final String BEER_DRINKER = "beerDrinker";
	public static final String OTHER_JOB = "job2";
	public static final String WHISKY_CONNOISSEUR = "whiskyconnoisseur";
	public static final String ISLAY_WHISKY_IS_THE_BEST = "Islay whisky is the best";

	public interface SerializeableAnswer<T> extends Answer<T>, Serializable {
	}

	@Mock
	transient SCMDescriptor scmDescriptor;
	@Mock
	transient SCM scm;
	@Mock
	transient ChangeLogParser parser;
	@Mock
	transient ChangeLogSet set;
	@Mock
	transient ChangeLogSet.Entry entry;
	@Rule
	public transient JenkinsRule jenkinsRule = new JenkinsRule();

	private boolean changeSetEmpty = true;
	private List<String> affectedPaths = Collections.emptyList();

	@Before
	public void setup()
			throws IOException, InterruptedException, SAXException {
		initMocks(this);
		//scm = new MyScm();
		when(scm.getDescriptor()).thenReturn(scmDescriptor);
		when(scm.createChangeLogParser()).thenReturn(parser);
		doAnswer(createChangelog()).when(scm).checkout(any(Run.class), any(Launcher.class), any(FilePath.class), any(TaskListener.class), any(File.class), any(SCMRevisionState.class));
		when(scm.checkout(any(AbstractBuild.class), any(Launcher.class), any(FilePath.class), any(BuildListener.class), any(File.class))).thenCallRealMethod();
		when(parser.parse(any(Run.class), any(RepositoryBrowser.class), any(File.class))).thenReturn(set);
		when(parser.parse(any(AbstractBuild.class), any(File.class))).thenCallRealMethod();
		when(set.isEmptySet()).thenAnswer(getIsEmpty());
		when(set.iterator()).thenAnswer(getIterator());
		when(entry.getMsg()).thenReturn("A commit");
		when(entry.getAuthor()).thenReturn(User.getUnknown());
		when(entry.getAffectedPaths()).thenAnswer(getAffectedPaths());
	}

	private SerializeableAnswer<Iterator<ChangeLogSet.Entry>> getIterator() {
		return new SerializeableAnswer<Iterator<ChangeLogSet.Entry>>() {
			@Override
			public Iterator<ChangeLogSet.Entry> answer(InvocationOnMock invocationOnMock)
					throws Throwable {
				return Arrays.asList(entry).iterator();
			}
		};
	}

	private SerializeableAnswer<Boolean> getIsEmpty() {
		return new SerializeableAnswer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocationOnMock)
					throws Throwable {
				return changeSetEmpty;
			}
		};
	}

	private SerializeableAnswer<List<String>> getAffectedPaths() {
		return new SerializeableAnswer<List<String>>() {
			@Override
			public List<String> answer(InvocationOnMock invocationOnMock)
					throws Throwable {
				return affectedPaths;
			}
		};
	}

	private SerializeableAnswer createChangelog() {
		return new SerializeableAnswer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation)
					throws Throwable {
				File changelogFile = invocation.getArgumentAt(4, File.class);
				if (changelogFile == null) {
					changelogFile = File.createTempFile("changelog", ".xml");
				}
				if (!changelogFile.exists()) {
					changelogFile.createNewFile();
				}
				return null;
			}
		};
	}

	private void mockEmptyChangeSet() {
		changeSetEmpty = true;
		affectedPaths = Collections.emptyList();
	}

	private void mockChangeSet(String... pathnameList)
			throws SAXException, IOException {
		changeSetEmpty = false;
		affectedPaths = Arrays.asList(pathnameList);
	}

	private void setupMultiJobPhase(MultiJobProject multi, FreeStyleProject beerDrinker, FreeStyleProject whiskyConnoisseur, FreeStyleProject otherJob)
			throws IOException {
		multi.setUseCommitPath(true);
		beerDrinker.getBuildersList().add(new Shell("echo "+I_LIKE_BEER));
		otherJob.getBuildersList().add(new Shell("echo "+OTHER_JOB_MESSAGE));
		whiskyConnoisseur.getBuildersList().add(new Shell("echo "+ ISLAY_WHISKY_IS_THE_BEST));
		List<PhaseJobsConfig> configList = new ArrayList<>();
		configList.add(new PhaseJobsConfig(BEER_DRINKER, null, true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "", false, false, true, "beer/.+"));
		configList.add(new PhaseJobsConfig(WHISKY_CONNOISSEUR, null, true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "", false, false, true, "whisky/.+"));
		configList.add(new PhaseJobsConfig(OTHER_JOB, null, true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, false, "", false, false, false, ""));
		multi.getBuildersList().add(new MultiJobBuilder("Phase", configList, ContinuationCondition.SUCCESSFUL, MultiJobBuilder.ExecutionType.PARALLEL));
		multi.setScm(scm);
	}

	@Test
	public void testSkippingJobBasedOnChangelog()
			throws Exception {
		jenkinsRule.jenkins.getInjector().injectMembers(this);
		MultiJobProject multi = jenkinsRule.jenkins.createProject(MultiJobProject.class, "MultiTop");
		FreeStyleProject beerDrinkerJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, BEER_DRINKER);
		FreeStyleProject whiskyConnoisseurJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, WHISKY_CONNOISSEUR);
		FreeStyleProject otherJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, OTHER_JOB);
		setupMultiJobPhase(multi, beerDrinkerJob, whiskyConnoisseurJob, otherJob);
		mockEmptyChangeSet();
		jenkinsRule.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
		mockChangeSet("beer/mikkeller/AmericanDream");
		jenkinsRule.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
		assertLogContains("MultiJob should skip job "+OTHER_JOB, multi, "Commit path: Skipping "+OTHER_JOB+". Project is not affected by the SCM changes in this build.");
		assertLogContains("MultiJob should skip job "+WHISKY_CONNOISSEUR, multi, "Commit path: Skipping "+WHISKY_CONNOISSEUR+". Project is not affected by the SCM changes in this build.");
		assertLogContains("MultiJob should run job", multi, "Starting build job "+BEER_DRINKER+".");
		assertLogContains("shell task writes message to log", beerDrinkerJob, I_LIKE_BEER);
		assertNotNull("First build should be ok", otherJob.getLastBuild());
		assertNull("Expecting only one build of other job",otherJob.getLastBuild().getPreviousBuild());
	}

	@Test
	public void testSkippingJobBasedOnLargerChangelog()
			throws Exception {
		jenkinsRule.jenkins.getInjector().injectMembers(this);
		MultiJobProject multi = jenkinsRule.jenkins.createProject(MultiJobProject.class, "MultiTop");
		FreeStyleProject beerDrinkerJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, BEER_DRINKER);
		FreeStyleProject whiskyConnoisseurJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, WHISKY_CONNOISSEUR);
		FreeStyleProject otherJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, OTHER_JOB);
		setupMultiJobPhase(multi, beerDrinkerJob, whiskyConnoisseurJob, otherJob);
		mockEmptyChangeSet();
		jenkinsRule.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
		mockChangeSet("beer/Mikkeller/AmericanDream", "beer/Omnipollo/Anagram", "whisky/Bruichladdich/Port_Charlotte", "whisky/Bruichladdich/Octomore");
		jenkinsRule.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
		assertLogContains("MultiJob should skip job "+OTHER_JOB, multi, "Commit path: Skipping "+OTHER_JOB+". Project is not affected by the SCM changes in this build.");
		assertLogContains("MultiJob should run job "+BEER_DRINKER, multi, "Starting build job "+BEER_DRINKER+".");
		assertLogContains("MultiJob should run job "+WHISKY_CONNOISSEUR, multi, "Starting build job "+WHISKY_CONNOISSEUR+".");
		assertLogContains("shell task writes message to log", beerDrinkerJob, I_LIKE_BEER);
		assertLogContains("shell task writes message to log", whiskyConnoisseurJob, ISLAY_WHISKY_IS_THE_BEST);
		assertNotNull("First build should be ok", otherJob.getLastBuild());
		assertNull("Expecting only one build of other job",otherJob.getLastBuild().getPreviousBuild());
	}

	@Test
	public void testChangelogWithAdditionalCommitShallRunAllJobs()
			throws Exception {
		jenkinsRule.jenkins.getInjector().injectMembers(this);
		MultiJobProject multi = jenkinsRule.jenkins.createProject(MultiJobProject.class, "MultiTop");
		FreeStyleProject beerDrinkerJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, BEER_DRINKER);
		FreeStyleProject whiskyConnoisseur = jenkinsRule.jenkins.createProject(FreeStyleProject.class, WHISKY_CONNOISSEUR);
		FreeStyleProject otherJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, OTHER_JOB);
		setupMultiJobPhase(multi, beerDrinkerJob, whiskyConnoisseur, otherJob);
		mockEmptyChangeSet();
		jenkinsRule.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
		mockChangeSet("beer/Mikkeller/AmericanDream", "whisky/Bruichladdich/Octomore", "food/sushi");
		jenkinsRule.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
		assertLogContains("MultiJob should run job "+BEER_DRINKER, multi, "Starting build job "+BEER_DRINKER+".");
		assertLogContains("MultiJob should run job "+OTHER_JOB, multi, "Starting build job "+OTHER_JOB+".");
		assertLogContains("MultiJob should run job "+WHISKY_CONNOISSEUR, multi, "Starting build job "+WHISKY_CONNOISSEUR+".");
		assertLogContains("shell task writes message to log", beerDrinkerJob, I_LIKE_BEER);
		assertLogContains("shell task writes message to log", otherJob, OTHER_JOB_MESSAGE);
		assertLogContains("shell task writes message to log", whiskyConnoisseur, ISLAY_WHISKY_IS_THE_BEST);
	}

	@Test
	public void testEmptyChangelogShallRunAllJobs()
			throws Exception {
		jenkinsRule.jenkins.getInjector().injectMembers(this);
		MultiJobProject multi = jenkinsRule.jenkins.createProject(MultiJobProject.class, "MultiTop");
		FreeStyleProject beerDrinkerJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, BEER_DRINKER);
		FreeStyleProject whiskyConnoisseur = jenkinsRule.jenkins.createProject(FreeStyleProject.class, WHISKY_CONNOISSEUR);
		FreeStyleProject otherJob = jenkinsRule.jenkins.createProject(FreeStyleProject.class, OTHER_JOB);
		setupMultiJobPhase(multi, beerDrinkerJob, whiskyConnoisseur, otherJob);
		mockEmptyChangeSet();
		jenkinsRule.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
		jenkinsRule.assertBuildStatus(Result.SUCCESS, multi.scheduleBuild2(0, new UserCause()).get());
		assertLogContains("MultiJob should run job "+BEER_DRINKER, multi, "Starting build job "+BEER_DRINKER+".");
		assertLogContains("MultiJob should run job "+OTHER_JOB, multi, "Starting build job "+OTHER_JOB+".");
		assertLogContains("MultiJob should run job "+WHISKY_CONNOISSEUR, multi, "Starting build job "+WHISKY_CONNOISSEUR+".");
		assertLogContains("shell task writes message to log", beerDrinkerJob, I_LIKE_BEER);
		assertLogContains("shell task writes message to log", otherJob, OTHER_JOB_MESSAGE);
		assertLogContains("shell task writes message to log", whiskyConnoisseur, ISLAY_WHISKY_IS_THE_BEST);
	}

	private void assertLogContains(String message, Project project, String expected)
			throws IOException {
		List<String> multiLog = project.getLastBuild().getLog(30);
		if (!multiLog.contains(expected)) {
			for (String line : multiLog) {
				message += "\n" + line;
			}
			Assert.fail(message);
		}
	}
}
