package jiyuiydi.infra.ui.opengl;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Stack;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jogamp.opengl.util.awt.TextRenderer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jiyuiydi.infra.*;
import jiyuiydi.infra.ui.Animator;
import jiyuiydi.infra.ui.opengl.BatchTextRenderer.TextRenderingClient;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;
import jiyuiydi.util.Observer;
import jiyuiydi.util.Observer.Observable;

public class Editor extends Observable implements GLEventListener, ParentOfView, EditActionCaptureContext, Observer {

	static final int  undoStackSize = 100;
	static final int  font_defaultPointSize = 24;

	static final Font font_default           = new Font("Tahoma"       , Font.TRUETYPE_FONT                          , font_defaultPointSize);
	static final Font font_small             = new Font("Tahoma"       , Font.TRUETYPE_FONT                          , font_defaultPointSize / 2);
	static final Font font_defaultMonospaced = new Font(Font.MONOSPACED, Font.TRUETYPE_FONT                          , font_defaultPointSize);
	static final Font font_defaultBoldItalic = new Font("Tahoma"       , Font.TRUETYPE_FONT | Font.ITALIC | Font.BOLD, font_defaultPointSize);
	static final Font font_quantities        = new Font(Font.MONOSPACED, Font.TRUETYPE_FONT | Font.BOLD              , font_defaultPointSize);
	static final Font font_specialChars      = new Font("Ariel"        , Font.TRUETYPE_FONT                          , font_defaultPointSize);
	static final Font font_smallSpecialChars = new Font("Ariel"        , Font.TRUETYPE_FONT                          , font_defaultPointSize / 2);

	static Image image_parameterField;

	static public void main(String[] args) {
		Editor e = new Editor();
		AsynchronousEditsQueue.instance.addObserver(e);
		e.show();
		if(args.length >= 1) e.load(new File(args[0]));
	}

	// instance //////////////////////////////////////////////////////////

	JFrame frame;
	GLCanvas canvas;
	View rootView;
	Animator animator;
	int drawCount;

	final Selection selections = new Selection();

	int margin = 5;
	int itemSpacing = 13;
	int lineThickness = 2;

	BatchTextRenderer batchTextRenderer;
	int fontIndex_defaultText;
	int fontIndex_smallText;
	int fontIndex_defaultMonospaced;
	int fontIndex_defaultTextBoldItalic;
	int fontIndex_defaultQuantity;
	int fontIndex_specialChars;
	int fontIndex_smallSpecialChars;

	Stack<EditAction> undoStack = new Stack<>();
	Stack<EditAction> redoStack = new Stack<>();

	// DataEditor ////////////////////////////////////////////////////////

