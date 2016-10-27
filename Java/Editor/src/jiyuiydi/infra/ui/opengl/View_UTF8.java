package jiyuiydi.infra.ui.opengl;

import java.util.ArrayList;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.Node;
import jiyuiydi.infra.UTF8;

public class View_UTF8 extends View {

	protected UTF8 text;
	protected ArrayList<View_Char> expandedChars;

	public View_UTF8(UTF8 model, ParentOfView p) {
		super(model, p);
		this.text = model;
		addFace(new Face_String(this), true);
	}

	// View //////////////////////////////////////////////////////////////

	@Override public void destroy() { text.removeObserver(this); super.destroy(); }

	@Override public Node getModel() { return text; }

	@Override void setSubstructureOpen(boolean open) {
		if(open == isSubstructureOpen) return;
		if(open) {
			expandedChars = new ArrayList<>();
			String s = text.get();
			for(int i = 0; i < s.length(); ++i)
				expandedChars.add(new View_Char(s.charAt(i), this));
			expandedChars.add(new View_Char(' ', this));
		} else {
			expandedChars = null;
		}
		invalidateLayout();
		isSubstructureOpen = open;
	}

	@Override
	void moveBy(int dx, int dy) {
		super.moveBy(dx, dy);
		if(expandedChars != null)
			for(View v : expandedChars)
				v.moveBy(dx, dy);
	}

	@Override
	public void drawBackground(GLAutoDrawable d, Editor context) {
		super.drawBackground(d, context);
		if(isPatchResultMode()) {
			final GL2 gl = d.getGL().getGL2();
			gl.glColor4f(.7f, .3f, 0f, .1f);
			drawRect(gl, internalPadding_left + liveX, liveY, liveX+liveW, liveY+liveH, true);
		}
	}

	// ParentOfView //////////////////////////////////////////////////////

	@Override public int count() { return expandedChars == null ? 0 : expandedChars.size(); }
	@Override public View get(int index) { return index == Node.INDEX_METADATA ? metadata : expandedChars.get(index); }

	// Observer //////////////////////////////////////////////////////////

	@Override
	public void update(UpdateMessage msg, Observable subject) {
		if(subject == text) {
			if(msg instanceof UpdateMessage.Inserted) {
				if(isSubstructureOpen) {
					int index = ((UpdateMessage.Inserted<String>) msg).index;
					String s = ((UpdateMessage.Inserted<String>) msg).item;
					for(int i = 0; i < s.length(); ++i)
						expandedChars.add(index++, new View_Char(s.charAt(i), this));
				}
				invalidateLayout();
			}
			if(msg instanceof UpdateMessage.Removed) {
				if(isSubstructureOpen) {
					int index = ((UpdateMessage.Removed<String>) msg).index;
					String s = ((UpdateMessage.Removed<String>) msg).item;
					for(int i = 0; i < s.length(); ++i)
						expandedChars.remove(index);
				}
				invalidateLayout();
			}
			if(msg instanceof UpdateMessage.Changed) {
				invalidateLayout();
			}
		}

		super.update(msg, subject);
	}

}
