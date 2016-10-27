package jiyuiydi.infra.ui.opengl;

import java.awt.Component;
import java.awt.FontMetrics;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.awt.TextRenderer;

import jiyuiydi.infra.*;
import jiyuiydi.infra.PatchEditor.SubclassSpecialty;
import jiyuiydi.infra.ui.Animator;
import jiyuiydi.infra.ui.Animator.Animation;
import jiyuiydi.infra.ui.opengl.BatchTextRenderer.TextRenderingClient;
import jiyuiydi.util.Observer;
import jiyuiydi.util.Observer.Observable;
import jiyuiydi.util.Observer.UpdateMessage;
import jiyuiydi.util.Reflection;

public abstract class View extends Observable implements Animation, ParentOfView, TextRenderingClient, Observer, EditActionCaptureContext {

	static private final float tweenPercent = .35f;

	static public View createView(Node n, ParentOfView parent) {
		View v = constructView(n, parent);
		if(!(parent instanceof View)) v.isSubstructureOpen = true;
		return v;
	}

	static private View constructView(Node n, ParentOfView parent) {
		final Class<?> nc = n.getClass();

		for(View v : recycledViews)
			if(v.getModel() == n) {
				recycledViews.remove(v);
				v.parent = parent;
				return v;
			}

		if(nc == Symbol.Null     .class) return new View_Null((Symbol.Null) n, parent);
		if(nc == Symbol.Parameter.class) return new View_Symbol_Parameter((Symbol.Parameter) n, parent);

		if(n instanceof Quantity) return new View_Quantity((Quantity) n, parent);
		if(n instanceof UTF8)     return new View_UTF8    ((UTF8)     n, parent);
		if(n instanceof Patch)    return new View_Patch   ((Patch)    n, parent);

		//if(n instanceof Command) return new View_Command((Keyed) n, parent);
		if(n instanceof Keyed)    {
			boolean ansestorOfPatch = false;
			ParentOfView p = parent;
			do {
				if(p instanceof View_Box && ((View_Box) p).getModel() instanceof Metadata) break; // top-level metadata entries
				if(p instanceof View_Patch) { ansestorOfPatch = true; break; } // bingo
				if(p instanceof View) p = ((View) p).parent; else p = null; // move along
			} while(p != null);
			if(ansestorOfPatch) return new View_Command((Keyed) n, parent);
			return new View_Keyed   ((Keyed)    n, parent);
		}

		if(n instanceof Box )     return new View_Box   ((Box)    n, parent);
		if(n instanceof Symbol)   return new View_Symbol((Symbol) n, parent);

		if(n instanceof MutationRequest) return new View_Mutation((MutationRequest) n, parent);

		return new View_Unknown(n, parent);
	}

	static protected final ArrayList<View> recycledViews = new ArrayList<>();
	static protected void recycleView(View v) { if(v != null) recycledViews.add(v); }
	static public void destroyRecycledViewsOf(Node n) {
		if(n == null) return;
		for(Iterator<View> i = recycledViews.iterator(); i.hasNext();) {
			View v = i.next();
			if(v.getModel() == n) {
				v.destroy();
				i.remove();
			}
		}
	}

	static protected class ViewReplacement {
		Node oldModel, newModel;
		View oldView, newView;
		ViewReplacement() {}
		ViewReplacement(Node oldModel, Node newModel, View oldView, View newView) { this.oldModel = oldModel; this.newModel = newModel; this.oldView = oldView; this.newView = newView; }
	}

	static void drawRect(GL2 gl, float x1, float y1, float x2, float y2, boolean fill) {
		gl.glBegin(fill ? GL2.GL_QUADS : GL2.GL_LINE_LOOP);
			gl.glVertex2f(x1+.5f, y1+.5f);
			gl.glVertex2f(x2+.5f, y1+.5f);
			gl.glVertex2f(x2+.5f, y2+.5f);
			gl.glVertex2f(x1+.5f, y2+.5f);
		gl.glEnd();
	}

	// instance //////////////////////////////////////////////////////////

	ParentOfView parent;
	Animator animator;

	protected float liveX = Float.MAX_VALUE, liveY, liveW, liveH;
	protected float goalX, goalY, goalW, goalH;
	protected int internalPadding_left; // Patch resultEditor uses left margin for status display
	protected int internalMargin_top; // Metadata drawn in top margin
	protected int internalMargin_right; // amount Metadata-width overhangs main width

