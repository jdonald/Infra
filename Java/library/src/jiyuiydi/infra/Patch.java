package jiyuiydi.infra;

import java.util.ArrayList;
import java.util.Stack;

import jiyuiydi.util.Observer;

public class Patch extends Box implements Observer {

	static final Patch selfReferenceNode = new Patch();
	static {
		selfReferenceNode.setHome(new Spine(selfReferenceNode)); // prevent home from getting set
		selfReferenceNode.result = selfReferenceNode.reduced = selfReferenceNode;
	}

	static public Node createReference(Spine target, Spine patchHome) {
		Patch p = new Patch();
		Trail t = Trail.trailFromTo(patchHome, target);
		if(t != null) {
			if(t.parentHops.size() == 1 && t.childHops.size() == 1) {
				int diff = t.childHops.get(0) - t.parentHops.get(0);
				if(diff > 0) p.add(new Command(Opcode.RIGHT, diff));
				else         p.add(new Command(Opcode.LEFT, -diff));
			} else {
				p.add(new Command(Opcode.UP, t.parentHops.size()));
				Command downs = new Command(Opcode.DOWN);
				for(int i : t.childHops) downs.add(i);
				p.add(downs);
			}
		}
		return p;
	}

	// instance //////////////////////////////////////////////////////////

	protected Spine home;
	protected Node result, reduced;
	ArrayList<Observable> observations = new ArrayList<>();

	// Execution state
	protected Spine codePtr;
	protected SpineOverlay cursor;
	protected int depthCount;
	protected Stack<Integer> depthFromNestedBlock = new Stack<>();
	protected boolean hasSelection, selectionEval;
	protected int selectionLo, selectionHi;
	//protected Trail fromHome;

	// Patch /////////////////////////////////////////////////////////////

	public Patch() {}

	public Patch(Object... os) { add(os); }

	public boolean hasHome() { return home != null; }

	public Node getResult() {
		if(result != null) return result;
		execute();
		return result;
	}

	public Node getReduced() {
		if(reduced != null) return reduced;
		reduce();
		return reduced;
	}

	public Node getEvaluated() {
		if(reduced == null) reduce();
		if(reduced instanceof Patch) return ((Patch) reduced).getResult();
		return reduced;
	}

	public void setHome(Spine h) {
		if(home != null) return; // Only accept a home if we haven't been claimed yet.
		home = h.fork();
		home.focus = this;
	}

	protected void execute() {
		result = selfReferenceNode;
		init();
		run();

		downToSelection(); // commit 'lazy down'
		result = cursor.finalizeSubtree();
		if(result == this)
			result = selfReferenceNode;
		if(result instanceof Patch) // ended up pointing to another Patch
			((Patch) result).setHome(home); // prepare for recursive eval
	}

	private void reduce() {
		Node temp, cycleProbe = reduced = this; // don't start with 'result' in case it is an ERROR.
		do {
			temp = ((Patch) reduced).getResult();
			if(temp.getClass() == Symbol.Error.class) return;
			if(temp == cycleProbe) { reduced = selfReferenceNode; return; }
			reduced = temp; // advance 'reduced' by one step

			if(!(reduced instanceof Patch)) return;

			temp = ((Patch) reduced).getResult();
			if(temp.getClass() == Symbol.Error.class) return;
			if(temp == cycleProbe) { reduced = selfReferenceNode; return; }
			reduced = temp; // advance 'reduced' by a second step

			if(cycleProbe instanceof Patch) // advance probe by one step for every two taken by 'reduced'
				cycleProbe = ((Patch) cycleProbe).getResult();
		} while(reduced instanceof Patch);
	}

	protected void init() {
		addDependency(this);
		//fromHome = new Trail();
		if(home == null) home = new Spine(this); // if we have no home, run as an island
		codePtr = new Spine(home);
		codePtr.down(0); // enter top code block
		cursor = new SpineOverlay(home);

		depthCount = 0;
		depthFromNestedBlock.clear();
	}

