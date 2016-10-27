package jiyuiydi.infra.ui.opengl;

import jiyuiydi.infra.*;

public class View_Quantity extends View_UTF8 {

	Quantity num;
	String prettyPrint;

	public View_Quantity(Quantity q, ParentOfView parent) {
		super(new UTF8(String.valueOf(q.get())), parent);
		num = q;
		num.addObserver(this);
		updatePrettyString();
		if(num.hasMetadata())
			metadata = createView(num.getMetadata(), this);
		//super.model.set(String.valueOf(model.getValue()));
		addFace(new Face_Quantity(this), true);
	}

	private void updatePrettyString() {
		StringBuilder s = new StringBuilder(text.get());
		int i = text.get().length();
		if(num instanceof Float32)
			i = text.get().lastIndexOf('.');
		int first = (num.asFloat() < 0) ? 1 : 0;
		while(i-3 > first) {
			i -= 3;
			s.insert(i, ',');
		}
		prettyPrint = s.toString();
	}

	// View //////////////////////////////////////////////////////////////

	@Override public void destroy() { num.removeObserver(this); super.destroy(); }
	@Override public Quantity getModel() { return num; }

	@Override
	public void update(UpdateMessage msg, Observable subject) {
		if(subject == num) { // update text based on num
			if(msg instanceof UpdateMessage.Changed) {
				text.set(String.valueOf(num.get()));
				updatePrettyString();
				invalidateLayout();
				//text.set(s);
			}
		}

		if(subject == text) { // update num based on text
			if(msg instanceof UpdateMessage.Changed) {
				if(num instanceof Int32) ((Int32) num).set(Integer.parseInt(text.get()));
				else
				if(num instanceof Float32) ((Float32) num).set(Float.parseFloat(text.get()));
				updatePrettyString();
				invalidateLayout();
			}
		}

		super.update(msg, subject);
	}

}
