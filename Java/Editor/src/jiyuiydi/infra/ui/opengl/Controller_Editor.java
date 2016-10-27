package jiyuiydi.infra.ui.opengl;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jiyuiydi.infra.*;
import jiyuiydi.infra.OS.OSExecutable;
import jiyuiydi.infra.java.JavaObjectModel;
import jiyuiydi.infra.ui.*;
import jiyuiydi.infra.ui.KeyBinding.*;
import jiyuiydi.util.BufferedBuffer;
import jiyuiydi.util.Keyboard;
import jiyuiydi.util.Observer;
import jiyuiydi.util.Utilities;

public class Controller_Editor extends Controller<Controller_Editor> implements Observer {

	static final List<KeyBinding<Controller_Editor>> bindings = new ArrayList<>();
	static void addHotkey(String desc, SoftkeyMode m, char ch, Consumer<Controller_Editor> fn) { bindings.add(new Hotkey<Controller_Editor>(m, ch , fn)); }
	static void addHotkey(String desc, SoftkeyMode m, int key, Consumer<Controller_Editor> fn) { bindings.add(new Hotkey<Controller_Editor>(m, key, fn)); }
	static {
		addHotkey("Quit"                , SoftkeyMode.CTRL      , 'Q'                   , e -> e.editor.close());
		addHotkey("Save"                , SoftkeyMode.CTRL      , 'S'                   , e -> e.editor.save());
		addHotkey("Increase font size"  , SoftkeyMode.CTRL      , KeyEvent.VK_EQUALS    , e -> e.editor.scaleFonts(1.2f));
		addHotkey("Decrease font size"  , SoftkeyMode.CTRL      , KeyEvent.VK_MINUS     , e -> e.editor.scaleFonts(0.8f));
		addHotkey("Undo"                , SoftkeyMode.CTRL      , 'Z'                   , e -> e.editor.undo());
		addHotkey("Redo"                , SoftkeyMode.CTRL      , 'Y'                   , e -> e.editor.redo());
		addHotkey("Copy"                , SoftkeyMode.CTRL      , 'C'                   , e -> e.copy());
		addHotkey("Paste"               , SoftkeyMode.CTRL      , 'V'                   , e -> e.paste());

		addHotkey("Select parent"       , SoftkeyMode.PLAIN     , KeyEvent.VK_PAGE_UP   , e -> e.selectParent());
		addHotkey("Select parent"       , SoftkeyMode.SHIFT     , KeyEvent.VK_UP        , e -> e.selectParent());
		addHotkey("Select child"        , SoftkeyMode.PLAIN     , KeyEvent.VK_PAGE_DOWN , e -> e.selectChild());
		addHotkey("Select child"        , SoftkeyMode.SHIFT     , KeyEvent.VK_DOWN      , e -> e.selectChild());
		addHotkey("Select left sibling" , SoftkeyMode.PLAIN     , KeyEvent.VK_LEFT      , e -> e.selectHorizontal(-1));
		addHotkey("Select right sibling", SoftkeyMode.PLAIN     , KeyEvent.VK_RIGHT     , e -> e.selectHorizontal(+1));
		addHotkey("Select more left"    , SoftkeyMode.SHIFT     , KeyEvent.VK_LEFT      , e -> e.editor.addSelectionRange(-1));
		addHotkey("Select more right"   , SoftkeyMode.SHIFT     , KeyEvent.VK_RIGHT     , e -> e.editor.addSelectionRange(+1));
		addHotkey("Select all"          , SoftkeyMode.CTRL      , 'A'                   , e -> e.selectAll());
		addHotkey("Select first"        , SoftkeyMode.PLAIN     , KeyEvent.VK_HOME      , e -> e.editor.setSelection(e.editor.selections.parent.get(0)));
		addHotkey("Select last"         , SoftkeyMode.PLAIN     , KeyEvent.VK_END       , e -> e.editor.setSelection(e.editor.selections.parent.get(e.editor.selections.parent.count()-1)));
		addHotkey("Select metadata"     , SoftkeyMode.PLAIN     , KeyEvent.VK_ESCAPE    , e -> e.selectMeta());

		addHotkey("Create left"         , SoftkeyMode.CTRL      , KeyEvent.VK_LEFT      , e -> e.createHorizontal(-1));
		addHotkey("Create right"        , SoftkeyMode.CTRL      , KeyEvent.VK_RIGHT     , e -> e.createHorizontal(+1));
		addHotkey("Create inside"       , SoftkeyMode.CTRL      , KeyEvent.VK_PAGE_DOWN , e -> e.createInner());

		addHotkey("Move in"             , SoftkeyMode.ALT       , KeyEvent.VK_PAGE_DOWN , e -> e.moveInner());
		addHotkey("Move out"            , SoftkeyMode.ALT       , KeyEvent.VK_PAGE_UP   , e -> e.moveOuter());
		addHotkey("Move left"           , SoftkeyMode.ALT       , KeyEvent.VK_LEFT      , e -> e.moveHorizontal(-1));
		addHotkey("Move right"          , SoftkeyMode.ALT       , KeyEvent.VK_RIGHT     , e -> e.moveHorizontal(+1));

		addHotkey("Backspace"           , SoftkeyMode.PLAIN     , Keyboard.backspace    , e -> e.backspace());
		addHotkey("Delete"              , SoftkeyMode.PLAIN     , Keyboard.delete       , e -> e.deleteSelection());
		addHotkey("Enter"               , SoftkeyMode.PLAIN     , KeyEvent.VK_ENTER     , e -> e.dispatchEnter());
		addHotkey("Space"               , SoftkeyMode.PLAIN     , KeyEvent.VK_SPACE     , e -> e.space(false));
		addHotkey("ShiftSpace"          , SoftkeyMode.SHIFT     , KeyEvent.VK_SPACE     , e -> e.space(true));
		addHotkey("Delete all children" , SoftkeyMode.CTRL      , Keyboard.backspace    , e -> e.backspaceAll());

		addHotkey("Evaluate  "          , SoftkeyMode.CTRL      , 'E'                   , e -> e.evaluate());
		addHotkey("Unevaluate"          , SoftkeyMode.CTRL_SHIFT, 'E'                   , e -> e.unevaluate());

		addHotkey("Convert to Key-value", SoftkeyMode.CTRL      , KeyEvent.VK_SEMICOLON , e -> e.convertToKeyed());
		addHotkey("Cycle view mode"     , SoftkeyMode.PLAIN     , KeyEvent.VK_TAB       , e -> e.cycleViewMode(false));
		addHotkey("Cycle view mode"     , SoftkeyMode.SHIFT     , KeyEvent.VK_TAB       , e -> e.cycleViewMode(true));

		addHotkey("Submit"              , SoftkeyMode.CTRL      , KeyEvent.VK_ENTER     , e -> e.submit());
	}

