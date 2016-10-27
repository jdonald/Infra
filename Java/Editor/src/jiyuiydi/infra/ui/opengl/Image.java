package jiyuiydi.infra.ui.opengl;

//import java.awt.Component;
//import java.awt.Graphics2D;
//import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
//import javax.swing.JButton;
//import javax.swing.SwingUtilities;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class Image {

//	private static BufferedImage getComponentImage(Component aComponent, Rectangle region) {
//		BufferedImage image = new BufferedImage(/*aComponent.getWidth()*/100, /*aComponent.getHeight()*/100, BufferedImage.TYPE_INT_RGB);
//		Graphics2D g2d = image.createGraphics();
//		SwingUtilities.paintComponent(g2d, aComponent, aComponent.getParent(), region);
//		g2d.dispose();
//		//Graphics g = image.getGraphics();
//		//aComponent.paint(g);
//		//aComponent.paintAll(g);
//		//SwingUtilities.paintComponent(g,aComponent,aComponent.getParent(),region);
//		//g.dispose();
//		return image;
//	}

	// instance //////////////////////////////////////////////////////////

	Texture tex;
	BufferedImage image;

	// Rendering /////////////////////////////////////////////////////////

	public Image(String filename) {
		try { tex = TextureIO.newTexture(new File(filename), false); }
		catch (GLException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}

	public Image(BufferedImage image) {
		tex = AWTTextureIO.newTexture(GLProfile.getDefault(), image, /*mipmap*/ false);
	}

//	public Image(Component c) {
//		image = getComponentImage(c, c.getBounds());
//	}

	public int getWidth () { if(tex == null) return 0; return tex.getWidth (); }
	public int getHeight() { if(tex == null) return 0; return tex.getHeight(); }
	
	public void draw(GLAutoDrawable glD, float x, float y) {
		final GL2 gl = glD.getGL().getGL2();

		if(tex == null && image != null) {
			tex = AWTTextureIO.newTexture(GLProfile.getDefault(), image, /*mipmap*/ false);
		}

		if(tex == null) return;

		//gl.glDisable(GL.GL_DEPTH_TEST);
		tex.bind(gl);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		float w = tex.getWidth(), h = tex.getHeight();
		gl.glColor3f(1f, 1f, 1f);
		gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0f, 1f); gl.glVertex2f(x  , y);
			gl.glTexCoord2f(1f, 1f); gl.glVertex2f(x+w, y);
			gl.glTexCoord2f(1f, 0f); gl.glVertex2f(x+w, y+h);
			gl.glTexCoord2f(0f, 0f); gl.glVertex2f(x  , y+h);
		gl.glEnd();
		gl.glDisable(GL2.GL_TEXTURE_2D);
		//gl.glEnable(GL.GL_DEPTH_TEST);
	}

}
