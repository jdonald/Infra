package jiyuiydi.util;

import java.util.ArrayList;

public interface Observer {

	public void update(UpdateMessage msg, Observable subject);

	// Utilities /////////////////////////////////////////////////////////

	static public abstract class UpdateMessage {

		static public final Initialize initializeMessage = new Initialize();
		static public final Changed    changedMessage    = new Changed();

		static public final class Initialize  extends UpdateMessage {}
		static public final class Changed     extends UpdateMessage {}
		static public final class Invalidated extends UpdateMessage {}

		static public final class Inserted<T> extends UpdateMessage {
			public int index;
			public T item;
			public Inserted(int index, T item) { this.index = index; this.item = item; }
		}

		static public final class Removed<T> extends UpdateMessage {
			public int index;
			public T item;
			public Removed(int index, T item) { this.index = index; this.item = item; }
		}

		static public final class Replaced<T> extends UpdateMessage {
			public int index;
			public T was, is;
			public Replaced(int index, T was, T is) { this.index = index; this.was = was; this.is = is; }
		}

	}

	static public class Observable {
		final ArrayList<Observer> obs = new ArrayList<>();

		public int countObservers() { return obs.size(); }
		public ArrayList<Observer> getObservers() { return obs; }

		public void addObserver   (Observer o) { obs.add   (o); o.update(new UpdateMessage.Initialize(), this); }
		public void removeObserver(Observer o) { obs.remove(o); /*o.update(new UpdateMessage.Goodbye(), this);*/ }
		public void clearObservers() { obs.clear(); }

		public     void notifyObservers()                  { notifyObservers(new UpdateMessage.Changed    (           )); }
		public <T> void notifyInsertion(int index, T item) { notifyObservers(new UpdateMessage.Inserted<T>(index, item)); }
		public <T> void notifyRemoval  (int index, T item) { notifyObservers(new UpdateMessage.Removed <T>(index, item)); }

		public <T> void notifyReplacement(int index, T was, T is) { notifyObservers(new UpdateMessage.Replaced<T>(index, was, is)); }

		public     void notifyObservers(UpdateMessage msg) {
			ArrayList<Observer> copy = new ArrayList<>(obs); // clone list because updates may result in the addition or removal of observers
			for(Observer o : copy)
				o.update(msg, this);
		}
	}

}