	static KeyBinding<?> kbTyping = new Printables<Controller_Editor>( (e, letter) -> e.typeText(letter) );

	// instance //////////////////////////////////////////////////////////

	private Editor editor;
	private Set<EditActionCaptureContext> activeCaptureContexts = new HashSet<>();
	View previousHover;
	final ArrayList<View> clipboard = new ArrayList<>();

	// Controller_Editor /////////////////////////////////////////////////

	public Controller_Editor(Editor editor) {
		super(editor.canvas);
		this.editor = editor;
		editor.frame.setTransferHandler(new FileDropHandler(editor::load));
		editor.selections.addObserver(this);

		editor.canvas.setFocusTraversalKeysEnabled(false);
		loadKeyBindings(this);
	}

	void copy() {
		if(editor.selections.count() > 0) {
			clipboard.clear();
			clipboard.addAll(editor.selections.getViews());
		}
	}

	void paste() {
		if(editor.selections.count() == 0 || clipboard.isEmpty()) return;
		if(editor.selections.parent instanceof View_Box) {
			Box dest = ((View_Box) editor.selections.parent).getModel();
			int at = editor.selections.low();
			Spine from = editor.selections.parent.get(at).getMomentOfTree();
			deleteSelection();
			for(View v : clipboard) {
				Spine target = v.getMomentOfTree();
				dest.insert(at++, Patch.createReference(target, from));
			}
		}
	}

