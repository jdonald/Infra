package jiyuiydi.infra.ui.opengl;

import java.util.List;

import jiyuiydi.infra.EditAction;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.Patch;
import jiyuiydi.infra.PatchEditor;

public class PatchEditor_ForView extends PatchEditor {

	public PatchEditor_ForView(Patch p, Node result) {
		super(p, result);
	}

	@Override
	protected void performEdit(EditAction in, List<EditAction> out) {
		if(in instanceof ViewEditAction.SelectFutureChildren) {
			in.forPatchEditorsOnly = false;
			out.add(in);
		} else
			super.performEdit(in, out);
	}

}
