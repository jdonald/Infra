package jiyuiydi.infra;

import static org.junit.Assert.*;

import org.junit.Test;

import jiyuiydi.infra.*;

public class PatchTest {

	@Test
	public void testEmptyPatch() {
		Spine t = new Spine(new Patch());
		assertEquals("{}", t.toString());
		assertEquals(Patch.selfReferenceNode, new Patch().getResult());
		assertEquals(Patch.selfReferenceNode, new Patch().getReduced());
	}

	@Test
	public void testNoOpPatch() {
		Spine t = new Spine(new Patch(new Command(), new Command()));
		assertEquals("{NoOp() NoOp()}", t.toString());
		assertEquals("{}", t.evaluated().toString());
	}

	@Test
	public void testSimplePatchToString() {
		Patch p;
		Spine t = new Spine(new Box("hello", p = new Patch()));
		p.add(new Command(Opcode.LEFT, 1));
		assertEquals("[hello {left(1)}]", t.toString());
	}

	@Test
	public void testPatchReduceSimpleNavigation() {
		Spine t = new Spine(new Box("hello", new Patch(new Command(Opcode.LEFT, 1))));
		t.down(1);
		t.evaluated();
		assertEquals("[hello hello]", t.root().toString());
	}

	@Test
	public void testPatchNavigation() {
		Node tree = new Box(new Box("hello"), new Box(new Patch(
				new Command(Opcode.UP),
				new Command(Opcode.LEFT, 1),
				new Command(Opcode.DOWN, 0)
				)));
		Spine t = new Spine(tree);
		t.down(1, 0);
		t.evaluated();
		assertEquals("[[hello] [hello]]", t.root().toString());
	}

	@Test
	public void testInsertIntoEmptyList() {
		{
		Patch p = new Patch(
				new Command(Opcode.VALUE, new Box()),
				new Command(Opcode.DOWN, 0),			// 0
				new Command(Opcode.INSERT, "hi"),		// insert
				new Command(Opcode.UP)
				);
		assertEquals("[hi]", p.getResult().toString());
		}
		{
		Patch p = new Patch(
				new Command(Opcode.VALUE, new Box()),
				new Command(Opcode.DOWN, -1),			// at end
				new Command(Opcode.INSERT, "hi"),		// insert
				new Command(Opcode.UP)
				);
		assertEquals("[hi]", p.getResult().toString());
		}
	}

	@Test
	public void testAutoParams() {
		Patch p = new Patch(new Command("if"));
		Spine s = new Spine(p);
		assertEquals("{if _ then _ else _}", s.reduced(false).toString());

		p = new Patch(new Command("if", new Symbol.True(), "yes"));
		s = new Spine(p);
		assertEquals("yes", s.evaluated().toString());
	}

	@Test
	public void testRecursiveEval() {
		Patch p1, p2;
		Spine s = new Spine(new Box("hi", "bye", p1 = new Patch(), p2 = new Patch()));
		p1.add(new Command(Opcode.LEFT, 2));
		p2.add(new Command(Opcode.LEFT, 1));
		s.down(3);
		assertEquals("hi", s.evaluated().toString());
	}

	@Test
	public void testIndirectSelfReference() {
		Spine s = new Spine(new Box(new Patch(new Command(Opcode.RIGHT, 1)), new Patch(new Command(Opcode.LEFT, 1))));
		assertEquals("[{right(1)} {left(1)}]", s.toString());
		s.reduced(true);
		assertEquals("[{} {}]", s.toString());
	}

	@Test
	public void testLongIndirectSelfReference() {
		Keyed rt1 = new Command(Opcode.RIGHT, 1);
		Keyed lt3 = new Command(Opcode.LEFT, 3);
		Spine s = new Spine(new Box(new Patch(rt1), new Patch(rt1), new Patch(rt1), new Patch(lt3)));
		//s.down(2); s.evaluated(); s.up();
		s.reduced(true);
		assertEquals("[{} {} {} {}]", s.toString());
	}

	@Test
	public void testNestedPatch() { // they must be given a home (setHome) by their Patch parent
		Spine s = new Spine(new Box("goal", new Patch(new Command(Opcode.VALUE, new Patch(
				new Command(Opcode.UP, 2),
				new Command(Opcode.LEFT, 1)
				)))));
		s.down(1);
		s.reduced(false);
		assertEquals("goal", s.get().toString());
	}