	void cycleViewMode(boolean prev) {
		for(View v : editor.selections.getViews())
			if(prev) v.prevFace();
			else     v.nextFace();
	}

	boolean selectAtXY(View v, int x, int y) {
		if(v.isSubstructureOpen)
			for(int i = 0; i < v.count(); ++i) {
				if(selectAtXY(v.get(i), x, y)) return true;
			}
		if(v.isPointInside(x, y)) {
			//while(v.parent instanceof View && !((View)v.parent).isSubstructureOpen)
			//	v = (View) v.parent; // crawl up to next open view
			if(v.count() > 0 && !v.isSubstructureOpen && editor.selections.isOnlySelection(v)) {
				v.setSubstructureOpen(true);
				return selectAtXY(v, x, y); // auto-open the substructure and try again
			}

			editor.setSelection(v);
			//if(v instanceof View_PatchResult) {
			//	((View_PatchResult) v).setCaptureEnabled(false);
			//}
			return true;
		}
		return false;
	}

	View getViewAtXY(View v, int x, int y) {
		if(v.isPointInside(x, y)) {
			if(v.isSubstructureOpen) {
				for(int i = 0; i < v.count(); ++i) {
					View cv = getViewAtXY(v.get(i), x, y);
					if(cv != null) return cv;
				}
			}
			return v;
		}
		return null;
	}

	void hoverHighlight(View v) {
		if(v == previousHover) return;
		if(previousHover != null)
			previousHover.setHoverHighlight(false);
		if(v != null)
			v.setHoverHighlight(true);
		previousHover = v;
		editor.canvas.repaint();
	}

	void typeText(char ch) {
		for(View s : editor.selections.getViews()) {
			if(s instanceof View_UTF8) {
				View_UTF8 vt = (View_UTF8) s;
				//vt.text.set(vt.text.get() + ch);
				//editor.performEdit(new EditAction.SetText(vt.text, vt.text.get() + ch));
				vt.performEdit(new EditAction.InsertInText(vt.text, String.valueOf(ch), -1));
				checkQuantityConvertion(vt);
			}
			else
			if(s instanceof View_Char) {
				View_Char vc = (View_Char) s;
				View_UTF8 vs = (View_UTF8) vc.parent;
				int index = vs.indexOf(vc);
				vs.performEdit(new EditAction.InsertInText(vs.text, String.valueOf(ch), index));
				editor.setSelection(s); // update selection index
			}
			else
			if(s instanceof View_Box) {
				if(s.count() == 0) {
					if(s.parent instanceof View_Box) {
						View_Box pv = (View_Box) s.parent;
						Box pm = pv.model;
						UTF8 n = new UTF8(""+ch);
						//pm.replace(s.getModel(), n);
						//n.set(""+ch);
						pv.performEdit(new EditAction.ReplaceInBox(pm, s.getModel(), n));
						//pv.performEdit(new EditAction.InsertInText(n, ""+ch, 0));
					}
				}
			}
		}
	}

	void backspace() {
		for(View s : editor.selections.getViews()) {
			if(s instanceof View_UTF8) {
				View_UTF8 vt = (View_UTF8) s;
				String str = vt.text.get();
				if(str.length() > 0)
					//vt.text.set(str.substring(0, str.length() - 1));
					vt.performEdit(new EditAction.RemoveFromText(vt.text, -1, 1));
				checkQuantityConvertion(vt);
			}
			if(s instanceof View_Char) {
				View_Char vc = (View_Char) s;
				View_UTF8 vs = (View_UTF8) vc.parent;
				int index = vs.indexOf(vc);
				String str = vs.text.get();
				if(index > 0 && str.length() > 0)
					vs.performEdit(new EditAction.RemoveFromText(vs.text, index-1, 1));
				editor.setSelection(s); // update selection index
			}
		}
	}