	public Editor() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		//init();
		//setModel(new Box(new Box()));
	}

	private void init() {
		GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
		caps.setAlphaBits(8);

		canvas = new GLCanvas(caps);
		canvas.addGLEventListener(this);
		animator = new Animator(canvas::repaint);
		new Controller_Editor(this);

		setModel(new Box(new Box()));
		//setModel(Examples.basicPatch());
		//setModel(Examples.sickAndTired());
		//setModel(Examples.kennedyQuote());
		//setModel(new Box(Examples.loadLessThan()));
		//setModel(Examples.patchInPatch());
		//setModel(Examples.sampleTree());
		//setModel(Examples.fishRedblue());
		//setModel(Examples.pairsVsCommands());
		//setModel(Examples.demoLoad());
		//setModel(Examples.fib());
		//setModel(Examples.figure1());
		//setModel(Examples.metadata());
		//setModel(Examples.metadataMetadata());
		//setModel(Examples.paragraph());
		//setModel(Examples.namedNodes());
		//setModel(new jiyuiydi.infra.OS.FileSystems(new File("C:/Data/Projects/Programming/Infra/")));
		//setModel(new Box(new Patch(new Keyed("file"))));
		//setModel(new Box(new UTF8("hello\nmulti-line\nworld")));
		//setModel(Examples.simpleSave());
		//setModel(Examples.url());
		//setModel(Examples.nameAgeTable());
		//setModel(Examples.fileObject());
	}

	public void setModel(Node tree) {
		setSelection(null);
		Node was = null;
		if (rootView != null) {
			was = rootView.getModel();
			View.recycleView(rootView);
		}
		rootView = View.createView(tree, this);
		View.destroyRecycledViewsOf(was);
		setSelection(rootView.get(0)); // also repaints canvas
	}

	public void show() {
		if(frame != null) frame.dispose();
		frame = new JFrame("Data Editor");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) { close(); }
		});

		init();

		canvas.setSize(1280, 900);
		frame.add(canvas);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		canvas.requestFocusInWindow();
		canvas.requestFocus();
	}

	void close() {
		frame.remove(canvas);
		frame.dispose();
		rootView.destroy();
		System.exit(0);
	}

	void load(File f) {
		Path path = FileSystems.getDefault().getPath(f.getPath());
		try {
			Node tree = InfraEncoding.load(path);
			setModel(tree);
		} catch(IOException e) { e.printStackTrace(); }
	}

	void save() {
		Path path = FileSystems.getDefault().getPath("current.infra");
		try { InfraEncoding.save(rootView.getModel(), path); }
		catch (IOException e) { e.printStackTrace(); }
	}

	public void setSelection(View v) {
		selections.select(v);
		canvas.repaint();
	}

	public void addSelectionRange(int addition) {
		selections.addSelectionRange(addition);
		canvas.repaint();
	}

	public void scaleFonts(float scaleFactor) {
		batchTextRenderer.scaleFonts(scaleFactor);
		rootView.revalidateLayout();
		invalidateLayout();
	}

	public void undo() {
		if(!undoStack.empty())
			redoStack.push(undoStack.pop()).undo();
	}

	public void redo() {
		if(!redoStack.empty())
			undoStack.push(redoStack.pop()).redo();
	}

	// GLEventListener ///////////////////////////////////////////////////

	@Override
	public void init(GLAutoDrawable d) {
		batchTextRenderer = new BatchTextRenderer(canvas.getGraphics());
		fontIndex_defaultText           = batchTextRenderer.registerFont(font_default);
		fontIndex_smallText             = batchTextRenderer.registerFont(font_small);
		fontIndex_defaultMonospaced     = batchTextRenderer.registerFont(font_defaultMonospaced);
		fontIndex_defaultTextBoldItalic = batchTextRenderer.registerFont(font_defaultBoldItalic);
		fontIndex_defaultQuantity       = batchTextRenderer.registerFont(font_quantities);
		fontIndex_specialChars          = batchTextRenderer.registerFont(font_specialChars);
		fontIndex_smallSpecialChars     = batchTextRenderer.registerFont(font_smallSpecialChars);

		image_parameterField = new Image("bin/textures/parameterField.png");

		final GL gl = d.getGL();

		gl.glClearColor(1f, 1f, 1f, 1f);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_BLEND);
	}

	@Override
	public void reshape(GLAutoDrawable d, int x, int y, int width, int height) {
		final GL2 gl = d.getGL().getGL2();
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, width, height, 0, -100, 100);
		//context.glu.gluPerspective(30, context.width/(float)context.height, -100, 100);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		//gl.glTranslatef(0f, height, 0f);
		//gl.glScalef(1f, -1f, 1f);
		//gl.glTranslatef(0f, -height, 0f);
	}

	@Override
	public void display(GLAutoDrawable d) {
		AsynchronousEditsQueue.instance.applyEdits(rootView.getModel());

		if(!rootView.isLayoutValid)
			rootView.moveTo(margin, margin);
		rootView.validateLayout(this, LayoutMode.NORMAL);

		final GL2 gl = d.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		rootView.draw(d, this, LayoutMode.NORMAL);
//		gl.glViewport(0, 0, canvas.getWidth(), canvas.getHeight());
//		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
//		gl.glLoadIdentity();
//		gl.glOrtho(0, canvas.getWidth(), canvas.getHeight(), 0, -100, 100);
//		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
//		gl.glScissor(0, 0, canvas.getWidth(), canvas.getHeight());

		++drawCount;
		batchTextRenderer.requestTextRendering(fontIndex_smallText, new TextRenderingClient() {
			public void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c) {
				final String s = "Render frame " + drawCount;
				tr.draw(s, canvas.getWidth() - fm.stringWidth(s), fm.getDescent());
			}
		});

		batchTextRenderer.render(d, canvas);
	}

	@Override
	public void dispose(GLAutoDrawable d) {}

	// parentOfView //////////////////////////////////////////////////////

	@Override public int indexOf(View v) { return 0; }
	@Override public int count() { return 1; }
	@Override public View get(int index) { return rootView; }
	@Override public Animator getAnimator() { return animator; }
	@Override public void invalidateLayout() { animator.postRedisplay(); }
	@Override public int getMaxX() { return canvas.getWidth(); }
	@Override public View replace(int index, View v) { View prev = rootView; rootView = v; invalidateLayout(); return prev; }

	// EditActionCaptureContext //////////////////////////////////////////

	@Override
	public void performEdit(EditAction a) {
		if(a.forPatchEditorsOnly) return;
		a.redo(); // do for the first time
		if(!undoStack.empty() && undoStack.peek().canMerge(a))
			a = undoStack.pop().merge(a);
		undoStack.push(a);
		while(undoStack.size() > undoStackSize) undoStack.remove(0); // forget oldest undo actions
		redoStack.clear();
	}

	@Override public boolean isCaptureEnabled() { return true; }
	@Override public void setCaptureEnabled(boolean enabled) {}

	// Observer //////////////////////////////////////////////////////////

	@Override
	public void update(UpdateMessage msg, Observable subject) {
		if(subject == AsynchronousEditsQueue.instance) {
			if(msg instanceof UpdateMessage.Initialize) {}
			else {
				canvas.repaint(); // Trigger draw cycle - to pull and apply edits from queue
			}
		}
	}

}
