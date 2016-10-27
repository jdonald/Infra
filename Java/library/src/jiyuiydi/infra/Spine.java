package jiyuiydi.infra;

import java.util.Stack;

public class Spine {

	protected Spine          nextSegment;
	protected Stack<Box>     parents = new Stack<>();
	protected Stack<Integer> indexes = new Stack<>();
	protected Node           focus;
	protected boolean        hasSetRootPatchLocations;

	// Spine /////////////////////////////////////////////////////////////

	public Spine(Node n) { focus = n; }

	public Spine(Spine parent) {
		nextSegment = parent;
		focus = parent.focus;
	}

	public int getDepth() { return indexes.size() + (nextSegment == null ? 0 : nextSegment.getDepth()); }

	public Spine fork() { // Make THIS and a NEW spine extend a STATIC third. This allows list copying to be lazy (only if this or the fork goes UP).
		Spine f = new Spine(focus);
		f.nextSegment = nextSegment;
		f.hasSetRootPatchLocations = hasSetRootPatchLocations;

		Stack<Box> emptyParents = f.parents;		// swap data with an empty one
		Stack<Integer> emptyIndexes = f.indexes;
		f.parents = parents;
		f.indexes = indexes;
		parents = emptyParents;
		indexes = emptyIndexes;

		nextSegment = f;		// THIS one extends the old data
		return new Spine(f);	// a NEW one extends the old data
	}

	public void prepend(Spine s) {
		if(nextSegment == null) nextSegment = s;
		else                    nextSegment.prepend(s);
	}

	public void syncWith(Spine s) {
		nextSegment = s.nextSegment;
		parents.clear(); parents.addAll(s.parents);
		indexes.clear(); indexes.addAll(s.indexes);
		focus = s.focus;
	}

	private void updateRootPatchLocations() {
		if(hasSetRootPatchLocations) return;
		if(nextSegment == null)
			new Spine(root()).setPatchLocations();
		else
			nextSegment.updateRootPatchLocations();
		hasSetRootPatchLocations = true;
	}

	// read //////////////////////////////////////////////////////////////

	public Box getParent() {
		if(!parents.isEmpty()) return parents.peek();
		return nextSegment == null ? null : nextSegment.getParent();
	}

	public int indexInParent() {
		if(indexes.isEmpty()) {
			if(nextSegment != null) return nextSegment.indexInParent();
			return Node.INDEX_ERROR;
		}
		return indexes.peek();
	}

	public Node root() {
		if(nextSegment != null) return nextSegment.root();
		if(parents.isEmpty()) return focus;
		return parents.firstElement();
	}

	public int numChildren() { return focus.count(); }

	public Node get() { return focus; }

	public Node get(int index) { return focus.get(index); }

	// write /////////////////////////////////////////////////////////////

	public void remove (int index        ) { ((Box) focus).remove (index   ); }
	public void insert (int index, Node n) { ((Box) focus).insert (index, n); }
	public void replace(int index, Node n) { ((Box) focus).replace(index, n); }

	public void replace(Node n) {
		Box p = getParent();
		if(p != null) p.replace(indexInParent(), n);
		focus = n;
	}

	public void add(Node n) { insert(focus.count(), n); }

	// navigation ////////////////////////////////////////////////////////

	public boolean evaluatedOnce() {
		if(!(focus instanceof Patch)) return false;
		updateRootPatchLocations();
		((Patch) focus).setHome(this); // may fork this Spine
		final Node was = focus;
		focus = ((Patch) focus).getResult();
		return focus != was;
	}

	public Node reduced(boolean childRecursive) {
		if(focus instanceof Patch) {
			updateRootPatchLocations();
			((Patch) focus).setHome(this); // may fork this Spine
			focus = ((Patch) focus).getReduced();
		}
		if(childRecursive)
			preReduceChildern();
		return focus;
	}

	public void preReduceChildern() {
		if(numChildren() < 1) return;
		Spine f = fork();
		f.down(0);
		do f.reduced(true); while(f.sibling(1));
	}

	public Node evaluated() {
		if(focus instanceof Patch) {
			updateRootPatchLocations();
			((Patch) focus).setHome(this); // may fork this Spine
			focus = ((Patch) focus).getReduced();
			if(focus instanceof Patch)
				focus = ((Patch) focus).getResult(); // will be an ERROR symbol (or selfReferenceNode)
		}
		return focus;
	}

	public boolean up() {
		while(parents.isEmpty()) {
			if(nextSegment == null) return false;
			parents.addAll(nextSegment.parents);
			indexes.addAll(nextSegment.indexes);
			nextSegment = nextSegment.nextSegment;
		}
		focus = parents.pop();
		if(indexes.pop() < 0) // coming from META or VIRTUAL
			focus = focus.get(0); // unbox it. it may have been a leaf
		return true;
	}

	public boolean down(int index) {
		if(index >= focus.count()) return false;
		parents.push((Box) focus);
		indexes.push(index);
		focus = focus.get(index);
		return true;
	}

	public void meta(Node lang) {
		parents.push(new Box(focus)); // box it in case focus was a leaf
		indexes.push(Node.INDEX_METADATA);
		focus = focus.getMetadataChannel(lang);
	}

	public void downVirtual(Node n) {
		parents.push(new Box(focus)); // box it in case focus was a leaf
		indexes.push(Node.INDEX_VIRTUALCHILD);
		focus = n;
	}

	public boolean sibling(int relativeIndex) {
		Box parent = getParent();
		if(parent == null) return false;
		int newIndex = indexInParent() + relativeIndex;
		if(newIndex < 0 || newIndex >= parent.count()) return false;
		if(indexes.isEmpty()) { // all parents are in NextSegment. maybe we forked recently?
			up(); down(newIndex);
		} else {
			indexes.set(indexes.size()-1, newIndex);
			focus = parent.get(newIndex);
		}
		return true;
	}

	public boolean up(int times) {
		for(int i = 0; i < times; ++i)
			if(!up()) return false;
		return true;
	}

	public boolean down(int... indexes) {
		if(indexes.length == 0) return down(0);
		for(int index : indexes)
			if(!down(index)) return false;
		return true;
	}

	public void upToRoot() { while(up()); }

	// utility ///////////////////////////////////////////////////////////

	protected void setPatchLocations() {
		if(focus instanceof Patch)
			((Patch) focus).setHome(this);
		else {
			int count = numChildren();
			if(count > 0) {
				down(0);
				for(int i = 0; i < count; ++i) {
					setPatchLocations();
					sibling(1);
				}
				up();
			}
		}
	}

	// Object ////////////////////////////////////////////////////////////

	@Override public String toString() {
		Stack<Integer> path = new Stack<>();
		Spine s = this;
		while(s != null) { path.addAll(0, s.indexes); s = s.nextSegment; }
		if(path.isEmpty()) return focus.toString();
		StringBuilder sb = new StringBuilder();
		for(int i : path)
			sb.append(i + "->");
		sb.append("> ");
		sb.append(focus.toString());
		return sb.toString();
	}

}
