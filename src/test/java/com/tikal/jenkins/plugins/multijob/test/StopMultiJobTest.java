package com.tikal.jenkins.plugins.multijob.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder.ContinuationCondition;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig.KillPhaseOnJobResultCondition;
import com.tikal.jenkins.plugins.multijob.test.testutils.WaitForJobsJenkinsRule;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.model.Cause.UserIdCause;
import hudson.model.Queue.Item;
import hudson.model.queue.QueueTaskFuture;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.slaves.DumbSlave;

public class StopMultiJobTest {

	@Rule
	public transient WaitForJobsJenkinsRule jenkinsRule = new WaitForJobsJenkinsRule();
	@Mock
	@SuppressWarnings({ "rawtypes" })
	private transient SCMDescriptor scmDescriptor;
	@Mock
	private transient SCM scm;
	@Mock
	private transient ChangeLogParser parser;
	@Mock
	@SuppressWarnings({ "rawtypes" })
	private transient ChangeLogSet set;
	@Mock
	private transient ChangeLogSet.Entry entry;

	public interface SerializeableAnswer<T> extends Answer<T>, Serializable {
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setup()
			throws IOException, InterruptedException, SAXException {
		initMocks(this);
		when(scm.getDescriptor()).thenReturn(scmDescriptor);
		when(scm.createChangeLogParser()).thenReturn(parser);
		doAnswer(createChangelog()).when(scm).checkout(any(Run.class), any(Launcher.class), any(FilePath.class), any(TaskListener.class), any(File.class), any(SCMRevisionState.class));
		when(scm.checkout(any(AbstractBuild.class), any(Launcher.class), any(FilePath.class), any(BuildListener.class), any(File.class))).thenCallRealMethod();
		when(parser.parse(any(Run.class), any(RepositoryBrowser.class), any(File.class))).thenReturn(set);
		when(parser.parse(any(AbstractBuild.class), any(File.class))).thenCallRealMethod();
		when(set.isEmptySet()).thenReturn(true);
		when(set.iterator()).thenAnswer(getIterator());
		when(entry.getMsg()).thenReturn("A commit");
		when(entry.getAuthor()).thenReturn(User.getUnknown());
	}

	@Test
	public void testStopMultiJobAlsoClearsSubJobsFromQueue()
			throws Exception {
		MultiJobProject multi = jenkinsRule.jenkins.createProject(MultiJobProject.class, "MultiTop");
		FreeStyleProject project1 = jenkinsRule.jenkins.createProject(FreeStyleProject.class, "Project1");
		FreeStyleProject project2 = jenkinsRule.jenkins.createProject(FreeStyleProject.class, "Project2");
		DumbSlave offlineNode = jenkinsRule.createSlave();
		project2.setAssignedNode(offlineNode);
		offlineNode.getComputer().setTemporarilyOffline(true, null); // Project2 can't start
		setupMultiJobPhase(multi, project1, project2);
		QueueTaskFuture<MultiJobBuild> startedJob = multi.scheduleBuild2(0, new UserIdCause());
		startedJob.waitForStart();
		assertFalse(multi.getBuilds().isEmpty());
		MultiJobBuild topProjectLastBuild = multi.getBuilds().getLastBuild();
		assertTrue(topProjectLastBuild.isBuilding());
		jenkinsRule.waitForAnyBuildStart(15, TimeUnit.SECONDS); // Top job
		jenkinsRule.waitForAnyBuildStart(15, TimeUnit.SECONDS); // Project1 build start
		Queue queue = jenkinsRule.jenkins.getQueue();
		Item[] items = queue.getItems();
		assertEquals(1, items.length);
		Item project2Job = items[0];
		assertEquals("slave0 is offline", project2Job.getWhy());
		jenkinsRule.waitForAnyBuildCompletion(10, TimeUnit.SECONDS); // Project1 build complete
		topProjectLastBuild.doStop();
		jenkinsRule.waitForAnyBuildCompletion(25, TimeUnit.SECONDS);
		assertFalse(topProjectLastBuild.isBuilding());
		assertEquals(Result.ABORTED, topProjectLastBuild.getResult());
		assertEquals(0, queue.getItems().length);
	}

	private void setupMultiJobPhase(MultiJobProject multi, FreeStyleProject... projects)
			throws IOException {
		List<PhaseJobsConfig> configList = new ArrayList<>();
		for (FreeStyleProject project : projects) {
			configList.add(new PhaseJobsConfig(project.getName(), null, null, true, null, KillPhaseOnJobResultCondition.NEVER, false, false, "", 0, false, true, "", false, false));
		}
		multi.getBuildersList().add(new MultiJobBuilder("Phase", configList, ContinuationCondition.SUCCESSFUL, MultiJobBuilder.ExecutionType.PARALLEL));
		multi.setScm(scm);
	}

	private SerializeableAnswer<Iterator<ChangeLogSet.Entry>> getIterator() {
		return new SerializeableAnswer<Iterator<ChangeLogSet.Entry>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<ChangeLogSet.Entry> answer(InvocationOnMock invocationOnMock)
					throws Throwable {
				return Arrays.asList(entry).iterator();
			}
		};
	}

	private SerializeableAnswer<Void> createChangelog() {
		return new SerializeableAnswer<Void>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Void answer(InvocationOnMock invocation)
					throws Throwable {
				File changelogFile = invocation.getArgument(4);
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
}
