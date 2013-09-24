package com.tikal.jenkins.plugins.multijob.scm;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.util.*;

public class MultiSCMChangeLogSet extends ChangeLogSet<Entry> {

	private final HashMap<String, ChangeLogSetWrapper> changes;
    private final Set<String> kinds;
	
	protected MultiSCMChangeLogSet(AbstractBuild<?, ?> build) {
		super(build);
		changes = new HashMap<String, ChangeLogSetWrapper>();
        kinds = new HashSet<String>();
	}

	public static class ChangeLogSetWrapper {
		private AbstractBuild build;
		private List<Entry> logs;
		private Class clazz;
		private String friendlyName;
		
		public ChangeLogSetWrapper(AbstractBuild build, String friendlyName, Class handler) {
			this.build = build;
			this.logs = new ArrayList<Entry>();
			this.clazz = handler;
			this.friendlyName = friendlyName;
		}
		
		public AbstractBuild getBuild() {
			return build;
		}
		
		public Class getHandlerClass() {
			return clazz;
		}

		public String getName() {
			return friendlyName;
		}
		
		public List<Entry> getLogs() {
			return logs;
		}

		public void addChanges(ChangeLogSet<? extends Entry> cls) {
			for(Entry e : cls)
				logs.add(e);
		}
	}
		
	private static class MultiSCMChangeLogSetIterator implements Iterator<Entry> {

		MultiSCMChangeLogSet set;
		Iterator<String> scmIter = null;
		String currentScm = null;
		Iterator<Entry> logIter = null;

		public MultiSCMChangeLogSetIterator(MultiSCMChangeLogSet set) {
			this.set = set;
			scmIter = set.changes.keySet().iterator();
		}
		
		public boolean hasNext() {
			if(logIter == null || !logIter.hasNext())
				return scmIter.hasNext();
			return true;
		}

		public Entry next() {
			if(logIter == null || !logIter.hasNext()) {
				currentScm = scmIter.next();
				logIter = set.changes.get(currentScm).logs.iterator();
			}			
			return logIter.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("Cannot remove changeset items");
		}		
	}
	
	public Iterator<Entry> iterator() {
		return new MultiSCMChangeLogSetIterator(this);
	}

	@Override
	public boolean isEmptySet() {
		return changes.isEmpty();
	}
	
	public void add(String scmClass, String scmFriendlyName, ChangeLogSet<? extends Entry> cls) {
		if(!cls.isEmptySet()) {
			ChangeLogSetWrapper wrapper = changes.get(scmClass);
			if(wrapper == null) {
				wrapper = new ChangeLogSetWrapper(build, scmFriendlyName, cls.getClass());
				changes.put(scmClass, wrapper);
			}
			wrapper.addChanges(cls);
		}
        kinds.add(cls.getKind());
	}
	
	public Collection<ChangeLogSetWrapper> getChangeLogSetWrappers() {
		return changes.values();
	}

    @Override public String getKind() {
        if (kinds.size() == 1) {
            return kinds.iterator().next();
        } else {
            return "Multi" + kinds;
        }
    }

}
