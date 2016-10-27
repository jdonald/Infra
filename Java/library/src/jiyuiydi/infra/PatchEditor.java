package jiyuiydi.infra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import jiyuiydi.infra.EditAction.*;

@PatchEditor.SubclassSpecialty(Patch.class)
public class PatchEditor {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	static public @interface SubclassSpecialty {
		public Class<? extends Patch> value();
	}

	// instance //////////////////////////////////////////////////////////

	public final Patch patch;
	public final Node result;
	public boolean captureEnabled;

	public PatchEditor(Patch p, Node result) { patch = p; this.result = result; }

	public EditAction performEdit(EditAction in) {
		if(!captureEnabled) return in;
		ArrayList<EditAction> out = new ArrayList<>();
		performEdit(in, out);
		if(out.size() >  1) return new EditAction.Group(out);
		if(out.size() == 1) return out.get(0);
		return null;
	}

	protected void performEdit(EditAction in, List<EditAction> out) {
		if(in instanceof EditAction.Group)
			for(EditAction ea : ((EditAction.Group) in).actions)
				performEdit(ea, out);

		if(in instanceof InsertInText) {
			InsertInText a = (InsertInText) in;

			Box cmds = new Box();
			if(!addNavigationCommands(a.model, cmds)) return;
			cmds.add(new Command(Opcode.DOWN, a.index));
			cmds.add(new Command(Opcode.INSERT, a.snip));
			out.add(new EditAction.InsertInBox(cmds, patch, -1));
		}

		if(in instanceof RemoveFromText) {
			RemoveFromText a = (RemoveFromText) in;
			Box cmds = new Box();
			if(!addNavigationCommands(a.model, cmds)) return;
			cmds.add(new Command(Opcode.DOWN, a.index));
			//a.removed.length();
			cmds.add(new Command(Opcode.WRITE));
			out.add(new EditAction.InsertInBox(cmds, patch, -1));
		}

		if(in instanceof InsertInBox) {
			InsertInBox a = (InsertInBox) in;

			Box cmds = new Box();
			if(!addNavigationCommands(a.box, cmds)) return;
			cmds.add(new Command(Opcode.DOWN_NOEVAL, a.index));
			cmds.add(new Command(Opcode.INSERT, a.item.deepCopy()));
			out.add(new EditAction.InsertInBox(cmds, patch, -1));
		}

		if(in instanceof RemoveFromBox) {
			RemoveFromBox a = (RemoveFromBox) in;
			if(a.count > 0) {
				Node first = a.box.get(a.index);
				Box cmds = new Box();
				if(!addNavigationCommands(first, cmds)) return;
				for(int i = 0; i < a.count; ++i)
					cmds.add(new Command(Opcode.WRITE));
				out.add(new EditAction.InsertInBox(cmds, patch, -1));
			}
		}

		if(in instanceof ReplaceInBox) {
			ReplaceInBox a = (ReplaceInBox) in;
			Box cmds = new Box();
			if(!addNavigationCommands(a.existing, cmds)) return;
			cmds.add(new Command(Opcode.WRITE, a.with));
			out.add(new EditAction.InsertInBox(cmds, patch, -1));
		}

		if(in instanceof MoveBy) {
			MoveBy a = (MoveBy) in;
			Box cmds = new Box();
			if(!addNavigationCommands(a.parent.get(a.index), cmds)) return;
			cmds.add(new Command(Opcode.MOVE_BY, a.direction));
			out.add(new EditAction.InsertInBox(cmds, patch, -1));
		}
	}

	private boolean addNavigationCommands(Node target, Box b) {
		Trail p = new Trail();
		if(!p.searchFor(target, result)) return false;
		//if(p.childHops.size() == 1) b.add(new Command(Opcode.DOWN, p.childHops.get(0)));
		//if(p.childHops.size() >= 2) b.add(new Command(Opcode.DOWN, Node.toNode(p.childHops)));
		if(p.childHops.size() > 0) {
			Command down = new Command(Opcode.DOWN_NOEVAL);
			for(Integer i : p.childHops) down.add(i);
			b.add(down);
		}
		return true;
	}

}
