package jiyuiydi.infra;

import java.io.File;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.Command;
import jiyuiydi.infra.Keyed;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.Opcode;
import jiyuiydi.infra.Patch;
import jiyuiydi.infra.Symbol;

public class Examples {

	static public Patch helloWorldPatch() {
		return new Patch(new Command(Opcode.VALUE, new Box("Hello", "world!")));
	}

	static public Node basicPatch() {
		Patch p = new Patch();
		p.add(new Command(Opcode.RIGHT, -2));
		p.add(new Box(
				new Command(Opcode.DOWN, 0),
				new Command(Opcode.INSERT, "pre")
				));
		return new Box("Hello", new Box("groovy", "World", new Box(), p));
	}

	static public Node kennedyQuote() {
		// [Ask not what your country can do for you ,] → [ask what you can do for your country .]
		Box quote = new Box("Ask", "not", "what", "your", "country", "can", "do", "for", "you", ",");
		//{ sibling(-1) [ child(0) cursor(0) write(a) ] cursor(1) write() cursor(2) moveBy(4) cursor(-2) moveBy(-4) cursor(2) moveBy(5) cursor(-1) write(.) }

		Patch quotePatch = new Patch();
		quotePatch.add(new Command(Opcode.RIGHT, -1));
		quotePatch.add(new Box(
				new Keyed(Opcode.DOWN, 0),
				new Keyed(Opcode.DOWN, 0),
				new Keyed(Opcode.WRITE, "a")
				));
		quotePatch.add(new Box(
				new Command(Opcode.DOWN, 1),
				new Command(Opcode.WRITE)
				));
		quotePatch.add(new Box(
				new Command(Opcode.DOWN, 2),
				new Command(Opcode.MOVE_BY, 4)
				));
		quotePatch.add(new Box(
				new Command(Opcode.DOWN, -2),
				new Command(Opcode.MOVE_BY, -4)
				));
		quotePatch.add(new Box(
				new Command(Opcode.DOWN, 2),
				new Command(Opcode.MOVE_BY, 5)
				));
		quotePatch.add(new Box(
				new Command(Opcode.DOWN, -1),
				new Command(Opcode.WRITE, ".")
				));

		return new Box(quote, quotePatch);
	}

	static public Node loadLessThan() {
		Patch p = new Patch();
		p.add(new Keyed(">"));
		return p;
	}

	static public Node patchInPatch() {
		Node m = new Box(12, 9, new Box("hello", "world"), new Patch(
				new Command(Opcode.VALUE, -1),
				new Command(Opcode.UP),
				new Command(Opcode.RIGHT, new Patch(
						new Command(Opcode.UP),
						new Command(Opcode.RIGHT, -2),
						new Command(Opcode.DOWN, 1)
						))
				));
		return m;
	}

	static public Node sampleTree() {
		Node alice = new Keyed("person",
			new Box(
				new Keyed("name", "Alice"),
				new Keyed("birthday", new Box("Jan", 22, 1983)),
				//new Keyed("spouse", new Patch(new Command(Opcode.UP, 3), new Command(Opcode.RIGHT, 1)))
				new Keyed("spouse",
						new Keyed("person",
								new Box(
									new Keyed("name", "Bob"  ),
									new Keyed("birthday", new Box("Feb", 03, 1984)),
									new Keyed("spouse")
						)))
			));
		Node bob = new Keyed("person",
			new Box(
				new Keyed("name", "Bob"  ),
				new Keyed("birthday", new Box("Feb", 03, 1984)),
				//new Keyed("spouse", new Patch(new Command(Opcode.UP, 3), new Command(Opcode.RIGHT, -1)))
				new Keyed("spouse",
						new Keyed("person",
								new Box(
									new Keyed("name", "Alice"),
									new Keyed("birthday", new Box("Jan", 22, 1983)),
									new Keyed("spouse")
								))
						)
			));
		Box people = new Box(alice, bob);
		return people;
	}

	static public Node fishRedblue() {
		Box data =
			new Box(
				new UTF8("fish").with(Metadata.lang_ID, "b"),
				new Box(
					new Keyed(
						new Int32(1).with(Metadata.lang_ID, "k"),
						new UTF8("red").with(Metadata.lang_ID, "vs")
					),
					new Keyed(
						new Int32(2).with(Metadata.lang_ID, "k"),
						new Symbol.True().with(Metadata.lang_ID, "vb")
					)
				)
			);
		data.with(Metadata.lang_ID, "a");
		Box type =
			new Keyed("type",
				new Box(
					"",
					new Box(
						new Keyed(
							0,
							new Patch(
								new Command(Opcode.VALUE, new Box("", new Symbol.False())),
								new Command(Opcode.CHOICE, 1)
							)
						)
					)
				)
			);
		Box typeTypes = new Box("Text", new Box(new Keyed("Integer", new Box("Text", "|", "Boolean"))));

		return new Box(data, type, typeTypes);
	}

	static public Node sickAndTired() {
		return new Box("I'm", new Box("sick", "and", "tired"), new Box("of", "being"), new Patch(new Command(Opcode.LEFT, 2)));
	}