	void backspaceAll() {
		for(View s : editor.selections.getViews()) {
			if(s instanceof View_UTF8) {
				View_UTF8 vt = (View_UTF8) s;
				vt.performEdit(new EditAction.RemoveFromText(vt.text, 0, vt.text.get().length()));
				checkQuantityConvertion(vt);
			}
			if(s instanceof View_Box) {
				int count = s.count();
				for(int i = 0; i < count; ++i)
					s.performEdit(new EditAction.RemoveFromBox(((View_Box) s).model, 0));
			}
		}
	}

	void deleteSelection() {
		if(editor.selections.count() == 0) return;
		int lo = editor.selections.low();
		int hi = editor.selections.high();
		if(editor.selections.parent instanceof View_Box) {
			View_Box p = (View_Box) editor.selections.parent;
			Box m = p.model;
			//editor.selections.deselect();
			EditAction.Group g = new EditAction.Group();
			g.add(new ViewEditAction.SelectFutureChildren(editor, null));
			for(int i = lo; i <= hi; ++i) {
				//View.destroyRecycledViewsOf(m.remove(lo));
				g.add(new ViewEditAction.Delete(m, lo)); // calls View.destroyRecycledViewsOf
			}
			lo = Math.min(lo, m.count() - 1);
			//editor.selections.select(lo > -1 ? p.get(lo) : p);
			if(lo > -1)
				g.add(new ViewEditAction.SelectFutureChildren(editor, null, new Selection.Path(p, lo)));
			else
				g.add(new ViewEditAction.SelectFutureChildren(editor, null, new Selection.Path(p.parent, p.parent.indexOf(p))));
			p.performEdit(g);
		}
		else
		if(editor.selections.parent instanceof View_UTF8) { // expanded string
			View_UTF8 s = (View_UTF8) editor.selections.parent;
			if(lo < s.text.get().length()) {
				s.performEdit(new ViewEditAction.SelectFutureChildren(editor, null));
				s.performEdit(new EditAction.RemoveFromText(s.text, lo, hi-lo+1));
				//editor.selections.select(s.get(lo));
				s.performEdit(new ViewEditAction.SelectFutureChildren(editor, null, new Selection.Path(s, lo)));
			}
		}
	}

	void dispatchEnter() {
		for(View s : editor.selections.getViews()) {
			if(s instanceof View_UTF8) {
			}
			else
			if(s instanceof View_Char) typeText('\n');
			else
				layoutOrientation();
		}
	}

	void layoutOrientation() {
		for(View v : editor.selections.getViews()) {
			v.setVerticalLayout(!v.isVerticalLayout());
		}
	}

	void space(boolean shift) {
		if(editor.selections.count() == 0) return;
		View s = editor.selections.getEnd();

		if(s instanceof View_Char) {
			if(shift) {
				// TODO: split into 2 strings
				return;
			}
			typeText(' ');
			return;
		}

		if(shift) {
			typeText(' ');
			return;
		}

		ParentOfView p = editor.selections.parent;
		int index = editor.selections.end + 1;

		if(s instanceof View_Box && s.count() == 0) {
			p = s; index = 0;
		}

		if(p instanceof View_Box) {
			Box pm = ((View_Box) p).getModel();
			//pm.insert(index, new Box());
			//editor.setSelection(p.get(index));

			boolean hasPatchViewAnscestor = false;
			ParentOfView an = p;
			while(an.getClass() == View_Box.class || an instanceof View_Patch) {
				if(an instanceof View_Patch) { hasPatchViewAnscestor = true; break; }
				an = ((View) an).parent;
			}
			Node n = hasPatchViewAnscestor ? new Keyed() : new Box();

			((View) p).performEdit(new EditAction.InsertInBox(n, pm, index));
			((View) p).performEdit(new ViewEditAction.SelectFutureChildren(editor, new Selection.Path(p, index)));
		}
	}

