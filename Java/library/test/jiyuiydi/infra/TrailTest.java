package jiyuiydi.infra;

import static org.junit.Assert.*;

import org.junit.Test;

public class TrailTest {

	@Test
	public void testTrailFrom_Sibling() {
		Node tree = new Box("a", "b");

		Spine from = new Spine(tree);
		from.down(0); // a
		Spine to = new Spine(tree);
		to.down(1); // b

		Trail answer = new Trail();
		answer.up(0);
		answer.down(1);

		Trail path = Trail.trailFromTo(from, to);
		assertEquals(answer, path);
	}

	@Test
	public void testTrailFrom_ToSelf() {
		Node tree = new Box("a", "b");

		Spine from = new Spine(tree);
		from.down(0); // a
		Spine to = new Spine(tree);
		to.down(0); // a

		Trail answer = new Trail();

		Trail path = Trail.trailFromTo(from, to);
		assertEquals(answer, path);
	}

	@Test
	public void testTrailFrom_FromDeeper() {
		Node tree = new Box(new Box("a", "b"), "c");

		Spine from = new Spine(tree);
		from.down(0, 1); // b
		Spine to = new Spine(tree);
		to.down(1); // c

		Trail answer = new Trail();
		answer.up(1);
		answer.up(0);
		answer.down(1);

		Trail path = Trail.trailFromTo(from, to);
		assertEquals(answer, path);
	}

	@Test
	public void testTrailFrom_ToDeeper() {
		Node tree = new Box("a", new Box("b", "c"));

		Spine from = new Spine(tree);
		from.down(0); // a
		Spine to = new Spine(tree);
		to.down(1, 1); // c

		Trail answer = new Trail();
		answer.up(0);
		answer.down(1);
		answer.down(1);

		Trail path = Trail.trailFromTo(from, to);
		assertEquals(answer, path);
	}

	@Test
	public void testTrailFrom_NoPath() {
		Node tree1 = new Box("a", "b");
		Node tree2 = new Box("a", "b");

		Spine from = new Spine(tree1);
		from.down(0); // a
		Spine to = new Spine(tree2);
		to.down(1); // b

		Trail answer = null;

		Trail path = Trail.trailFromTo(from, to);
		assertEquals(answer, path);
	}

	@Test
	public void testTrailFrom_CommonAncestorIsNotRoot() {
		Node tree = new Box("a", new Box("b", "c"));

		Spine from = new Spine(tree);
		from.down(1, 1); // c
		Spine to = new Spine(tree);
		to.down(1, 0); // b

		Trail answer = new Trail();
		answer.up(1);
		answer.down(0);

		Trail path = Trail.trailFromTo(from, to);
		assertEquals(answer, path);
	}

}