	protected boolean isLayoutValid;
	boolean highlight, hoverHighlight;
	boolean slidewaysMode; // animation tweening mode: lock Y value & teleport at edges
	boolean isSubstructureOpen = false;

	PatchEditor resultEditor;

	ArrayList<Face> faces = new ArrayList<>();
	int activeFace = -1;

	View metadata;

	// View //////////////////////////////////////////////////////////////

	public View(Node model, ParentOfView parent) {
		this.parent = parent;
		animator = parent.getAnimator();

		if(model != null) {
			model.addObserver(this);
			if(model.hasMetadata())
				metadata = createView(model.getMetadata(), this);
		}
	}

	public void destroy() {
		getModel().removeObserver(this);
		//destroyRecycledViewsOf(getModel());
		if(isPatchResultMode()) resultEditor.patch.removeObserver(this);
	}

	public abstract Node getModel();

	void setHighlight(boolean h) {
		highlight = h;
		if(metadata != null) metadata.setHoverHighlight(h);
	}

	void setHoverHighlight(boolean h) { hoverHighlight = h; }
	boolean isVerticalLayout() { return false; }
	void setVerticalLayout(boolean enabled) {}
	void setSubstructureOpen(boolean open) {
		if(open == isSubstructureOpen) return;
		isSubstructureOpen = open;
		//invalidateLayout();
	}