	private void run() {
		while(codePtr.focus != this) {
			if(codePtr.focus instanceof Patch) {
				Patch p = (Patch) codePtr.focus;
				p.setHome(codePtr);
				codePtr.focus = p.getReduced();
			}

			if(codePtr.focus instanceof Keyed) execKeyed();
			else
			if(codePtr.focus instanceof Box) {
				codePtr.down(); // enter nested code block
				//downToSelection(); // can't be lazy when capping root
				//cursor.capRoot(); // make it so that cursor can't navigate up from this point
				depthFromNestedBlock.push(depthCount);
				continue;
			}
			else
			if(codePtr.focus instanceof UTF8) { // shortcut for ID:UTF8
				doID(codePtr.focus);
			}

			if(!codePtr.sibling(1)) { // fetch next instruction
				do {
					codePtr.up(); // no more instructions in the block (goto block itself)
					if(!depthFromNestedBlock.isEmpty()) {
						final int prevDepthCount = depthFromNestedBlock.pop();
						int relDepth = depthCount - prevDepthCount;
						if(hasSelection && relDepth > 0) { hasSelection = false; --relDepth; }
						cursor.up(relDepth);
						depthCount = prevDepthCount;
					} else break;
				} while(!codePtr.sibling(1));
			}
		}
	}

	private void execKeyed() {
		if(codePtr.numChildren() == 0) return;
		codePtr.down(0);
		Node instr = codePtr.evaluated();
		//Node arg = codePtr.sibling(1) ? arg = codePtr.get() : null;
		if(instr instanceof Int32) {
			int code = instr.asInt();
			if(code < Opcode.values().length) {
				switch(Opcode.values()[code]) {
					case UP:      doUp    (); break;
					case DOWN:    doDown  (); break;
					case LEFT:    doLeft  (); break;
					case RIGHT:   doRight (); break;
					case META:                break;

					case ID:      doID    (); break;
					//case UID:     doUID   (); break;

					case WRITE:   doWrite (); break;
					case INSERT:  doInsert(); break;
					//case ADD:     doAdd   (); break;
					case MOVE_BY: doMoveBy(); break;

					case VALUE:   doValue (); break;
					case CLONE:   doClone (); break;

					case DOWN_NOEVAL: doDownNoEval(); break;
					case WRITE_CLONE: doWriteClone(); break;
					case EVAL:    doEval(); break;

					case SAVE:    doSave(); break;
					case CHOICE:  doDown(); break;
					default: break;
				}
			}
		} else { // command name is not an integer
			LibraryPatch lp = Library.core.loadInstance(instr);
			if(lp != null) {
				cursor.downVirtual(lp);
				//fromHome.down(Spine.INDEX_VIRTUALCHILD);
				int numArgs = codePtr.getParent().count() - 1;
				for(int i = 0; numArgs > 0 && i < cursor.numChildren(); ++i) {
					if(cursor.focus.get(i).getClass() == Symbol.Parameter.class) {
						codePtr.sibling(1);
						if(codePtr.focus instanceof Patch)
							((Patch) codePtr.focus).setHome(codePtr);
						cursor.replace(i, codePtr.focus);
						--numArgs;
					}
				}
			} else {
				cursor.replace(new Symbol.Error().with(Metadata.lang_comment, "Library ID not found."));
			}
		}
		codePtr.up();
	}

	private void downToSelection() {
		if(hasSelection) {
			if(selectionEval) cursor.evaluated();
			if(cursor.focus instanceof UTF8) {
				String s = ((UTF8) cursor.focus).get();
				cursor.replace(new UTF8(s.substring(selectionLo, selectionHi + 1)));
			} else {
				if(selectionLo < 0) selectionLo += cursor.numChildren();
				if(!cursor.down(selectionLo))
					throw new RuntimeException(cursor.toString() + " has no child(" + selectionLo + ")!");
			}
			hasSelection = false;

			// Load children of focus
			if(cursor.focus.isUnloaded())
				DelayedLoader.useLoader(cursor.focus);
		}
	}

	private void advanceSelection(int nextIndex, boolean eval) {
		downToSelection();
		setSelection(nextIndex, eval);
	}

	private void setSelection(int index, boolean eval) {
		selectionLo = index;
		selectionEval = eval;
		hasSelection = true;
	}

