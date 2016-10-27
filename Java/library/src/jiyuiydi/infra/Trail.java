package jiyuiydi.infra;

import java.util.Stack;

public class Trail {

	public final Stack<Integer> parentHops = new Stack<>();
	public final Stack<Integer> childHops  = new Stack<>();

	public boolean isEmpty() { return childHops.isEmpty() && parentHops.isEmpty(); }
	public boolean hasParentHops() { return !parentHops.isEmpty(); }
	public boolean hasChildHops() { return !childHops.isEmpty(); }

	public int steps() { return parentHops.size() + childHops.size(); }

	public void clear() { parentHops.clear(); childHops.clear(); }

	public void up(int fromChildIndex) {
		if(childHops.isEmpty())
			parentHops.push(fromChildIndex);
		else
			childHops.pop();
	}

	public void down(int toChildIndex) {
		if(!parentHops.isEmpty() && parentHops.lastElement() == toChildIndex)
			parentHops.pop();
		else
			childHops.push(toChildIndex);
	}

	public void rewindAndReset(Spine t) {
		t.up(childHops.size());
		childHops.clear();
		while(!parentHops.isEmpty()) { t.down(parentHops.pop()); }
	}

	public void rewind(Spine t) {
		t.up(childHops.size());
		for(int i = parentHops.size() - 1; i >= 0; --i)
			t.down(parentHops.get(i));
	}

	public void follow(Spine t) {
		t.up(parentHops.size());
		for(int i : childHops ) t.down(i);
	}

	public boolean searchFor(Node goal, Node in) {
		if(goal == in) return true;
		for(int i = 0; i < in.count(); ++i) {
			down(i);
			if(searchFor(goal, in.get(i)))
				return true;
			up(i);
		}
		return false;
	}

	// Object ////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(parentHops.size() > 0)
			sb.append("up(" + parentHops.size() + ") ");
		if(childHops.size() > 0) {
			sb.append("down(");
			for(int i : childHops) sb.append(i + " ");
			sb.delete(sb.length()-1, sb.length());
			sb.append(')');
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Trail)) return false;
		Trail o = (Trail) obj;
		return parentHops.equals(o.parentHops) && childHops.equals(o.childHops);
	}

	// utility ///////////////////////////////////////////////////////////

	static public Trail trailFromTo(Spine from, Spine to) {
		Spine p1 = from.fork();
		int depth1 = p1.getDepth();
		int[] path1 = new int[depth1];
		for(int i = 0; i < depth1; ++i) {
			path1[i] = p1.indexInParent();
			p1.up();
		}

		Spine p2 = to.fork();
		int depth2 = p2.getDepth();
		int[] path2 = new int[depth2];
		for(int i = 0; i < depth2; ++i) {
			path2[i] = p2.indexInParent();
			p2.up();
		}

		if(p1.focus != p2.focus) return null; // no common ancestor :(

		int diverge = 1, commonDepth = Math.min(depth1, depth2);
		while(diverge <= commonDepth &&
				path1[depth1 - diverge] == path2[depth2 - diverge])
			++diverge;

		Trail t = new Trail();
		for(int i = 0; i <= depth1 - diverge; ++i)
			t.up(path1[i]);
		for(int i = diverge; i <= depth2; ++i)
			t.down(path2[depth2 - i]);
		return t;
	}

}
