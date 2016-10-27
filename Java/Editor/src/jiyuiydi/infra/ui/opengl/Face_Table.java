package jiyuiydi.infra.ui.opengl;

import java.util.ArrayList;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class Face_Table implements Face {

	protected final View_Box vb;
	protected final ArrayList<Integer> colWidths = new ArrayList<>();
	protected final ArrayList<Integer> rowHeights = new ArrayList<>();

	Face_Table(View_Box vb) { this.vb = vb; }

	@Override
	public void doLayout(Editor context, LayoutMode mode) {
		// Establish cell dimensions
		colWidths.clear();
		rowHeights.clear();
		for(View row : vb.views) {
			rowHeights.add(context.margin * 2);
			if(row.count() == 0) {
				row.validateLayout(context, mode);
				rowHeights.set(rowHeights.size()-1, row.getHeight());
				if(colWidths.isEmpty()) colWidths.add(0);
				colWidths.set(0, Math.max(row.getWidth(), colWidths.get(0)));
			} else {
				for(int col = 0; col < row.count(); ++col) {
					View cell = row.get(col);
					cell.validateLayout(context, mode);
					rowHeights.set(rowHeights.size()-1, Math.max(rowHeights.get(rowHeights.size()-1), cell.getHeight()));
					if(colWidths.size() <= col) colWidths.add(0);
					colWidths.set(col, Math.max(cell.getWidth(), colWidths.get(col)));
				}
			}
		}

		// Add column padding
		for(int col = 0; col < colWidths.size() - 1; ++col)
			colWidths.set(col, colWidths.get(col) + context.itemSpacing);
		// Add row padding
		//for(int row = 0; row < rowHeights.size() - 1; ++row)
		//	rowHeights.set(row, rowHeights.get(row) + context.itemSpacing);

		vb.internalPadding_left = vb.isCaptureEnabled() ? context.margin*4 : 0;
		int margin = mode == LayoutMode.METADATA ? 0 : context.margin;
		int startX = vb.internalPadding_left + margin;
		int x = startX, y = margin;

		int rowNum = 0;
		for(View row : vb.views) {
			if(row.count() == 0) {
				row.moveTo(vb.getMinX() + x, vb.getMinY() + y);
			} else {
				for(int col = 0; col < row.count(); ++col) {
					View cell = row.get(col);
					cell.moveTo(vb.getMinX() + x, vb.getMinY() + y);
					x += colWidths.get(col);
				}
			}
			x = startX;
			y += rowHeights.get(rowNum++);
		}

		int width = 0; for(int i : colWidths) width += i;
		vb.sizeTo(width, y);

		if(vb.metadata != null) {
			vb.metadata.validateLayout(context, LayoutMode.METADATA);
			vb.metadata.moveTo((int) vb.goalX, (int) (vb.goalY - vb.metadata.goalH));
		}
	}

	@Override
	public int getBaseline(Editor context, LayoutMode mode) {
		return 0;
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		final GL2 gl = d.getGL().getGL2();

		if(vb.parent instanceof View) vb.drawBorder(d, context);

		int row = 0;
		float y = vb.liveY;
		for(View v : vb.views) {
			if(v.highlight) { // highlight entire row
				gl.glColor4f(203/255f, 226/255f, 253/255f, .8f);
				View.drawRect(gl, vb.liveX, y, vb.liveX + vb.liveW, y + rowHeights.get(row), true);
			}

			if(v.count() == 0) v.draw(d, context, mode);
			else
				for(int i = 0; i < v.count(); ++i)
					v.get(i).draw(d, context, mode);

			y += rowHeights.get(row);
			++row;
		}

		if(vb.isPatchResultMode())
			gl.glColor4f(.7f, .3f, 0f, .7f);
		else
			gl.glColor4f(0f, .3f, .7f, .5f);
		float x = vb.liveX;
		float top = vb.liveY;
		float bottom = top + vb.liveH;
		gl.glBegin(GL2.GL_LINES);
			for(int col = 0; col < colWidths.size() - 1; ++col) {
				x += colWidths.get(col);
				gl.glVertex2f(x +.5f, top    +.5f);
				gl.glVertex2f(x +.5f, bottom +.5f);
			}
		gl.glEnd();

		if(vb.metadata != null) vb.metadata.draw(d, context, LayoutMode.METADATA);
	}

}