	@Test
	public void testNestedInstructions() {
		Spine s = new Spine(new Box(
				new Box("A", new Box("B")),
				new Patch(
						new Command(Opcode.LEFT, 1),
						new Box(new Command(Opcode.DOWN, 1, 0), new Command(Opcode.WRITE, "C"))
						)
				));
		s.reduced(true);
		assertEquals("[[A [B]] [A [C]]]", s.toString());
	}

	@Test
	public void testID() {
		Spine s = new Spine(new Box(
				new Int32(5).with(Metadata.lang_ID, "x"),
				new Patch(new Command(Opcode.ID, "x"))
				));
		s.reduced(true);
		assertEquals("[5 5]", s.toString());

		s = new Spine(new Box(
				new Int32(5).with(Metadata.lang_ID, "o"),
				new Patch(new Command(Opcode.ID, "x"))
				));
		s.down(1);
		assertEquals(new Symbol.Error(), s.evaluated());
		assertEquals("error w/(Patch:[ID x `not found'])", s.evaluated().toString());
	}

//	@Test
//	public void testClone() {
//		Patch p1, p2;
//		Node tree = new Box(1, p1 = new Patch(), 2, p2 = new Patch());
//		p1.addCommand(Opcode.LEFT, 1);
//		p2.addCommand(Opcode.LEFT, 2); p2.addCommand(Opcode.CLONE);
//
//		Spine s = new Spine(tree);
//		s.down(3); s.reduce();
//		assertEquals("2", s.get().toString());
//	}

