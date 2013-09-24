package com.tikal.jenkins.plugins.multijob.scm;

import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;

import java.io.IOException;
import java.net.URL;

public class MultiSCMRepositoryBrowser extends RepositoryBrowser<ChangeLogSet.Entry> {

	private static final long serialVersionUID = 1L;

	@Override
	public URL getChangeSetLink(ChangeLogSet.Entry changeSet) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