	// native procedures /////////////////////////////////////////////////

	private void doUp() {
		int times;
		if(codePtr.sibling(1)) times = codePtr.get().asInt();
		else                   times = 1;

		if(depthFromNestedBlock.size() > 0) {
			final int maxUp = depthCount - depthFromNestedBlock.peek();
			if(times > maxUp) times = maxUp;
		}

		if(hasSelection) { hasSelection = false; --times; }
		if(times > 0) cursor.up(times);
	}

	private void doDown() {
		while(codePtr.sibling(1)) {
			final int index = codePtr.get().asInt();
			advanceSelection(index, true);
			++depthCount;
		}
	}

	private void doDownNoEval() {
		while(codePtr.sibling(1)) {
			final int index = codePtr.get().asInt();
			advanceSelection(index, false);
			++depthCount;
		}
	}

	//private void doMeta() { ++depthCount; }

	private void doLeft() {
		int offset;
		if(codePtr.sibling(1)) offset = codePtr.get().asInt();
		else                   offset = 1;

		if(hasSelection) {
			selectionLo -= offset;
		} else {
			setSelection(cursor.indexInParent() - offset, false);
			if(!cursor.up())
				throw new RuntimeException("Cannot go up from " + cursor.toString() + "!");
		}
	}

	private void doRight() {
		int offset;
		if(codePtr.sibling(1)) offset = codePtr.get().asInt();
		else offset = 1;

		if(hasSelection) {
			selectionLo += offset;
		} else {
			setSelection(cursor.indexInParent() + offset, false);
			cursor.up();
		}
	}

	private void doID() {
		if(!codePtr.sibling(1)) return;
		final Node target = codePtr.evaluated();
		doID(target);
	}

	private void doID(Node target) {
		downToSelection(); // flush delayed selection

		// First, search current scope
		for(int i = 0; i < cursor.numChildren(); ++i) {
			Node n = cursor.get(i);
			Keyed ids = n.getMetadataChannel(Metadata.lang_ID);
			if(ids != null) {
				for(int idi = 1; idi < ids.count(); ++idi) {
					Node idVal = ids.get(idi);
					if(idVal.equals(target)) {
						advanceSelection(i, false);
						return;
					}
				}
			}
		}

		Spine probe = cursor.forkForNonDownwardNavigationOnly();
		do {
			Keyed b = probe.focus.getMetadataChannel(Metadata.lang_ID);
			if(b != null) {
				addDependency(b);
				for(int i = 1; i < b.count(); ++i) {
					Node idVal = b.get(i);
					addDependency(idVal);
					if(idVal.equals(target)) {
						cursor.downVirtual(probe.focus);
						return;
					}
				}
			}
		} while(probe.sibling(-1) || probe.up());
		cursor.downVirtual(new Symbol.Error().with(Metadata.lang_patchOutput, new Box("ID", target, "not found")));
	}

	private void doWrite() {
		if(codePtr.sibling(1)) { // first argument
			if(codePtr.focus instanceof Patch)
				((Patch) codePtr.focus).setHome(codePtr);
			if(hasSelection) {
				if(cursor.focus instanceof UTF8) {
					addDependency(cursor.focus);
					StringBuilder sb = new StringBuilder(cursor.focus.toString());
					if(selectionLo < 0) selectionLo += sb.length();
					sb.replace(selectionLo, selectionLo+1, codePtr.focus.toString());
					cursor.replace(new UTF8(sb.toString()));
				} else {
					if(selectionLo < 0) selectionLo += cursor.focus.count();
					cursor.replace(selectionLo, codePtr.focus);
				}
			} else
				cursor.replace(codePtr.focus);
		} else { // no argument (delete)
			if(!hasSelection) {
				setSelection(cursor.indexInParent(), false);
				cursor.up();
			}
			if(cursor.focus instanceof UTF8) {
				addDependency(cursor.focus);
				StringBuilder sb = new StringBuilder(cursor.focus.toString());
				if(selectionLo < 0) selectionLo += sb.length();
				sb.delete(selectionLo, selectionLo+1);
				cursor.replace(new UTF8(sb.toString()));
			} else {
				if(selectionLo < 0) selectionLo += cursor.focus.count();
				cursor.remove(selectionLo);
			}
		}
	}

