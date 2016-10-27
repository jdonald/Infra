package jiyuiydi.infra;

import static org.junit.Assert.*;

import org.junit.Test;

public class NodeTest {

	@Test
	public void testEqualsWithMetadata() {
		Node a = new Box().with(Metadata.lang_ID, 'x');
		Node b = new Box();
		assertTrue(a.equals(b));
		assertFalse(a.equalsWithMetadata(b));
		b = b.with(Metadata.lang_version, 1);
		assertFalse(a.equalsWithMetadata(b));
		b = b.with(Metadata.lang_ID, 'x');
		a = a.with(Metadata.lang_version, 1);
		assertTrue(a.equalsWithMetadata(b)); // equals even if language channels are out of order

		a.with(Metadata.lang_comment, "hi");
		a.with(Metadata.lang_comment, "bye");
		b.with(Metadata.lang_comment, "bye");
		b.with(Metadata.lang_comment, "hi");
		assertFalse(a.equalsWithMetadata(b)); // not equal if statements within a channel are out of order
	}

	@Test
	public void testFindKeyed() {
		Keyed a, b, c, k;
		Box data = new Box(a = new Keyed("a", 1), b = new Keyed("b", 2), c = new Keyed("c", 3));
		k = data.findKeyed("b");
		assertTrue(b == k);

		b.replace(0, "B");
		c.replace(0, "b");
		k = data.findKeyed("b");
		assertTrue(c == k);
	}

}