	void selectParent() {
		ParentOfView p = editor.selections.parent;

		if(p instanceof View) {
			editor.setSelection((View) p); // change selection first
			((View) p).setSubstructureOpen(false); // this may delete views (if closing an expanded UTF8 node)

			int i = ((View) p).parent.indexOf((View) p);
			View c = ((View) p).parent.get(i);

			if(p instanceof View_UTF8) // coming up from View_Char
				checkQuantityConvertion((View_UTF8) p);
		}
	}

	void selectChild() {
		if(editor.selections.count() > 0) {
			if(editor.selections.count() > 1) { // select multiple --> select single
				editor.setSelection(editor.selections.getStart());
			} else {
				View s = editor.selections.getEnd();
				s.setSubstructureOpen(true);
				if(s.count() > 0) { // selected node has children

					//if(s instanceof View_PatchResult)
					//	updateCaptureMode((View_PatchResult) s);

					int i = 0;
					if(s instanceof View_UTF8)
						i = s.count(); // select last sub-character
					if(s instanceof View_Box)
						i = ((View_Box) s).previouslySelectedIndex;
					//if(s instanceof View_PatchResult && ((View_PatchResult) s).resultView instanceof View_Box)
					//	i = ((View_Box) ((View_PatchResult) s).resultView).previouslySelectedIndex;
					i = Math.max(0, Math.min(s.count() - 1, i));
					editor.setSelection(s.get(i));
				}
			}
		}
	}

	void selectMeta() {
		if(editor.selections.count() == 0) return;
		View s = editor.selections.getEnd();
		if(s.metadata != null)
			editor.setSelection(s.metadata);
	}

	void selectHorizontal(int direction) {
		if(editor.selections.count() == 0) return;

		int newIndex;
		if(direction < 0) newIndex = direction + editor.selections.low();
		else              newIndex = direction + editor.selections.high();

		newIndex = Utilities.clamp(newIndex, 0, editor.selections.parent.count()-1);
		//if(newIndex >= 0 && newIndex < editor.selections.parent.count())
		editor.setSelection(editor.selections.parent.get(newIndex));
	}

	void selectAll() {
		if(editor.selections.parent == null) {
			editor.setSelection(editor.rootView);
			return;
		}
		if(editor.selections.count() == 0) return;
		editor.setSelection(editor.selections.parent.get(0));
		editor.addSelectionRange(editor.selections.parent.count()-1);
	}

	void createHorizontal(int direction) {
		if(editor.selections.count() == 0) return;

		if(editor.selections.parent instanceof View_Box) {
			View_Box p = (View_Box) editor.selections.parent;
			int index;
			if(direction <= 0)
				index = editor.selections.low();
			else
				index = editor.selections.high() + 1;
			//p.model.insert(index, new Box());
			p.performEdit(new EditAction.InsertInBox(new Box(), p.model, index));
		}
	}

	void createInner() {
		for(View s : editor.selections.getViews()) {
			if(s instanceof View_Box) {
				((View_Box) s).model.insert(0, new Box());
			}
		}
	}

	void moveInner() {
		if(editor.selections.count() == 0) return;

		if(editor.selections.parent instanceof View_Box) {
			View_Box pb = (View_Box) editor.selections.parent;

			List<View> vs = editor.selections.getViews();
			int index = editor.selections.low();
			int count = editor.selections.count();
			EditAction.Group actions = new EditAction.Group();
			//editor.selections.deselect();
			actions.add(new ViewEditAction.SelectFutureChildren(editor, null));

			for(int i = 0; i < count; ++i)
				//pb.model.remove(index);
				actions.add(new EditAction.RemoveFromBox(pb.model, index));

			Box b = new Box();
			//pb.model.insert(index, b);
			actions.add(new EditAction.InsertInBox(b, pb.model, index));

			int i = 0;
			for(View v : vs)
				//b.insert(b.count(), v.getModel());
				actions.add(new EditAction.InsertInBox(v.getModel(), b, i++));

			//editor.setSelection(pb.get(index).get(0));
			//editor.addSelectionRange(count + 1);
			actions.add(new ViewEditAction.SelectFutureChildren(editor, new Selection.Path(pb, index)));
			pb.performEdit(actions);
		}
	}