	private void doInsert() {
		if(codePtr.sibling(1)) {
			if(codePtr.focus instanceof Patch)
				((Patch) codePtr.focus).setHome(codePtr);
			if(!hasSelection) {
				setSelection(cursor.indexInParent(), false);
				cursor.up();
			}
			if(cursor.focus instanceof UTF8) {
				UTF8 s = (UTF8) cursor.focus;
				addDependency(s);
				StringBuilder sb = new StringBuilder(s.value);
				if(selectionLo < 0) selectionLo += sb.length() + 1;
				sb.insert(selectionLo, codePtr.focus.toString());
				cursor.replace(new UTF8(sb.toString()));
			} else {
				if(selectionLo < 0) selectionLo += cursor.focus.count() + 1;
				cursor.insert(selectionLo, codePtr.focus);
			}
		}
	}

	private void doMoveBy() {
		if(codePtr.sibling(1)) {
			int offset = codePtr.get().asInt();
			if(!hasSelection) {
				setSelection(cursor.indexInParent(), false);
				cursor.up();
			}
			if(selectionLo < 0) selectionLo += cursor.focus.count();
			Node n = cursor.get(selectionLo);
			cursor.remove(selectionLo);
			cursor.insert(selectionLo + offset, n);
		}
	}

	private void doWriteClone() {
		if(codePtr.sibling(1))
			cursor.replace(codePtr.focus.deepCopy()); // do not set arg's home
	}

	private void doValue() {
		if(!codePtr.sibling(1)) return;
		if(codePtr.focus instanceof Patch) {
			((Patch) codePtr.focus).setHome(codePtr);
			codePtr.focus = codePtr.focus;
		}
		cursor.downVirtual(codePtr.focus);
		//fromHome.down(Spine.INDEX_VIRTUALCHILD);
	}

	private void doClone() {
//		Node c = cursor.copyOriginal();
//		//doHome();
//		//home.focus = c;
//		if(c instanceof Patch) ((Patch) c).setHome(home);
//		//cursor.replace(c);
//		cursor.downVirtual(c);
	}

	private void doEval() {
//		int times;
//		if(codePtr.sibling(1)) times = codePtr.get().asInt();
//		else                   times = 1;
//
//		for(int i = 0; i < times; ++i) {
//			downSelection();
//			if(cursor.focus instanceof Patch) {
//				//cursor.finalizeSubtree();
//				//cursor.replace(((Patch) cursor.get()).getResult());
//				cursor.;
//			}
//		}
	}

	private void doSave() {
		Box box = cursor.root.getParent();
		int index = cursor.root.indexInParent();
		//cursor.setPatchLocations();
		//.setHome();
		Node with = cursor.root.get();

		downToSelection();
		cursor.downVirtual(new MutationRequest(box, index, with));
	}

	// Object ////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		if(this == selfReferenceNode) return "{}";
		if(reduced != null && reduced != this) return reduced.toString(); // the only cycle here is selfReference (handled above)
		//if(result != null) return result.toString(); // could have cycles!

		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.replace(0,               1,           "{");
		sb.replace(sb.length() - 1, sb.length(), "}");
		return sb.toString();
	}

	// Observer //////////////////////////////////////////////////////////

	@Override
	public void update(UpdateMessage msg, Observable subject) {
		if(msg instanceof UpdateMessage.Initialize) return;

		// An observed node changed
		Node resultWas = result;
		result = reduced = null; // clear cache so Patch gets re-evaluated when needed
		clearDependencies();
		if(resultWas != null) {
			UpdateMessage m = new UpdateMessage.Invalidated();
			resultWas.notifyObservers(m);
			//Node eval = getEvaluated();
			//if(eval != resultWas)
			//	eval.notifyObservers(m);
		}
	}

	private void clearDependencies() {
		for(Observable o : observations) o.removeObserver(this);
		observations.clear();
	}

	private void addDependency(Observable o) { o.addObserver(this); observations.add(o); }

}
