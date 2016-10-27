package jiyuiydi.infra;

import java.util.ArrayList;
import java.util.Collection;

public abstract class EditAction {

	public boolean forPatchEditorsOnly;

	abstract public void undo();
	abstract public void redo();

	public boolean canMerge(EditAction a) { return false; }
	public EditAction merge(EditAction a) { return this ; }

	// modules ///////////////////////////////////////////////////////////

	static public class Group extends EditAction {
		public ArrayList<EditAction> actions = new ArrayList<>();

		public Group() {}
		public Group(Collection<EditAction> addAll) { actions.addAll(addAll); }

		public void add(EditAction a) { actions.add(a); }

		@Override public void undo() { for(int i = actions.size() - 1; i >=0; --i) actions.get(i).undo(); }

		@Override public void redo() { for(EditAction a : actions) a.redo(); }
	}

	static public class ReplaceInBox extends EditAction {
		protected Box box;
		protected Node existing, with;
		protected int index = 0;

		public ReplaceInBox(Box box, int index, Node with) {
			this.box = box;
			existing = box.get(index);
			this.with = with;
			this.index = index;
		}

		public ReplaceInBox(Box box, Node existing, Node with) {
			this.box = box;
			this.existing = existing;
			this.with = with;
			findIndex();
		}

		private void findIndex() {
			for(int i = 0; i < box.count(); ++i)
				if(box.get(i) == existing) { index = i; break; }
		}

		@Override public void undo() { box.replace(with, existing); }

		@Override public void redo() { box.replace(existing, with); }

		@Override public boolean canMerge(EditAction a) {
			if(a instanceof InsertInText && with == ((InsertInText) a).model)
				return true;
			return false;
		}

		@Override public EditAction merge(EditAction a) {
			return this; // text edits already affected 'with'
		}
	}

	static public class InsertInBox extends EditAction {
		protected Node item;
		protected Box box;
		protected int index;

		public InsertInBox(Node n, Box in, int at) { item = n; box = in; index = at; }
		@Override public void undo() { box.remove(index); }
		@Override public void redo() {
			if(index < 0) index += box.count() + 1;
			box.insert(index, item);
		}
	}

	static public class RemoveFromBox extends EditAction {
		protected Box box;
		protected int index, count;
		protected ArrayList<Node> items = new ArrayList<>();

		public RemoveFromBox(Box box, int index) { this.box = box; this.index = index; count = 1; }

		@Override public void undo() {
			for(Node i : items) box.insert(index, i);
			items.clear();
		}

		@Override public void redo() {
			for(int i = 0; i < count; ++i)
				items.add(box.remove(index));
		}

		@Override public boolean canMerge(EditAction a) {
			if(a instanceof RemoveFromBox) {
				RemoveFromBox d = (RemoveFromBox) a;
				if(d.box == box)
					if(d.index == index || d.index + d.count == index) return true;
			}
			return false;
		}

		@Override public EditAction merge(EditAction a) {
			RemoveFromBox d = (RemoveFromBox) a;
			count += d.count;
			if(d.index == index)
				items.addAll(0, d.items);
			else {
				items.addAll(d.items);
				index = d.index;
			}
			return this;
		}
	}

	static public class MoveBy extends EditAction {
		protected Box parent;
		protected int index, direction;

		public MoveBy(Box parent, int index, int direction) {
			this.parent = parent;
			this.index = index;
			this.direction = direction;
		}

		@Override
		public void undo() {
			Node n = parent.remove(index + direction);
			parent.insert(index, n);
		}

		@Override
		public void redo() {
			Node n = parent.remove(index);
			parent.insert(index + direction, n);
		}

	}

	static public class Delete extends RemoveFromBox {
		public Delete(Box from, int index) { super(from, index); }
	}

	//static public class SetText extends EditAction {
	//	UTF8 model; String oldText, newText;
	//	SetText(UTF8 model, String text) { this.model = model; this.newText = text; oldText = model.get(); }
	//	@Override public void undo() { model.set(oldText); }
	//	@Override public void redo() { model.set(newText); }
	//}

	static public class InsertInText extends EditAction {
		protected UTF8 model;
		protected String snip;
		protected int index;

		public InsertInText(UTF8 model, String text, int index) {
			this.model = model; snip = text; this.index = index;
		}

		@Override public void undo() {
			StringBuilder s = new StringBuilder(model.get());
			s.replace(index, index + snip.length(), "");
			model.set(s.toString());
		}

		@Override public void redo() {
			if(index < 0) index = model.value.length() + 1 + index;
			//StringBuilder s = new StringBuilder(model.get());
			//s.insert(index, snip);
			//model.set(s.toString());
			model.insert(index, snip);
		}

		@Override public boolean canMerge(EditAction a) {
			if(a instanceof InsertInText)
				if(((InsertInText) a).index == index + snip.length())
					return true;
			return false;
		}

		@Override public EditAction merge(EditAction a) {
			snip = snip + ((InsertInText) a).snip;
			return this;
		}
	}

	static public class RemoveFromText extends EditAction {
		protected UTF8 model;
		protected int index;
		protected String removed;

		public RemoveFromText(UTF8 model, int index, int length) {
			this.model = model; this.index = index;

			if(index < 0) index += model.value.length(); // local index only
			removed = model.get().substring(index, index+length);
		}

		@Override public void undo() {
			StringBuilder s = new StringBuilder(model.get());
			s.insert(index, removed);
			model.set(s.toString());
		}

		@Override public void redo() {
			if(index < 0) index += model.value.length();
			//StringBuilder s = new StringBuilder(model.get());
			//s.delete(index, index+removed.length());
			//model.set(s.toString());
			model.remove(index, removed.length());
		}

		@Override public boolean canMerge(EditAction a) {
			if(a instanceof RemoveFromText) {
				RemoveFromText rft = (RemoveFromText) a;
				if(rft.model == model)
					if(rft.index == index || rft.index+rft.removed.length() == index)
						return true;
			}
			return false;
		}

		@Override public EditAction merge(EditAction a) {
			RemoveFromText rft = (RemoveFromText) a;
			if(rft.index == index)
				removed += rft.removed;
			else {
				index = rft.index;
				removed = rft.removed + removed;
			}
			return this;
		}
	}

	static public class SetQuantity extends EditAction {
		Quantity q;
		Number toValue, wasValue;

		public SetQuantity(Quantity q, Number value) {
			this.q = q;
			toValue = value;
			wasValue = q.get();
		}

		@Override public void undo() { q.set(wasValue); }
		@Override public void redo() { q.set(toValue); }
	}

}