	void moveOuter() { // move up in hierarchy
		if(editor.selections.count() == 0) return;
		if(editor.selections.parent instanceof View_Box) {
			View_Box p = (View_Box) editor.selections.parent;
			Box source = p.model;
			int fromIndex = editor.selections.low();
			int count = editor.selections.count();
			if(p.parent instanceof View_Box) {
				View_Box pp = (View_Box) p.parent;
				Box destination = pp.model;
				int toIndex = p.parent.indexOf(p) + 1;

				EditAction.Group actionsP = new EditAction.Group();
				EditAction.Group actionsPP = new EditAction.Group();
				//editor.selections.deselect();
				actionsP.add(new ViewEditAction.SelectFutureChildren(editor, null));
				for(int i = 0; i < count; ++i) {
					//Node n = source.remove(fromIndex);
					//destination.insert(toIndex + i, n);
					actionsP.add(new EditAction.RemoveFromBox(source, fromIndex));
					actionsPP.add(new EditAction.InsertInBox(source.get(fromIndex + i), destination, toIndex + i));
				}
				//editor.setSelection(pp.get(toIndex));
				//editor.addSelectionRange(count - 1);
				actionsPP.add(new ViewEditAction.SelectFutureChildren(editor, null, new Selection.Path(pp, toIndex, toIndex + count - 1)));

				//if(source.count() == 0)
				//	destination.remove(toIndex - 1);
				if(source.count() == count)
					actionsPP.add(new ViewEditAction.Delete(destination, toIndex - 1));

				p.performEdit(actionsP); // deselect, remove each
				pp.performEdit(actionsPP); // insert each, select
			}
		}
	}

	void moveHorizontal(int direction) {
		if(editor.selections.parent instanceof View_Box) {
			View_Box p = (View_Box) editor.selections.parent;
			int loIndex = editor.selections.low();
			int hiIndex = editor.selections.high();
			if(direction > 0)
				direction = Math.min(direction, p.count() - 1 - hiIndex); // clamp to head-room
			else
				direction = Math.max(direction, 0 - loIndex);

			List<View> lv = editor.selections.getViews();
			View v = editor.selections.getStart();
			int range = editor.selections.getRange();

			EditAction.Group actions = new EditAction.Group();

			actions.add(new ViewEditAction.SelectFutureChildren(editor, null)); // deselect
			//for(int i = loIndex; i <= hiIndex; ++i)
			//	actions.add(new EditAction.RemoveFromBox(p.model, loIndex));
			//for(int i = loIndex + direction; i <= hiIndex + direction; ++i)
			//	actions.add(new EditAction.InsertInBox(lv.remove(0).getModel(), p.model, i));
			for(int i = loIndex; i <= hiIndex; ++i)
				actions.add(new EditAction.MoveBy(p.model, loIndex, direction));
			actions.add(new ViewEditAction.SelectFutureChildren(editor, new Selection.Path(p, loIndex+direction, loIndex+direction+range)));

			p.performEdit(actions);
			editor.canvas.repaint();
		}
	}