	boolean isPatchResultMode() { return resultEditor != null; }
	void enablePatchResultMode(Patch p) {
		Class<? extends PatchEditor>[] editors = Reflection.getAllProperSublassesOf("jiyuiydi.infra", PatchEditor.class);
		for(Class<? extends PatchEditor> i : editors) {
			SubclassSpecialty ss = i.getAnnotation(SubclassSpecialty.class);
			if(ss == null) continue;
			if(ss.value().isAssignableFrom(p.getClass())) {
				try {
					Constructor<? extends PatchEditor> cons = i.getConstructor(Patch.class, Node.class);
					cons.setAccessible(true);
					resultEditor = cons.newInstance(p, getModel());
					resultEditor.patch.addObserver(this);
					return;
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
		resultEditor = new PatchEditor_ForView(p, getModel());
		resultEditor.patch.addObserver(this);
	}
	Patch getPatchOrigin() { return resultEditor == null ? null : resultEditor.patch; }

	public Spine getMomentOfTree() {
		Trail p = new Trail();
		View root = this;
		while(root.parent instanceof View) {
			p.up(root.parent.indexOf(root));
			root = (View) root.parent;
		}
		Spine t = new Spine(root.getModel());
		p.rewind(t);
		return t;
	}

	public int getMinX() { return (int) goalX; }
	public int getMinY() { return (int) goalY; }
	public int getMaxX() { return (int) (goalX + goalW); }
	public int getMaxY() { return (int) (goalY + goalH); }
	public int getWidth () { return (int) goalW; }
	public int getHeight() { return (int) goalH; }

	public float getLiveMinX() { return liveX; }
	public float getLiveMinY() { return liveY; }
	public float getLiveMaxX() { return liveX + liveW; }
	public float getLiveMaxY() { return liveY + liveH; }

	void moveTo(int x, int y) {
		if(liveX == Float.MAX_VALUE) { // snap if first location assignment
			liveX = x; liveY = y;
			//goalX = x; goalY = y;
			//snapBy(x, y);
			//animator.register(this);
			//return;
		}
		moveBy(x - (int) goalX, y - (int) goalY);
	}

	void snapBy(int x, int y) {
		liveX += x; liveY += y;
		goalX += x; goalY += y;
		for(int i = 0; i < count(); ++i)
			get(i).snapBy(x, y);
	}

	void moveBy(int dx, int dy) {
		goalX += dx; goalY += dy;
		if(metadata != null) metadata.moveBy(dx, dy);

		//float distSquared = (goalX - liveX) * (goalX - liveX) + (goalY - liveY) * (goalY - liveY);
		//double slidewaysDistSquared = Math.pow((double) slidewaysDist(), 2.0);
		//slidewaysMode = distSquared > slidewaysDistSquared;

		animator.register(this);
	}

	void sizeTo(int width, int height) {
		if(liveW == 0 && liveH == 0) { // first size assignment
			liveW = width; liveH = height;
		}
		goalW = width; goalH = height;
		animator.register(this);
	}

	//void snap() { liveX = goalX; liveY = goalY; liveW = goalW; liveH = goalH; }

	boolean isPointInside(int x, int y) {
		return x >= liveX && x <= liveX+liveW && y >= liveY && y <= liveY+liveH;
	}

	void validateLayout(Editor context, LayoutMode mode) { if(!isLayoutValid) doLayout(context, mode); isLayoutValid = true; }
	public void revalidateLayout() { isLayoutValid = false; for(int i = 0; i < count(); ++i) get(i).revalidateLayout(); }

	public int addFace(Face f, boolean useNow) {
		faces.add(f);
		if(useNow) useFace(faces.size() - 1);
		return faces.size() - 1;
	}

	public void useFace(int index) {
		activeFace = Math.min(index, faces.size() - 1);
		revalidateLayout();
		invalidateLayout();
	}

	public void nextFace() {
		if(faces.isEmpty()) return;
		useFace((activeFace + 1) % faces.size());
	}

	public void prevFace() {
		if(faces.isEmpty()) return;
		useFace((activeFace - 1 + faces.size()) % faces.size());
	}

	void doLayout(Editor context, LayoutMode mode) {
		internalPadding_left = isCaptureEnabled() ? context.margin*4 : 0;
		internalMargin_top = internalMargin_right = 0;
		sizeTo(internalPadding_left + context.margin*2, context.margin*2);

		if(activeFace >= 0) faces.get(activeFace).doLayout(context, mode);
	}

	int getBaseline(Editor context, LayoutMode mode) {
		if(activeFace >= 0) return faces.get(activeFace).getBaseline(context, mode);
		return 0;
	}

	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		drawBackground(d, context);
		if(activeFace >= 0) faces.get(activeFace).draw(d, context, mode);
	}

	public void drawBackground(GLAutoDrawable d, Editor context) {
		final GL2 gl = d.getGL().getGL2();

		if(isCaptureEnabled()) {
			gl.glColor4f(.7f, .3f, 0f, .1f);
			drawRect(gl, liveX, liveY, liveX+liveW, liveY+liveH, true);
			double x = liveX + context.margin*2;
			double y = liveY + context.margin*2 + internalMargin_top;
			double r = context.margin;
			gl.glColor4f(.7f, .3f, 0f, 1f);
			gl.glBegin(GL2.GL_TRIANGLE_FAN);
				double pi2 = Math.PI * 2.0;
				double inc = 1.0 / r;
				gl.glVertex2d(x   +.5f, y   +.5f);
				for(double a = 0.0; a <= pi2; a += inc) {
					gl.glVertex2d(x+r*Math.cos(a) +.5f, y+r*Math.sin(a)   +.5f);
				}
			gl.glEnd();
		}

		float top = liveY + internalMargin_top;
		float right = liveX + liveW - internalMargin_right;
		if(highlight) {
			gl.glColor4f(203/255f, 226/255f, 253/255f, .8f);
			drawRect(gl, internalPadding_left + liveX, top, right, liveY+liveH, true);
		} else if(hoverHighlight) {
			gl.glColor4f(203/255f, 226/255f, 253/255f, .3f);
			drawRect(gl, internalPadding_left + liveX, top, right, liveY+liveH, true);
		}

		//gl.glColor4f(0/255f, 0/255f, 0/255f, .7f);
		//gl.glBegin(GL2.GL_LINES);
		//	gl.glVertex2f(-50+ liveX        +.5f, -50+ liveY        +.5f);
		//	gl.glVertex2f(+50+ liveX + liveW+.5f, +50+ liveY + liveH+.5f);
		//gl.glEnd();
	}

	void drawBorder(GLAutoDrawable d, Editor context) {
		final GL2 gl = d.getGL().getGL2();
		gl.glLineWidth(context.lineThickness);
		if(isPatchResultMode())
			gl.glColor4f(.7f, .3f, 0f, .7f);
		else
			gl.glColor4f(0f, .3f, .7f, .5f);
		float top = liveY + internalMargin_top;
		float right = liveX + liveW - internalMargin_right;
		if(isSubstructureOpen || getModel().count() == 0) {
			drawRect(gl, liveX, top, right, liveY+liveH, false);
		} else {
			gl.glBegin(GL2.GL_LINES);
				gl.glVertex2f(liveX +.5f, top +.5f);
				gl.glVertex2f(right +.5f, top +.5f);
			gl.glEnd();
		}
	}

	private float slidewaysDist() {
		float minX = (parent instanceof View) ? ((View)parent).goalX : 0;
		float maxX = parent.getMaxX();
		if(goalY > liveY) return (maxX - liveX) + (goalX - minX);
		else              return (minX - liveW - liveX) + (goalX - maxX);
	}

	// Animation /////////////////////////////////////////////////////////

	@Override
	public boolean animate() {
		if(slidewaysMode) {
			if(liveY != goalY) { // phase 1
				float minX = (parent instanceof View) ? ((View)parent).goalX : 0;
				float maxX = parent.getMaxX();
				if(goalY > liveY) {
					float dx = (maxX - liveX) + (goalX - minX);
					liveX += dx * tweenPercent;
					if(liveX > maxX) { liveX = minX - liveW; liveY = goalY; }
				}
				else {
					float dx = (minX - liveW - liveX) + (goalX - maxX);
					liveX += dx * tweenPercent;
					if(liveX < minX - liveW) { liveX = maxX; liveY = goalY; }
				}
			} else { // phase 2
				float dx = goalX - liveX;
				liveX += dx * tweenPercent;
				if(Math.abs(dx) < 1f) liveX = goalX;
			}
		}
		else {
			float dx = goalX - liveX;
			float dy = goalY - liveY;
			liveX += dx * tweenPercent;
			liveY += dy * tweenPercent;
			if(Math.abs(dx) < 1f) liveX = goalX;
			if(Math.abs(dy) < 1f) liveY = goalY;
		}

		float dw = goalW - liveW;
		float dh = goalH - liveH;
		liveW += dw * tweenPercent;
		liveH += dh * tweenPercent;
		if(Math.abs(dw) < 1f) liveW = goalW;
		if(Math.abs(dh) < 1f) liveH = goalH;

		return (liveX == goalX && liveY == goalY) && (liveW == goalW && liveH == goalH);
	}

	// ParentOfView //////////////////////////////////////////////////////

	@Override public int count() { return 0; }
	@Override public View get(int index) { return index == Node.INDEX_METADATA ? metadata : null; }
	@Override public Animator getAnimator() { return animator; };
	@Override public void invalidateLayout() { isLayoutValid = false; parent.invalidateLayout(); }
	@Override public int indexOf(View v) {
		if(v == metadata) return Node.INDEX_METADATA;
		for(int i = 0; i < count(); ++i) {
			if(get(i) == v) return i;
			//if(get(i) instanceof View_PatchResult && ((View_PatchResult) get(i)).resultView == v)
			//	return i;
		}
		return Node.INDEX_ERROR;
	}
	@Override public View replace(int index, View v) { return null; }

	// TextRenderingClient ///////////////////////////////////////////////

	@Override public void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c) {}

	// Observer //////////////////////////////////////////////////////////

	@Override
	public void update(UpdateMessage msg, Observable subject) {
		if(isPatchResultMode() && subject == resultEditor.patch) {
			if(msg instanceof UpdateMessage.Initialize) return;
			return;
		}

		if(isPatchResultMode()) {
			if(msg instanceof UpdateMessage.Invalidated)
				notifyObservers(msg); // forward to views
		}
	}

	// Object ////////////////////////////////////////////////////////////

	@Override public String toString() { return "View of: " + getModel().toString(); }

	// EditActionCaptureContext //////////////////////////////////////////

	@Override public void performEdit(EditAction a) {
		if(isPatchResultMode()) {
			a.forPatchEditorsOnly = true;
			if(isCaptureEnabled()) {
				a = resultEditor.performEdit(a);
				if(a == null) return;
			}
		}
		if(parent instanceof EditActionCaptureContext)
			((EditActionCaptureContext) parent).performEdit(a);
	}
	@Override public boolean isCaptureEnabled() { return resultEditor == null ? false : resultEditor.captureEnabled; }
	@Override public void setCaptureEnabled(boolean enabled) {
		if(resultEditor == null || resultEditor.captureEnabled == enabled) return;
		resultEditor.captureEnabled = enabled;
		invalidateLayout();
	}

	// modules ///////////////////////////////////////////////////////////

	static enum LayoutMode { NORMAL, METADATA, SUMMARY, MINIMIZED, }

}
