package jiyuiydi.infra.ui.opengl;

import java.util.ArrayList;
import java.util.function.Consumer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.Node;
import jiyuiydi.util.Observer;
import jiyuiydi.util.Observer.UpdateMessage;

public class View_Box extends View {

	Box model;
	protected final ArrayList<View> views = new ArrayList<>();
	int baseline;
	int previouslySelectedIndex;
	boolean layoutVertical;

	public View_Box(Box model, ParentOfView p) {
		super(model, p);
		this.model = model;
		update(UpdateMessage.initializeMessage, model);
		addFace(new Face_Box(this), true);
		addFace(new Face_Table(this), false);
	}

	// View //////////////////////////////////////////////////////////////

	@Override public void destroy() {
		model.removeObserver(this);
		for(View v : views) v.destroy();
		views.clear();
	}

	@Override public Box getModel() { return model; }

	@Override boolean isVerticalLayout() { return layoutVertical; }
	@Override void setVerticalLayout(boolean enabled) {
		layoutVertical = enabled;
		invalidateLayout();
	}

	@Override
	void moveBy(int dx, int dy) {
		super.moveBy(dx, dy);
		for(View v : views) v.moveBy(dx, dy);
	}

	// ParentOfView //////////////////////////////////////////////////////

	@Override public int count() { return views.size(); }
	@Override public View get(int index) { return index == Node.INDEX_METADATA ? metadata : views.get(index); }
	@Override public View replace(int index, View v) {
		View was = views.set(index, v);
		notifyReplacement(index, was, v);
		was.destroy();
		invalidateLayout();
		return was;
	}

	// Observer //////////////////////////////////////////////////////////

	private ViewReplacement activeViewReplacement;

	@Override
	public void update(UpdateMessage msg, Observable subject) {
		if(subject == model) {
			if(msg instanceof UpdateMessage.Initialize) {
				model.foreach(n -> views.add(createView(n, this)));
				invalidateLayout();
			}
			if(msg instanceof UpdateMessage.Replaced) {
				@SuppressWarnings("unchecked")
				UpdateMessage.Replaced<Node> m = (UpdateMessage.Replaced<Node>) msg;
				ViewReplacement vr = new ViewReplacement();
				vr.oldModel = m.was;
				vr.oldView = views.get(m.index);
				vr.newModel = model.get(m.index);
				vr.newView = createView(vr.newModel, this);
				vr.oldView.destroy();
				views.set(m.index, vr.newView);
				activeViewReplacement = vr;
				notifyReplacement(m.index, vr.oldView, vr.newView);
				invalidateLayout();
			}
			if(msg instanceof UpdateMessage.Removed) {
				if(activeViewReplacement != null) {
					// removed; now need inserted
				} else {
					@SuppressWarnings("unchecked")
					UpdateMessage.Removed<Node> m = (UpdateMessage.Removed<Node>) msg;
					View v = views.remove(m.index);
					recycleView(v);
					notifyRemoval(m.index, v);
					invalidateLayout();
				}
			}
			if(msg instanceof UpdateMessage.Inserted) {
				if(activeViewReplacement != null) {
					activeViewReplacement = null; // complete
				} else {
					@SuppressWarnings("unchecked")
					UpdateMessage.Inserted<Node> m = (UpdateMessage.Inserted<Node>) msg;
					View newView = createView(m.item, this);
					views.add(m.index, newView);
					notifyInsertion(m.index, newView);
					invalidateLayout();
				}
			}
		}

		super.update(msg, subject);
	}

}