	static public Node pairsVsCommands() {
		Box pairs = new Box(
				new Keyed(0),                  //nothing
				new Keyed(1),                  //stop

				new Keyed(2, 1),                  //up
				new Keyed(3, new Box(0,2,1)),     //down
				new Keyed(4, 2),                  //right
				new Keyed(5, 2),                  //right
				new Keyed(6),                     //meta
				new Keyed(7),                     //root
				new Keyed(8),                     //home

				new Keyed(9, 9),                  //select
				new Keyed(10, "Hello"),            //write
				new Keyed(11, "World"),             //insert

				new Keyed(12,  0),                  //move to
				new Keyed(13,-2),                  //move by
				new Keyed(14, new Box("abc", 123)),//data
				new Keyed(15, "if")                //load
				);
		Patch p = new Patch(); for(Node n : pairs.children) p.add(n);
		return new Box(pairs, p);
	}

	static public Node demoLoad() {
		Patch lt = new Patch(
				new Keyed(">"),
				new Box(new Command(Opcode.DOWN, 0), new Command(Opcode.WRITE, 12)),
				new Box(new Command(Opcode.DOWN, 2), new Command(Opcode.WRITE, 25))
				);
		Patch pIf = new Patch(
				new Keyed("if"),
				new Box(new Command(Opcode.DOWN, 1), new Command(Opcode.WRITE, new Symbol.True())),
				new Box(new Command(Opcode.DOWN, 3), new Command(Opcode.WRITE, "yes")),
				new Box(new Command(Opcode.DOWN, 5), new Command(Opcode.WRITE, "no"))
				);
		return new Box(lt, pIf, new Symbol.True(), new Symbol.False(), new Symbol.Error());
	}