	@Test
	public void testCurryingAndObservations() {
		Spine s = new Spine(new Patch(new Command("+", new Patch(new Command("-", 6,3)))));
		s.reduced(false);
		assertEquals("{3 + _}", s.toString());
		//assertEquals("ERROR", s.evaluated().toString());
		//s.down(2);
		s.replace(2, new Int32(7)); // should clear {3 + _}'s result
		//s.up();
		s.reduced(false);
		assertEquals("10", s.toString());
	}

//	@Test
//	public void testFib() {
//		Patch posA = new Patch(new Command(Opcode.UP, 5), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
//		Patch posB = new Patch(new Command(Opcode.UP, 3), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
//		Patch posC = new Patch(new Command(Opcode.UP, 10), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
//		Patch recurseMinus1 = new Patch(
//				new Command(Opcode.UP, 6),
//				new Command(Opcode.CLONE),
//				new Box(
//					new Command(Opcode.DOWN_NOEVAL, 0, 1),
//					new Command(Opcode.WRITE, new Patch(new Command("-", posC, 1)))
//				)
//				);
//		Patch recurseMinus2 = new Patch(
//				new Command(Opcode.UP, 6),
//				new Command(Opcode.CLONE),
//				new Box(
//					new Command(Opcode.DOWN_NOEVAL, 0, 1),
//					new Command(Opcode.WRITE, new Patch(new Command("-", posC, 2)))
//				)
//				);
//
//		int x = 10;
//		Node tree = new Patch(
//				new Command(Opcode.VALUE, x),
//				new Command(Opcode.VALUE, new Patch(new Command("if", new Patch(new Command("<", posA, 2)), posB, new Patch(new Command("+", recurseMinus1, recurseMinus2))))));
//
//		Spine s;
//
//		//s = new Spine(tree);
//		//s.downNoEval(1,1,0,1,0,1); // check arg ref posA
//		//s.reduce();
//		//assertEquals(String.valueOf(x), s.get().toString());
//
//		//s = new Spine(tree);
//		//s.downNoEval(1,1,0,2); // check arg ref posB
//		//s.reduce();
//		//assertEquals(String.valueOf(x), s.get().toString());
//
//		//s = new Spine(tree);
//		//s.downNoEval(1,1,0,3,0,1,3,1,0,1); // check arg ref posc
//		//s.reduce();
//		//assertEquals(String.valueOf(x), s.get().toString());
//
//		//s = new Spine(tree);
//		//s.downNoEval(1,1,0,1); // {x < 2}
//		//s.reduce();
//		//assertEquals(x<2?"true":"false", s.get().toString());
//
//		//s = new Spine(tree);
//		//s.downNoEval(1,1,0,3); // {+(,)}
//		//s.downNoEval(0,1); // clone write x-1
//		//s.eval(); // fib1
//		//s.downNoEval(0,1); // x-1
//		//s.reduce();
//		//assertEquals(String.valueOf((float)(x-1)), s.get().toString());
//
//		s = new Spine(tree);
//		long startTime = System.currentTimeMillis();
//		s.reduce();
//		long time = System.currentTimeMillis() - startTime;
//		//System.out.println("Patch fib(" + x + ") took " + time + "ms");
//		startTime = System.currentTimeMillis();
//		long answer = fib(x);
//		time = System.currentTimeMillis() - startTime;
//		//System.out.println("Java fib(" + x + ")="+answer+" took " + time + "ms");
//		assertEquals(String.valueOf(answer), s.toString());
//	}

//	private long fib(long i) { return i < 2 ? i : fib(i-1)+fib(i-2); }

//	@Test
//	public void testNetworkedFib() {
//		Patch posA = new Patch(new Command(Opcode.UP, 5), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
//		Patch posB = new Patch(new Command(Opcode.UP, 3), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
//		Patch posC = new Patch(new Command(Opcode.UP, 10), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
//		Patch recurseUpRight0 = new Patch(
//				new Command(Opcode.UP, 3), //recurseMinus1
//				new Command(Opcode.RIGHT, 1), //recurseMinus2
//				new Command(Opcode.EVAL)
//				//new Command(Opcode.DOWN_NOEVAL, 2, 1, 1)
//				);
//		Patch recurseMinus1 = new Patch(
//				new Command(Opcode.UP, 6),
//				new Command(Opcode.CLONE),
//				new Box(
//					new Command(Opcode.DOWN_NOEVAL, 0, 1),
//					new Command(Opcode.WRITE, new Patch(new Command("-", posC, 1)))
//				)
//				//new Box(
//				//		new Command(Opcode.DOWN_NOEVAL, 1, 1, 0, 3, 0, 1), //recurseMinus1
//				//		new Command(Opcode.WRITE_CLONE, recurseUpRight0)
//				//	)
//				);
//		Patch recurseMinus2 = new Patch(
//				new Command(Opcode.UP, 6),
//				new Command(Opcode.CLONE),
//				new Box(
//					new Command(Opcode.DOWN_NOEVAL, 0, 1),
//					new Command(Opcode.WRITE, new Patch(new Command("-", posC, 2)))
//				)
//				);
//
//		int x = 2;
//		Node tree = new Patch(
//				new Command(Opcode.VALUE, Symbol.paramNode),
//				new Command(Opcode.VALUE, new Patch(new Command(
//						"if",
//						new Patch(new Command("<", posA, 2)),
//						/*then*/ posB,
//						/*else*/ new Patch(new Command("+", recurseMinus1, recurseMinus2))))));
//
//		Spine s;
//		s = new Spine(tree);
//
//		//s.downNoEval(1,1);
//		//s.eval(); // {if _ then _ else _}
//		//s.eval(); // then
//		//s.eval(); // {_ + _}
//		//s.downNoEval(0);
//		//s.eval();
//		//s.downNoEval(0, 1);
//		//s.eval(); // {_ - _}
//		//s.eval(); // 1.0
//		//assertEquals("", s.toString());
//
//		s.reduce();
//		assertEquals(String.valueOf(fib(x)), s.toString());
//	}

//	@Test
//	public void testPatchEvalIdentity() {
//		Tree t = new Tree("hello", new Patch());
//		
//		t.down(1);
//		t.evaluate(true);
//		assertEquals("[hello {}]", t.root().toString());
//	}
//	
//	@Test
//	public void testInfiniteLoop() {
//		Patch p;
//		Tree t = new Tree("A", p = new Patch());
//		p.addCommand(Opcode.UP);
//		t.down(1);
//		t.evaluate();
//		assertEquals("[A [âˆž]]", t.root().toString());
//	}
//	
//	@Test
//	public void testSimpleWrite() {
//		Patch p;
//		Tree t = new Tree("hello", p = new Patch());
//		t.down(1);
//
//		p.addCommand(Opcode.RIGHT, -1);
//		t.evaluate(true);
//		assertEquals("[hello hello]", t.root().toString());
//
//		p.addCommand(Opcode.WRITE, "goodbye");
//		t.evaluate(true);
//		assertEquals("[hello goodbye]", t.root().toString());
//	}
//	
//	@Test
//	public void testTreeWrite() {
//		Patch p;
//		Tree t = new Tree(
//			new Box(
//				new Box(
//						new Box("000", "001"),
//						new Box("010", "011")
//				),
//				new Box(
//						new Box("100", "101"),
//						new Box("110", "111")
//				)
//			),
//			p = new Patch()
//		);
//		assertEquals("[[[[000 001] [010 011]] [[100 101] [110 111]]] {}]", t.toString());
//		t.down(1);
//
//		p.addCommand(Opcode.RIGHT, -1);
//		p.addCommand(Opcode.DOWN, new Box(0, 1, 1));
//		t.evaluate(true);
//		assertEquals("011", t.toString());
//		
//		p.addCommand(Opcode.WRITE, "ABB");
//		t.evaluate(true);
//		assertEquals("ABB", t.toString());
//		
//		p.addCommand(Opcode.UP, 3);
//		t.evaluate(true);
//		assertEquals("[[[000 001] [010 ABB]] [[100 101] [110 111]]]", t.toString());
//		
//		p.addCommand(Opcode.DOWN, new Box(1, 1, 0));
//		p.addCommand(Opcode.WRITE, "BBA");
//		
//		p.addCommand(Opcode.UP, 3);
//
//		t.evaluate(true);
//		assertEquals("[[[000 001] [010 ABB]] [[100 101] [BBA 111]]]", t.toString());
//	}
//
//	@Test
//	public void testWriteImmediate() {
//		Patch p;
//		Tree t = new Tree(p = new Patch());
//		p.addCommand(Opcode.WRITE, 42);
//		assertEquals("{WRITE(42)}", t.toString());
//		t.evaluate(true);
//		assertEquals("42", t.toString());
//	}
//	
//	@Test
//	public void testSelect() {
//		Patch p;
//		Tree t = new Tree(p = new Patch(), new Box("A", "B", "C"));
//		p.addCommand(Opcode.RIGHT, 1);
//		p.addCommand(Opcode.SELECT, 0); p.addCommand(Opcode.WRITE, 1);
//		p.addCommand(Opcode.SELECT, 1); p.addCommand(Opcode.WRITE, 2);
//		
//		t.down(0);
//		t.evaluate(true);
//		assertEquals("[1 2 C]", t.toString());
//	}
//
//	@Test
//	public void testInsert() {
//		Patch p;
//		Tree t = new Tree(p = new Patch(), new Box("B", "D", "F"));
//		t.down(0);
//		p.addCommand(Opcode.RIGHT, 1);
//
//		p.addCommand(Opcode.SELECT, 1); p.addCommand(Opcode.INSERT, "C");
//		t.evaluate(true); assertEquals("[B C D F]", t.toString());
//		
//		p.addCommand(Opcode.SELECT, 0); p.addCommand(Opcode.INSERT, "A");
//		t.evaluate(true); assertEquals("[A B C D F]", t.toString());
//		
//		p.addCommand(Opcode.SELECT, -1); p.addCommand(Opcode.INSERT, "G");
//		t.evaluate(true); assertEquals("[A B C D F G]", t.toString());
//		
//		p.addCommand(Opcode.SELECT, 4); p.addCommand(Opcode.INSERT, "E");
//		t.evaluate(true); assertEquals("[A B C D E F G]", t.toString());
//	}
//	
//	@Test
//	public void testIndexingFromTheEnd() {
//		Patch p; Int32 i1;
//		Tree t = new Tree(p = new Patch(), new Box("-3", "-2", "-1"));
//		p.addCommand(Opcode.RIGHT, 1);
//		p.addCommand(Opcode.DOWN, i1 = new Int32(-3)); // down negative
//		
//		t.down(0);
//		t.evaluate(true);
//		assertEquals("-3", t.toString());
//		
//		i1.value = -1;
//		t.evaluate(true);
//		assertEquals("-1", t.toString());
//		
//		t.up();
//		t.add(p = new Patch());
//		p.addCommand(Opcode.RIGHT, -1);
//		p.addCommand(Opcode.SELECT, -2); // select negative
//		p.addCommand(Opcode.WRITE);
//		
//		t.down(2);
//		t.evaluate(true);
//		assertEquals("[-3 -1]", t.toString());
//	}
//	
//	@Test
//	public void testInnerBox() {
//		Patch p; Box b1;
//		Tree t = new Tree("The", "whole", new Box("nested", "box"), "sentence");
//		t = new Tree(p = new Patch(), t);
//		p.addCommand(Opcode.RIGHT, 1);
//		
//		p.add(b1 = new Box());
//			b1.addCommand(Opcode.DOWN, new Box(2, 1));
//			b1.addCommand(Opcode.WRITE, "list");
//		
//		t.down(0);
//		t.evaluate(true);
//		assertEquals("[The whole [nested list] sentence]", t.toString());
//	}
//	
//	@Test
//	public void testMove() {
//		Patch p; Box b1;
//		Tree t = new Tree(new Box("Ask", "not", "what", "your", "country", "can", "do", "for", "you", ","), p = new Patch());
//		assertEquals("[[Ask not what your country can do for you ,] {}]", t.toString());
//		p.addCommand(Opcode.RIGHT, -1);
//		
//		p.add(b1 = new Box());
//			b1.addCommand(Opcode.DOWN); // DOWN(0)
//			b1.addCommand(Opcode.SELECT, 0);
//			b1.addCommand(Opcode.WRITE, 'a');
//		
//		p.addCommand(Opcode.SELECT, 1);
//		p.addCommand(Opcode.WRITE);
//		
//		p.addCommand(Opcode.SELECT, -2);
//		p.addCommand(Opcode.MOVE_TO, 2);
//
//		p.addCommand(Opcode.SELECT, new Box(3,4));
//		p.addCommand(Opcode.MOVE_BY, 3);
//		
//		p.addCommand(Opcode.SELECT, -1);
//		p.addCommand(Opcode.WRITE, '.');
//		
//		t.down(1);
//		t.evaluate(true);
//		assertEquals("[[Ask not what your country can do for you ,] [ask what you can do for your country .]]", t.root().toString());
//	}
//
//	@Test
//	public void testKeyedSelector() {
//		Patch p;
//		Tree t = new Tree(
//			p = new Patch(),
//			new Box(
//					new Keyed("A", "apple"),
//					new Keyed("B", "banana"),
//					new Keyed("C", "cantaloupe"),
//					new Keyed("D", "date")
//			)
//		);
//		p.addCommand(Opcode.RIGHT, 1);
//		p.addCommand(Opcode.DOWN, new Keyed("C"));
//		
//		t.down(0);
//		t.evaluate(true);
//		assertEquals("cantaloupe", t.toString());
//	}
//
//	@Test
//	public void testRecursiveEval() {
//		Tree t =
//			new Tree(
//				new Patch(new Command(Opcode.RIGHT, 1)),
//				new Patch(new Command(Opcode.WRITE, "hi"))
//			);
//		t.down(0); // down to left patch
//		t.evaluate(); // run it - triggering the right one to background eval
//		assertEquals("hi", t.toString());
//		assertEquals("[hi {WRITE(hi)}]", t.root().toString());
//		t.sibling(+1);
//		t.evaluate(); // quick eval with cached output from background eval
//		assertEquals("[hi hi]", t.root().toString());
//	}
//
//	@Test
//	public void testInnerPatch() {
//		Tree t = new Tree(
//				new Patch(
//						new Patch(
//								new Command(Opcode.UP),
//								new Command(Opcode.RIGHT, 1)
//								)
//						),
//				new Command(Opcode.WRITE, "wow")
//				);
//		t.down(0);
//		t.evaluate();
//		assertEquals("wow", t.toString());
//	}
//	
//	@Test
//	public void testSubCommandPatch() {
//		// A patch within a subtree of a command evaluated by a Patch's execution
//		Tree t = new Tree(
//				new Patch(
//						new UTF8("immediate"),
//						new Command(
//								Opcode.WRITE,
//								new Patch(
//										new Command(Opcode.UP),
//										new Command(Opcode.RIGHT, -1) // reference 'immediate'
//										)
//								)
//						)
//				);
//		t.evaluate();
//		assertEquals("immediate", t.toString());
//	}
//	
//	@Test
//	public void testLoading_writeArguments() { // >
//		Patch p;
//		Tree t = new Tree(p = new Patch());
//		p.addCommand(Opcode.LOAD, new UTF8(">"));
//		t.evaluate(true);
//		assertEquals("{1 > 0}", t.toString());
//		Int32 a, b;
//		p.addCommand(Opcode.SELECT, 0);
//		p.addCommand(Opcode.WRITE, a = new Int32(-3));
//		p.addCommand(Opcode.SELECT, 2);
//		p.addCommand(Opcode.WRITE, b = new Int32(9));
//		t.evaluate(true);
//		assertEquals("{-3 > 9}", t.toString());
//		t.evaluate();
//		assertEquals("FALSE", t.toString());
//		a.value = 4; b.value = 3;
//		t.evaluate(true);
//		assertEquals("{4 > 3}", t.toString());
//		t.evaluate();
//		assertEquals("TRUE", t.toString());
//	}
//
//	@Test
//	public void testLoading_patchInArguments() { // >
//		Patch p;
//		Tree t = new Tree(111, 222, p = new Patch());
//		p.addCommand(Opcode.LOAD, new UTF8(">"));
//		p.addCommand(Opcode.SELECT, 0);
//		p.addCommand(Opcode.WRITE, new Patch(
//										new Command(Opcode.UP, 2),
//										new Command(Opcode.RIGHT, -1)
//				));
//		p.addCommand(Opcode.SELECT, 2);
//		p.addCommand(Opcode.WRITE, new Patch(
//										new Command(Opcode.UP, 2),
//										new Command(Opcode.RIGHT, -2)
//				));
//		t.down(2);
//		t.evaluate();
//		assertEquals("{222 > 111}", t.toString());
//		t.evaluate();
//		assertEquals("TRUE", t.toString());
//	}
//	
//	@Test
//	public void testDataLiteral() {
//		Patch p;
//		Tree t = new Tree(p = new Patch());
//		p.addCommand(Opcode.DATA, new Command(Opcode.WRITE, "literal"));
//		p.addCommand(Opcode.SELECT, 1);
//		p.addCommand(Opcode.WRITE, "boom");
//		t.evaluate();
//		assertEquals("WRITE(boom)", t.toString());
//
//		//Tree t = new Tree(new Box(4, 2), new Patch(
//		//		new Command(Opcode.DATA, new Patch(new Command(Opcode.UP, 2), new Command(Opcode.RIGHT, -1))),
//		//		new Command(Opcode.SELECT, 0),
//		//		new Command(Opcode.WRITE, 9)
//		//		));
//		//t.down(1);
//		//t.evaluate();
//		//assertEquals("[[4 2] [9 2]]", t.root().toString()); // ensure that the original (external value) was not altered
//	}
//
////	@Test
////	public void testHome() {
////		Patch p;
////		Tree t = new Tree("A", p = new Patch());
////		p.addCommand(Opcode.RIGHT, -1);
////		t.down(1);
////		t.evaluate(true);
////		assertEquals("A", t.toString());
////
////		p.addCommand(Opcode.HOME);
////		t.evaluate(true);
////		assertEquals("{RIGHT(-1) HOME()}", t.toString());
////		
////		p.addCommand(Opcode.DOWN, new Box(1, 1));
////		t.evaluate(true);
////		assertEquals("â�š", t.toString());
////		p.addCommand(Opcode.HOME);
////		t.evaluate(true);
////		assertEquals("{RIGHT(-1) HOME() DOWN([1 1]) HOME()}", t.toString());
////	}
////	
////	@Test
////	public void testHomeFromAway() {
////		Patch p;
////		Tree t = new Tree(p = new Patch());
////		p.addCommand(Opcode.LOAD, new Box('>'));
////		t.evaluate();
////		assertEquals("{1 > 0}", t.toString());
////		p.addCommand(Opcode.HOME);
////		t.evaluate(true);
////		assertEquals("{LOAD([>]) HOME()}", t.toString());
////		p.addCommand(Opcode.DATA, new Box("A", "B", "C"));
////		t.evaluate(true);
////		assertEquals("[A B C]", t.toString());
////		p.addCommand(Opcode.HOME);
////		t.evaluate(true);
////		assertEquals("{LOAD([>]) HOME() DATA([A B C]) HOME()}", t.toString());
////	}
//	
////	@Test
////	public void testLocalVariable() {
////		Patch p;
////		Tree t = new Tree(p = new Patch());
////		p.addCommand(Opcode.DATA, 4);
////		p.addCommand(Opcode.WRITE, 6);
////		t.evaluate(true);
////		assertEquals("6", t.toString());
////		
////		p.addCommand(Opcode.DATA, new Patch(
////											new Command(Opcode.UP),
////											new Command(Opcode.RIGHT, -2),
////											new Command(Opcode.DOWN, 1)
////										));
////		p.addCommand(Opcode.WRITE, 8);
////		t.evaluate(true);
////		assertEquals("8", t.toString());
////	}
//	
//	@Test
//	public void testLoadInABox() {
//		Patch p;
//		Tree t = new Tree(p = new Patch(), new Box("A", "B", "C"));
//		p.addCommand(Opcode.RIGHT, 1);
//		p.addCommand(Opcode.DOWN, 1); // B
//		p.add(new Box(
//				//new Command(Opcode.HOME)
//				new Command(Opcode.LOAD, new Box(">"))
//				));
//		t.down(0);
//		t.evaluate();
//		assertEquals("B", t.toString());
//	}
//	
//	@Test
//	public void testConditional() {
//		Tree.patchMap.clear();
//		Patch p, ifStmnt;
//		Command inequality;
//		Tree t = new Tree(5, 6, p = new Patch());
//		t.down(2); // to the patch
//		p.addCommand(Opcode.WRITE, "greetings");
//		
//		p.add(ifStmnt = new Patch());
//			ifStmnt.addCommand(Opcode.LOAD, new UTF8("if")); // {0:if 1:A 2:then 3:B 4:else 5:C}
//			ifStmnt.addCommand(Opcode.SELECT, 1);
//			ifStmnt.addCommand(Opcode.WRITE, new Patch(
//					inequality = new Command(Opcode.LOAD, new UTF8("<")),
//					new Command(Opcode.SELECT, 0), new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 6), new Command(Opcode.DOWN, 0))),
//					new Command(Opcode.SELECT, 2), new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 6), new Command(Opcode.DOWN, 1)))
//					));
//			ifStmnt.addCommand(Opcode.SELECT, 3);
//			ifStmnt.addCommand(Opcode.WRITE, /*new Box*/(new Command(Opcode.WRITE, "hi")));
//		t.down(1); // to the if-statement constructions
//			assertEquals("{LOAD(if) SELECT(1) WRITE({LOAD(<) SELECT(0) WRITE({UP(6) DOWN(0)}) SELECT(2) WRITE({UP(6) DOWN(1)})}) SELECT(3) WRITE(WRITE(hi))}", t.toString());
//			t.evaluate(); // eval to get the concrete if-statement
//				assertEquals("{if {5 < 6} then WRITE(hi) else []}", t.toString());
//				t.evaluate(); // eval the if-statement
//					assertEquals("WRITE(hi)", t.toString());
//			t.unevaluate(true); // roll back to if-statement construction
//			assertEquals("{LOAD(if) SELECT(1) WRITE({LOAD(<) SELECT(0) WRITE({UP(6) DOWN(0)}) SELECT(2) WRITE({UP(6) DOWN(1)})}) SELECT(3) WRITE(WRITE(hi))}", t.toString());
//		t.up(); // to whole patch
//
//		//t.down(2);
//		//t.evaluate();
//		//assertEquals("{WRITE(greetings) WRITE(hi)}", t.toString());
//		t.evaluate();
//		assertEquals("hi", t.toString());
//
//		t.unevaluate(false);
//		assertEquals("{WRITE(greetings) {LOAD(if) SELECT(1) WRITE({LOAD(<) SELECT(0) WRITE({UP(6) DOWN(0)}) SELECT(2) WRITE({UP(6) DOWN(1)})}) SELECT(3) WRITE(WRITE(hi))}}", t.toString());
//		inequality.replace(1, ">");
//		assertEquals("{WRITE(greetings) {LOAD(if) SELECT(1) WRITE({LOAD(>) SELECT(0) WRITE({UP(6) DOWN(0)}) SELECT(2) WRITE({UP(6) DOWN(1)})}) SELECT(3) WRITE(WRITE(hi))}}", t.toString());
//		//t.unevaluate(true);
//		//assertEquals("{if {5 > 6} then WRITE(hi) else []}", t.toString());
//		t.evaluate(true);
//		//assertEquals("{if {5 > 6} then [WRITE(hi)] else []}", t.toString());
//		//t.evaluate();
//		assertEquals("greetings", t.toString());
//	}
//
//	@Test
//	public void testLocalVariable() {
//		Patch p;
//		Tree t = new Tree(p = new Patch(), "distraction");
//		t.down(0);
//		
//		p.addCommand(Opcode.NOOP, "variable");
//		//p.addCommand(Opcode.RIGHT, 1);
//		//t.evaluate(true); assertEquals("distraction", t.toString());
//		
//		// write stmt
//		p.add(new Box(
//				//new Command(Opcode.HOME),
//				new Command(Opcode.DOWN, new Box(0, 1)),
//				new Command(Opcode.SELECT, -1),
//				new Command(Opcode.INSERT, "A"),
//				new Command(Opcode.UP)
//				));
//		int nextI = p.count();
//
//		// return stmt
//		//p.addCommand(Opcode.HOME);
//		p.addCommand(Opcode.DOWN, new Box(0, 1));
//		
//		t.evaluate(true); assertEquals("variableA", t.toString());
//		
//		// insert a follow-up write stmt
//		p.insert(nextI++, new Box(
//				//new Command(Opcode.HOME),
//				new Command(Opcode.DOWN, new Box(0, 1)),
//				new Command(Opcode.SELECT, -1),
//				new Command(Opcode.INSERT, "B")
////				new Command(Opcode.DATA, "distraction")
//				));
//
//		t.evaluate(true); assertEquals("variableAB", t.toString());
//	}
//	
//	@Test
//	public void testImplementMax() {
//		Patch p;
//		Box data;
//		Tree t = new Tree(p = new Patch(), data = new Box(6, 4, 8, 7, 1));
//		t.down(0);
//		
//		p.add(new Keyed("next index", 0));
//		p.add(new Keyed("max so far", -999));
//		
//		for(int i = 0; i < data.count(); ++i) { // unrolled loop
//		p.add(new Patch(
//				new Command(Opcode.LOAD, new Box("if")),
//				new Command(Opcode.SELECT, 1),
//				new Command(Opcode.WRITE, new Patch(
//						new Command(Opcode.LOAD, new Box(">")),
//						new Command(Opcode.SELECT, 0),
//						new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 6), new Command(Opcode.DOWN, new Box(1, i)))),
//						new Command(Opcode.SELECT, 2),
//						new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 5), new Command(Opcode.DOWN, new Box(1, 1))))
//						)),
//				new Command(Opcode.SELECT, 3),
//				new Command(Opcode.WRITE, new Box(
//						new Command(Opcode.DOWN, new Box(1, 1)),
//						new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 6), new Command(Opcode.DOWN, new Box(1, i))))
//						))
//				));
//		}
//
//		p.addCommand(Opcode.DOWN, new Box(1));
//		
////		t.down(2); t.evaluate(); t.up();
////		t.down(3); t.evaluate(); t.up();
////		t.down(4); t.evaluate(); t.up();
////		t.down(5); t.evaluate(); t.up();
////		t.down(6); t.evaluate(); t.up();
//		
//		t.evaluate(true);
//		assertEquals("`max so far'(9)", t.toString());
//	}
//	
//	@Test
//	public void testFib() {
//		Box fib = new Box(3, new Patch(new Command(Opcode.DATA,
//				new Patch(
//						new Command(Opcode.LOAD, "if"),
//						new Command(Opcode.SELECT, 1),
//						new Command(Opcode.WRITE, new Patch(
//								new Command(Opcode.LOAD, "<"),
//								new Command(Opcode.SELECT, 0),
//								new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 6), new Command(Opcode.RIGHT, -1))),
//								new Command(Opcode.SELECT, 2),
//								new Command(Opcode.WRITE, 2)
//								)),
//						new Command(Opcode.SELECT, 3),
//						new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 4), new Command(Opcode.RIGHT, -1))),
//						new Command(Opcode.SELECT, 5),
//						new Command(Opcode.WRITE, new Patch(
//								new Command(Opcode.UP, 5),
//								new Command(Opcode.SELECT, 0),
//								new Command(Opcode.WRITE, new Patch(
//										new Command(Opcode.LOAD, "+"),
//										new Command(Opcode.SELECT, 0),
//										new Command(Opcode.WRITE, new Patch(
//												new Command(Opcode.LOAD, "-"),
//												new Command(Opcode.SELECT, 0),
//												new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 10), new Command(Opcode.RIGHT, -1))),
//												new Command(Opcode.SELECT, 2),
//												new Command(Opcode.WRITE, 1)
//												)),
//										new Command(Opcode.SELECT, 2),
//										new Command(Opcode.WRITE, new Patch(
//												new Command(Opcode.LOAD, "-"),
//												new Command(Opcode.SELECT, 0),
//												new Command(Opcode.WRITE, new Patch(new Command(Opcode.UP, 10), new Command(Opcode.RIGHT, -1))),
//												new Command(Opcode.SELECT, 2),
//												new Command(Opcode.WRITE, 2)
//												))
//										))
//								))
//						))));
//		Tree t = new Tree(fib);
//		System.out.println(t.get());
//		t.down(1);
////			//t.down(0, 1, 2, 1, 2, 1);
////			t.down(0, 1, 6, 1, 2, 1, 2, 1);
////			t.evaluate();
////			System.out.println("arg: " + t.get());
////			t.up(8);
//		t.evaluate();
//		System.out.println(t.get());
//		t.evaluate();
//		System.out.println(t.get());
////			t.down(0);
////			t.evaluate();
////			System.out.println(t.get());
////			t.down(2); t.evaluate();
////			System.out.println(t.get());
////			t.up();
////			t.up();
//		t.down(1);
//		t.evaluate();
//		System.out.println(t.get());
//		t.evaluate();
//		System.out.println(t.get());
////		t.down(1);
////		t.evaluate();
////		System.out.println(t.get());
////		t.evaluate();
////		System.out.println(t.get());
////		t.down(1);
////		t.evaluate();
////		System.out.println(t.get());
////		t.evaluate();
////		System.out.println(t.get());
//		assertEquals(1+1+2+3+5, t.get());
//	}

}
