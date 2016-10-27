package jiyuiydi.infra;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Stack;

public class SpineOverlay extends Spine {

	/* Spine member fields:
		Spine          nextSegment
		Stack<Box>     parents
		Stack<Integer> indexes
		Node           focus
		boolean        hasSetRootPatchLocations
	*/

	protected Spine root, leaf;            // root = mark top of overlay; leaf = mark where overlay was exited;
	protected OverlayNode map;             // memory of where overlay has grown
	protected Trail fromMap = new Trail(); // if(hasParentHops) path from root; if(hasChildHops) path from leaf;

	public SpineOverlay(Node n) { super(n); }

	public SpineOverlay(Spine s) { super(s.focus); syncWith(s); }

	public Node finalizeSubtree() {
		if(fromMap.hasParentHops()) // more cloning is only needed if focus is above the overlay root
			expandOverlay();
		return focus;
	}

	public Spine forkForNonDownwardNavigationOnly() { return super.fork(); }

	@Override public Spine fork() {
		// TODO: expand overlay to root
		return null;
	}

	// read //////////////////////////////////////////////////////////////

	// write /////////////////////////////////////////////////////////////

	@Override public void remove(int index) {
		expandOverlay();
		map.remove(index);
		super.remove(index);
	}

	@Override
	public void insert(int index, Node n) {
		expandOverlay();
		map.insert(index);
		super.insert(index, n.shallowCopy());
	}

	@Override public void replace(int index, Node n) {
		expandOverlay();
		super.replace(index, n.shallowCopy());
		map.set(index);
	}

	@Override public void replace(Node n) {
		Box p = getParent();
		if(p == null) {
			focus = n;
			root = null;
			leaf = null;
			map = null;
			fromMap.clear();
			return;
		}
		int iip = indexInParent();
		up();
		replace(iip, n);
		down(iip);
	}

	// navigation ////////////////////////////////////////////////////////

	@Override public boolean up() {
		Node was = focus;
		int iip = indexInParent();
		if(!super.up()) return false;
		if(map != null) {
			if(!fromMap.isEmpty() || was == root.focus) // outside or just left overlay
				fromMap.up(iip);
			else
				map = map.parent; // move map up
		}
		return true;
	}

	@Override public boolean down(int index) {
		if(map != null) {
			if(fromMap.isEmpty()) { // inside overlay
				OverlayNode on = map.get(index);
				if(on == null) { // leaving overlay
					leaf = super.fork(); // remember where we left from
					fromMap.down(index);
				}
				else map = on; // move map down
			} else { // outside overlay
				fromMap.down(index);
				if(fromMap.isEmpty()) // just entered overlay
					focus = root.focus;
			}
		}
		return super.down(index);
	}

	// utility ///////////////////////////////////////////////////////////

	private void expandOverlay() {
		if(map == null) {
			focus = focus.shallowCopy();
			root = super.fork();
			map = new OverlayNode(null);
		} else {
			if(fromMap.hasParentHops()) { // outside top of overlay
				for(int iip : fromMap.parentHops) { // move versionRoot up to apex
					Node oldRoot = root.focus;
					root.up();
					root.focus = root.focus.shallowCopy();
					((Box) root.focus).replace(iip, oldRoot);
					map = new OverlayNode(iip, map); // move map 'up'
				}
				fromMap.parentHops.clear();
				syncWith(root); // move focus to versionRoot; prepare to walk it back to where focus was
				leaf = root.fork(); // now the exit point is the root; prepare to use leaf to walk down
			}

			if(fromMap.hasChildHops()) {
				for(int i : fromMap.childHops) {
					leaf.replace(i, leaf.focus.get(i).shallowCopy());
					map = map.set(i); // expand and move map 'down'
					leaf.down(i);
				}
				fromMap.childHops.clear();
				focus = leaf.focus;
			}
		}
	}

	private String toString(Node n, ArrayDeque<Integer> pathToOverlay) {
		if(pathToOverlay.isEmpty()) return root.focus.toString();
		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		int pathI = pathToOverlay.peekLast();
		for(int i = 0; i < n.count(); ++i) {
			Node c = n.get(i);
			if(i == pathI) {
				pathToOverlay.removeLast();
				sb.append(toString(c, pathToOverlay));
			} else sb.append(c.toString());
			sb.append(' ');
		}
		if(n.count() > 0) sb.delete(sb.length() - 1, sb.length());
		sb.append(']');
		return sb.toString();
	}

	static class OverlayNode {
		final OverlayNode parent;
		final ArrayList<OverlayNode> children = new ArrayList<>();

		OverlayNode(OverlayNode parent) { this.parent = parent; }
		OverlayNode(int index, OverlayNode child) {
			parent = null;
			while(children.size() < index) children.add(null);
			children.add(child);
		}

		OverlayNode get(int index) { return index >= children.size() ? null : children.get(index); }
		void     insert(int index) { while(children.size() <  index) children.add(null); children.add(index, new OverlayNode(this)); }
		OverlayNode set(int index) { while(children.size() <= index) children.add(null); children.set(index, new OverlayNode(this)); return children.get(index); }
		void     remove(int index) { if(children.size() > index) children.remove(index); }
	}

	// Object ////////////////////////////////////////////////////////////

	@Override public String toString() {
		if(fromMap.hasChildHops() || !fromMap.hasParentHops()) return focus.toString();
		else return toString(focus, new ArrayDeque<Integer>(fromMap.parentHops));
	}

}
