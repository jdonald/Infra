package jiyuiydi.infra.ui.opengl;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.EditAction;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.Trail;

public class ViewEditAction {

	static public class SelectFutureChildren extends EditAction {

		protected Editor context;
		Selection.Path was, become;

		public SelectFutureChildren(Editor editor, Selection.Path beingSelected) {
			this(editor, new Selection.Path(editor.selections), beingSelected);
		}

		public SelectFutureChildren(Editor editor, Selection.Path wasSelected, Selection.Path beingSelected) {
			context = editor;
			was = wasSelected;
			become = beingSelected;
		}

		@Override public void undo() {
			if(was == null) { context.selections.deselect(); return; }
			was.select(context);
		}

		@Override public void redo() {
			if(become == null) { context.selections.deselect(); return; }
			become.select(context);
		}

	}

	static public class Delete extends EditAction.Delete {

		Delete(Box from, int index) { super(from, index); }

		@Override public void redo() {
			super.redo();
			for(Node i : items) View.destroyRecycledViewsOf(i);
		}

	}

}
