package jiyuiydi.infra.ui.opengl;

import java.util.ArrayList;
import java.util.List;

import jiyuiydi.infra.Node;
import jiyuiydi.util.Observer;
import jiyuiydi.util.Observer.Observable;
import jiyuiydi.util.Observer.UpdateMessage;

class Selection extends Observable implements Observer {

	static class Path {
		List<Integer> toParent = new ArrayList<>();
		int start, end;

		Path(Selection s) { this(s.parent, s.start, s.end); }
		Path(ParentOfView target, int startIndex) { this(target, startIndex, startIndex); }

		Path(ParentOfView target, int startIndex, int endIndex) {
			start = startIndex;
			end = endIndex;
			if(target instanceof View) {
				View temp = (View) target;
				while(temp.parent instanceof View) {
					toParent.add(0, ((View) temp.parent).indexOf(temp));
					temp = (View) temp.parent;
				}
			}
		}

		void select(Editor context) {
			View target = context.rootView;
			for(int index : toParent)
				target = target.get(index);
			context.setSelection(target.get(start));
			context.addSelectionRange(end - start);
		}
	}

	ParentOfView parent;
	int start, end;

	int low () { return Math.min(start, end); }
	int high() { return Math.max(start, end); }

	int count() {
		if(parent == null) return 0;
		return Math.abs(start - end) + 1;
	}

	ArrayList<View> getViews() {
		ArrayList<View> l = new ArrayList<View>(count());
		if(parent == null) return l;
		try {
			for(int i = low(); i <= high(); ++i)
				l.add(parent.get(i));
		} catch(IndexOutOfBoundsException e) {}
		return l;
	}

	void deselect() {
		if(parent == null) return;
		parent.removeObserver(this);
		for(View s : getViews()) {
			s.setHighlight(false);
		}
		parent = null;
	}

	void select(View v) {
		deselect();
		if(v == null) return;
		start = end = v.parent.indexOf(v);
		if(start == Node.INDEX_ERROR) return;
		parent = v.parent;
		if(parent instanceof View_Box)
			((View_Box) parent).previouslySelectedIndex = start;
		parent.addObserver(this);
		v.setHighlight(true);
		notifyObservers();
	}

	void select(View v, int range) {
		select(v);
		addSelectionRange(range);
	}

	View getStart() { return parent.get(start); }
	View getEnd() { return parent.get(end); }
	int getRange() { return end - start; }

	void addSelectionRange(int addition) {
		if(addition == 0) return;
		int newLoIndex = Math.min(start, end + addition);
		int newHiIndex = Math.max(start, end + addition);

		newLoIndex = Math.max(newLoIndex, 0);
		newHiIndex = Math.min(newHiIndex, parent.count() - 1);

		int oldLoIndex = low();
		int oldHiIndex = high();

		if(newLoIndex < oldLoIndex) // select more (left)
			for(int i = newLoIndex; i < oldLoIndex; ++i)
				addSelection(i);
		if(newHiIndex > oldHiIndex) // select more (right)
			for(int i = oldHiIndex + 1; i <= newHiIndex; ++i)
				addSelection(i);
		if(newLoIndex > oldLoIndex) // select less (left)
			for(int i = oldLoIndex; i < newLoIndex; ++i)
				removeSelection(i);
		if(newHiIndex < oldHiIndex) // select less (right)
			for(int i = newHiIndex + 1; i <= oldHiIndex; ++i)
				removeSelection(i);

		end += addition;
		clamp();
		notifyObservers();
	}

	void clamp() {
		start = Math.max(0, Math.min(parent.count() - 1, start));
		end   = Math.max(0, Math.min(parent.count() - 1, end  ));
	}

	private void addSelection(int i) {
		parent.get(i).setHighlight(true);
	}

	private void removeSelection(int i) {
		parent.get(i).setHighlight(false);
	}

	public boolean isOnlySelection(View v) {
		return (count() == 1 && getEnd() == v);
	}

	// Observer //////////////////////////////////////////////////////////

	@Override public void update(UpdateMessage msg, Observable subject) {
		if(msg instanceof UpdateMessage.Replaced) {
			@SuppressWarnings("unchecked")
			UpdateMessage.Replaced<View> m = (UpdateMessage.Replaced<View>) msg;
			if(m.index >= low() && m.index <= high()) {
				m.was.setHighlight(false);
				m.is.setHighlight(true);
			}
			notifyObservers();
		}
		if(msg instanceof UpdateMessage.Removed) {
			@SuppressWarnings("unchecked")
			UpdateMessage.Removed<View> m = (UpdateMessage.Removed<View>) msg;
			m.item.setHighlight(false);
			if(m.index < low()) {
				--start; --end;
			}
			clamp();
			notifyObservers();
		}
		if(msg instanceof UpdateMessage.Inserted) {
			@SuppressWarnings("unchecked")
			UpdateMessage.Inserted<View> m = (UpdateMessage.Inserted<View>) msg;
			if(m.index > low() && m.index < high())
				m.item.setHighlight(true);
			if(m.index <= low()) {
				++start; ++end;
			} else if(m.index <= high()) {
				if(start < end) ++end;
				else ++start;
			}
			notifyObservers();
		}
	}

}