	void evaluate() {
		ParentOfView sp = editor.selections.parent;
		for(int i = editor.selections.low(); i <= editor.selections.high(); ++i) {
			View s = sp.get(i);
			if(s instanceof View_Patch) {
				Patch p = (Patch) s.getModel();
				if(!p.hasHome())
					p.setHome(s.getMomentOfTree());
				View v = View.createView(p.getResult(), sp);
				v.enablePatchResultMode(p);
				View prev = sp.replace(i, v);
				View.destroyRecycledViewsOf(prev.getModel());
				if(prev.isPatchResultMode())
					p = prev.getPatchOrigin(); // forward same origin to next step
				v.addObserver(this);
			}
		}

		//for(View s : editor.selections.getViews()) {
		//if(s.getModel() instanceof Patch) {
		//	Spine t = s.getMomentOfTree();
		//	t.eval();
		//}
		//else
		//if(s.parent instanceof View_Patch) {
		//	int index = s.parent.indexOf(s);
		//	int pc = ((View_Patch) s.parent).partialEval + 1 + index;
		//	((View_Patch) s.parent).setPartialEval(pc);
		//	if(s.parent.count() > 0)
		//		editor.setSelection(s.parent.get(0));
		//	else
		//		editor.setSelection((View) s.parent);
		//}
		//else
		//if(Tree.isPatchResult(s.getModel())) {
		//	Spine t = s.getMomentOfTree();
		//	t.reeval();
		//}
	}

	void unevaluate() {
		ParentOfView sp = editor.selections.parent;
		for(int i = editor.selections.low(); i <= editor.selections.high(); ++i) {
			View s = sp.get(i);
			Patch p = s.getPatchOrigin();
			if(p != null) {
				View v = View.createView(p, sp);
				View prev = sp.replace(i, v);
				prev.removeObserver(this);
				View.destroyRecycledViewsOf(prev.getModel());
			}
		}

//		for(View s : editor.selections.getViews()) {
//			Spine t = new Spine(editor.rootView.getModel());
//			Trail p = new Trail();
//			View v = s;
//			while(v.parent instanceof View) {
//				p.up(v.parent.indexOf(v));
//				v = (View) v.parent;
//			}
//			p.rewind(t);
//			boolean didUneval = t.unevaluate(false);
//
//			if(!didUneval) {
//				if(s instanceof View_Patch) {
//					View_Patch vp = (View_Patch) s;
//					if(vp.partialEval > 0)
//						vp.setPartialEval(vp.partialEval - 1);
//				}
//				else
//				if(t.get() instanceof Box && ((Box) t.get()).count() == 0)
//					t.replace(new Patch());
//			}
//		}
	}

	void convertToKeyed() {
		int vIndex = editor.selections.low();
		if(!(editor.selections.parent instanceof View)) return;
		Box selectionParentModel = (Box) ((View) editor.selections.parent).getModel();
		for(View v : editor.selections.getViews()) {
			Node m = v.getModel();
			if(m instanceof Keyed) { // convert to Box
				Box rep = new Box();
				for(int i = 0; i < m.count(); ++i) rep.add(m.get(i));
				selectionParentModel.replace(vIndex, rep);
			}
			else
			if(m instanceof Box) { // convert to Keyed
				Keyed rep = new Keyed();
				for(int i = 0; i < m.count(); ++i) rep.add(m.get(i));
				selectionParentModel.replace(vIndex, rep);
			}
			else { // Wrap node in Keyed
				selectionParentModel.replace(vIndex, new Keyed(m));
			}
			++vIndex;
		}
	}

	private void checkQuantityConvertion(View_UTF8 s) {
		String formS = s.text.get();
		try {
			int formI = Integer.parseInt(formS);
			if(s.getModel() instanceof Int32) return;
			if(s.parent instanceof View_Box)
				((View_Box) s.parent).getModel().replace(s.getModel(), new Int32(formI));
			return;
		} catch(NumberFormatException e) {}
		try {
			float formF = Float.parseFloat(formS);
			if(s.getModel() instanceof Float32) return;
			if(s.parent instanceof View_Box)
				((View_Box) s.parent).getModel().replace(s.getModel(), new Float32(formF));
			return;
		} catch(NumberFormatException e) {}

		if(s instanceof View_Quantity) {
			if(s.parent instanceof View_Box)
				((View_Box) s.parent).getModel().replace(s.getModel(), new UTF8(formS));
			return;
		}
	}

