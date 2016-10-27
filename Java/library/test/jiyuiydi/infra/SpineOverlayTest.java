package jiyuiydi.infra;

import static org.junit.Assert.*;

import org.junit.Test;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.SpineOverlay;

public class SpineOverlayTest {

	@Test
	public void testDownModifyUp() {
		Node tree = new Box("a", new Box("b", "c"));
		SpineOverlay o = new SpineOverlay(tree);
		assertEquals("[a [b c]]", o.toString());
		o.down(1);
		assertEquals("[b c]", o.get().toString());
		o.down(1);
		assertEquals("c", o.get().toString());
		o.replace(Node.toNode("C"));
		assertEquals("C", o.get().toString());
		assertEquals("[a [b c]]", tree.toString());

		o.up();
		assertEquals("[b C]", o.get().toString());
		o.up();
		assertEquals("[a [b C]]", o.toString());
		o.down(0);
		assertEquals("a", o.get().toString());

		o.replace(Node.toNode("A"));
		assertEquals("A", o.get().toString());
		assertEquals("[a [b c]]", tree.toString());
	}

	@Test
	public void testModifyDownUp() {
		Node tree = new Box("a", new Box("b", "c"));
		SpineOverlay o = new SpineOverlay(tree);
		o.down(1);
		assertEquals("[b c]", o.get().toString());
		o.replace(new Box("B", "c"));
		assertEquals("[B c]", o.get().toString());
		assertEquals("[a [b c]]", tree.toString());
		o.down(1);
		assertEquals("c", o.get().toString());
		o.replace(Node.toNode("C"));
		o.up();
		assertEquals("[B C]", o.get().toString());
		assertEquals("[a [b c]]", tree.toString());
		o.up();
		assertEquals("[a [B C]]", o.get().toString());
		o.down(1);
		assertEquals("[B C]", o.get().toString());
	}

	@Test
	public void testCloning() {
		Box cd = new Box("c", "d");
		Box bcd = new Box("b", cd);
		Box abcd = new Box("a", bcd);
		SpineOverlay o = new SpineOverlay(abcd);
		assertEquals("[a [b [c d]]]", o.toString());
		assertTrue(abcd == o.get());

		o.replace(0, new Box("A", "A"));
		assertTrue(o.get(0) != abcd.get(0));
		assertTrue(o.get(1) == bcd);
		Node AA = o.get(0);
		o.down(0);
		o.replace(1, Node.toNode("Aa"));
		o.up();
		assertTrue(o.get(0) == AA); // modification of overlay nodes does not need further cloning

		o.down(1, 1);
		assertEquals("[c d]", o.toString());
		o.up(2);
		assertEquals("[[A Aa] [b [c d]]]", o.toString());

		o.down(1, 1, 1);
		assertEquals("d", o.toString());
		o.replace(Node.toNode("D"));
		assertEquals("D", o.toString());
		o.upToRoot();
		assertEquals("[[A Aa] [b [c D]]]", o.toString());
	}

//	@Test
//	public void testTrunkLocking() {
//		Node tree = new Box("a", new Box("b", new Box("c", "d")));
//		SpineOverlay o = new SpineOverlay(tree);
//		o.down(1, 1);
//		assertEquals("[c d]", o.toString());
//		o.capRoot();
//		assertFalse(o.up());
//		assertEquals("[c d]", o.toString());
//		o.replace(new Box("C", "D"));
//		assertEquals("[C D]", o.toString());
//		o.uncapRoot();
//		o.up();
//		assertEquals("[b [C D]]", o.toString());
//		o.upToRoot();
//		assertEquals("[a [b [C D]]]", o.finalizeSubtree().toString());
//
//		assertEquals("[a [b [c d]]]", tree.toString());
//	}

}