	static public Node fib() {
		Patch posA = new Patch(new Command(Opcode.UP, 5), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
		Patch posB = new Patch(new Command(Opcode.UP, 3), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
		Patch posC = new Patch(new Command(Opcode.UP, 9), new Command(Opcode.LEFT, 1), new Command(Opcode.DOWN, 1));
		Patch recurseMinus1 = new Patch(
				new Command(Opcode.UP, 6),
				new Command(Opcode.CLONE),
				new Command(Opcode.DOWN_NOEVAL, 0, 1),
				new Command(Opcode.WRITE, new Patch(new Command("-", posC, 1))),
				new Command(Opcode.UP, 2)
				);
		Patch recurseMinus2 = new Patch(
				new Command(Opcode.UP, 6),
				new Command(Opcode.CLONE),
				new Command(Opcode.DOWN_NOEVAL, 0, 1),
				new Command(Opcode.WRITE, new Patch(new Command("-", posC, 2))),
				new Command(Opcode.UP, 2)
				);

		int x = 7;
		Node tree = new Patch(
				new Command(Opcode.VALUE, x),
				new Command(Opcode.VALUE, new Patch(new Command("if", new Patch(new Command("<", posA, 2)), posB, new Patch(new Command("+", recurseMinus1, recurseMinus2))))));
		return new Box(tree);
	}

	static public Node figure1() {
		Node n = new Keyed("person", new Box(
				new Keyed("name", "Alice"),
				new Keyed("birthday", new Box("Jan", 22, 1983))
				));
		return new Box(n);
	}

	static public Node metadata() {
		//new Box(
		//		new Box("Marcus", "Aurelius").with(Metadata.lang_ID, "name"),
		//		Node.toNode("Chris").with(Metadata.lang_ID, "first"),
		//		Node.toNode("Hall").with(Metadata.lang_ID, "last")
		//		);

		Node fox = new UTF8("fox"), dog = new UTF8("dog");
		Box qbf = new Box("The", fox, "jumped", "over", "the", dog);
		fox.with(new UTF8("adj"), "quick", "brown");
		dog.with(new UTF8("adj"), "lazy");

		Box metadataExample = new Box("The", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog.");
		metadataExample.with(new UTF8("type"), new Box(""));
		metadataExample.with(new UTF8("CSS"), new Keyed("font style", "italic"), new Keyed("background color", "FF 92 12"), new Keyed("font weight", "bold"));
		//metadataExample.with(new UTF8("markup A"), "a1");
		//metadataExample.with(new UTF8("markup B"), "b1");
		//metadataExample.with(new UTF8("markup C"), "c1", "c2");

		UTF8 CSS = new UTF8("{ background-color:#ff9212; font-stylet:italic; }");
		Box infraCSS = new Box(new Keyed("background color", "FF 92 12"), new Keyed("font style", "italic"));

		return new Box(qbf, metadataExample, CSS, infraCSS);
	}

	static public Node metadataMetadata() {
		Node a = new UTF8("Chris").with(Metadata.lang_ID, "first");
		//a.getMetadata().with(Metadata.lang_version, 2);
		//a.getMetadataChannel(Metadata.lang_ID).with(Metadata.lang_format, "txt");
		a.getMetadataChannel(Metadata.lang_ID).get(1).with(Metadata.lang_version, 0.1);
		return new Box(
				new Box("Marcus", "Aurelius").with(Metadata.lang_ID, "name").with(Metadata.lang_version, 2),
				a,
				new UTF8("Hall").with(Metadata.lang_ID, "last")
				);
	}

	static public Node paragraph() {
		String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque diam nisi, interdum nec vehicula a, dictum ac odio. Nam tincidunt orci vel massa gravida rutrum. Etiam pretium arcu lacinia metus luctus gravida. Mauris sem magna, laoreet et volutpat eu, fermentum non ligula. Sed sit amet scelerisque metus. Vestibulum lobortis congue magna, ac euismod ligula eleifend a. Phasellus fermentum at massa sit amet tristique. Nullam lacinia, nulla at ultrices viverra, leo diam auctor metus, eget interdum tellus dui sed massa. Cras vel risus tortor.";
		String[] words = text.split(" ");
		Box b = new Box();
		for(String w : words) b.add(w);
		return b;
	}

	static public Node namedNodes() {
		Node a = new Box(
				new Int32(5).with(Metadata.lang_ID, "x"),
				new Patch(new Command(Opcode.ID, "x"))
				);
		Node b = new Box(
				new Int32(5).with(Metadata.lang_ID, "o"),
				new Patch(new Command(Opcode.ID, "x"))
				);
		return new Box(a, b);
	}

	static public Node simpleSave() {
		return new Box(
				"environment",
				new Box(
						"apple",
						new Patch(
								new Command(Opcode.LEFT, 1),
								new Command(Opcode.WRITE, "banana"),
								new Command(Opcode.SAVE)
								),
						0,
						new Patch(
								new Command(Opcode.LEFT, 1),
								new Command(Opcode.WRITE, new Patch(new Command("+", new Patch(new Command(Opcode.UP, 3), new Command(Opcode.LEFT, 1)), 1))),
								new Command(Opcode.SAVE)
								)
				)
			);
	}

	static public Node url() {
		return new Box(
				new UTF8("https"),//.with(null, ":"),
				new Box("www", "google", "com"),//.with(null, "//"),
				new UTF8("url"),//.with(null,  "/"),
				new Box(
					new Command("sa", "t"),
					new Command("rct","j"),
					new Command("q"),
					new Command("esrc","s"),
					new Command("source","web"),
					new Command("cd",6),
					new Command("cad","rja"),
					new Command("uact",8),
					new Command("ved","0CDkQFjAF"),
					//new Command("url","http%3A%2F%2Fen.wikipedia.org%2Fwiki%2FUniform_resource_locator"),
					//new Command("url","http://en.wikipedia.org/wiki/Uniform_resource_locator"),
					new Command("url", new Box("http", new Box("en", "wikipedia", "org"), new Box("wiki", "Uniform_resource_locator"))),
					new Command("ei","tE8sVe-iF4m8ggSZi4T4AQ"),
					new Command("usg","AFQjCNFVKoOa_HlcuDeUu8wYS_g70me4Kw"),
					new Command("bvm","bv.90491159,d.eXY")
				),//.with(null, "?")
				"https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=6&cad=rja&uact=8&ved=0CDkQFjAF&url=http%3A%2F%2Fen.wikipedia.org%2Fwiki%2FUniform_resource_locator&ei=tE8sVe-iF4m8ggSZi4T4AQ&usg=AFQjCNFVKoOa_HlcuDeUu8wYS_g70me4Kw&bvm=bv.90491159,d.eXY"
		);
	}

	static public Node nameAgeTable() {
		Box type = new Box(
				new Box(
						new UTF8().with(Metadata.lang_ID, "first"),
						new UTF8().with(Metadata.lang_ID, "last")
				).with(Metadata.lang_ID, "name"),
				new Int32().with(Metadata.lang_ID, "age"),
				new UTF8().with(Metadata.lang_ID, "gender"));
		Box john = new Box(
				new Box("John", "Smith"),//.with(Metadata.lang_ID, "name"),
				new Int32(45),//.with(Metadata.lang_ID, "age"),
				"male");
		Box mary = new Box(
				new Box("Mary", "van der Waal"),//.with(Metadata.lang_ID, "name"),
				new Int32(30)//.with(Metadata.lang_ID, "age"),
				);
		Box jen = new Box(
				new Box("Jennifer", "Spear"),//.with(Metadata.lang_ID, "name"),
				new Int32(10),//.with(Metadata.lang_ID, "age")
				"female");
		Box table = new Box(john, mary, jen);
		table.with(Metadata.lang_type, type);
		System.out.println(InfraEncoding.encoded(table).remaining());
		return table;
	}

	static public Node fileObject() {
		return new Box(
			new Patch(
				new Command("file"),
				new Command(Opcode.ID, "C:"),
				new Command(Opcode.ID, "Data"),
				new Command(Opcode.ID, "Projects")
			),
			new Patch(
				new Box(
					new Command("file"),
					"C:", "Data", "Projects", "Programming", "Infra"
				)
			),
			new jiyuiydi.infra.OS.FileSystems(new File("C:/Data/Projects/Programming/Infra/"))
		);
	}

}