	void submit() {
		Sender destination = null;
		ParentOfView p = editor.selections.parent;
		while(destination == null && p != null) {
			if(p instanceof View && ((View) p).getModel() instanceof Sender)
				destination = (Sender) ((View) p).getModel();
			if(p instanceof View)
				p = ((View) p).parent;
			else
				p = null;
		}
		if(destination == null) return;
		for(View s : editor.selections.getViews()) {
			destination.send(s.getModel());
			//BufferedBuffer en = InfraEncoding.encoded(s.getModel());
			//ByteBuffer bb = en.read((int) en.remaining());
			//System.out.write(bb.array(), bb.position(), bb.remaining());
		}
	}

	// Controller ////////////////////////////////////////////////////////

	@Override
	public void mousePressed(MouseEvent e) {
		editor.canvas.requestFocusInWindow();
		if(e.getButton() == MouseEvent.BUTTON1) {
			if(selectAtXY(editor.rootView, e.getX(), e.getY())) {
				View selected = editor.selections.getStart();
				if(selected instanceof View_Mutation) {
					((View_Mutation) selected).setDepressed(true);
				}
				e.consume();
			} else {
				editor.setSelection(null);
				e.consume();
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		View v = getViewAtXY(editor.rootView, e.getX(), e.getY());
		if(e.getButton() == MouseEvent.BUTTON1) {
			for(View selected : editor.selections.getViews()) {
				if(selected instanceof View_Mutation) {
					((View_Mutation) selected).setDepressed(false);
					if(v == selected) ((View_Mutation) selected).activate();
					editor.canvas.repaint();
					break;
				}
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON3) {
			JPopupMenu popup = new EditorPopupMenu(editor);
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		//editor.mousePos.setLocation(e.getPoint());
		//editor.canvas.repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		View v = getViewAtXY(editor.rootView, e.getX(), e.getY());
		hoverHighlight(v);
	}

	// Observer //////////////////////////////////////////////////////////

	@Override public void update(UpdateMessage msg, Observable subject) {
		// if selection changed
		if(subject == editor.selections && msg instanceof UpdateMessage.Changed) {
			for(View v : editor.selections.getViews()) {
				if(v.getModel() != null && v.getModel().isUnloaded())
					DelayedLoader.useLoader(v.getModel());
			}

			ArrayList<EditActionCaptureContext> whitelist = new ArrayList<>();
			//whitelist.addAll(editor.selections.getViews());
			ParentOfView p = editor.selections.parent;
			while(p instanceof View) {
				View v = (View) p;
				if(v.isPatchResultMode()) {
					v.setCaptureEnabled(true);
					activeCaptureContexts.add(v);
					whitelist.add(v);
				}
				p = v.parent;
			}
			for(Iterator<EditActionCaptureContext> i = activeCaptureContexts.iterator(); i.hasNext();) {
				EditActionCaptureContext cc = i.next();
				if(!whitelist.contains(cc)) {
					i.remove();
					cc.setCaptureEnabled(false);
				}
			}
			for(View v : editor.selections.getViews()) {
				if(v instanceof View_Box) continue;
				if(v.isPatchResultMode()) {
					v.setCaptureEnabled(true);
					activeCaptureContexts.add(v);
				}
			}
		}

		if(msg instanceof UpdateMessage.Invalidated) {
			if(subject instanceof View) {
				View v = (View) subject;
				if(v.isPatchResultMode()) {
					ParentOfView p = v.parent;
					int index = p.indexOf(v);
					Node newN;
					//if(v.getModel() instanceof Symbol.Error)
						newN = v.resultEditor.patch.getEvaluated();
					//else
					//	newN = v.resultEditor.patch.getReduced();
					View newV = View.createView(newN, p);
					newV.enablePatchResultMode(v.getPatchOrigin());
					newV.addObserver(this);
					v.removeObserver(this);
					p.replace(index, newV);
					View.destroyRecycledViewsOf(v.getModel());
				}
			}
		}
	}

}
