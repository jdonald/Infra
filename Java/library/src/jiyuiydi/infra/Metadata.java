package jiyuiydi.infra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class Metadata extends Box {

	static public final Node
		lang_format          = new UTF8("format"),
		lang_version         = new UTF8("version"),
		lang_ID              = new UTF8("ID"),
		lang_UID             = new UTF8("UID"),
		lang_encodingHints   = new UTF8("encoding"),
		lang_style           = new UTF8("style"),
		lang_type            = new UTF8("type"),
		lang_patchOutput     = new UTF8("Patch"),
		lang_metaMarkup      = new UTF8("meta"),
		lang_mathConstant    = new UTF8("math constant"),
		lang_comment         = new UTF8("comment"),
		lang_closet          = new UTF8("closet"), // isolated storage for reference data such as String-pools
		lang_messageHandlers = new UTF8("message handlers") // for turning Nodes into 'Objects' with named behaviors (can even override Patch opcodes)
	;

	// instance //////////////////////////////////////////////////////////

	HashMap<Node, Keyed> map = new HashMap<>();

	public Keyed getOrNull(Node lang) { return map.get(lang); }

	public Keyed getOrCreate(Node lang) {
		Keyed md = map.get(lang);
		if(md == null) {
			md = new Keyed(lang);
			children.add(md);
			map.put(lang, md);
		}
		return md;
	}

	public Collection<Keyed> getAll() { return map.values(); }

	public void add(Node lang, Object statement) {
		Keyed md = getOrCreate(lang);
		md.add(statement);
	}

	// Box ///////////////////////////////////////////////////////////////

	@Override public void add(Object... os) {
		for(Object o : os) {
			Node n = toNode(o);
			if(n instanceof Keyed && n.count() > 0) {
				Keyed lang = getOrCreate(n.get(0)); // will add to children when necessary
				for(int i = 1; i < n.count(); ++i) // may add no statements
					lang.add(n.get(i));
			} else {
				children.add(n); // loose items
			}
		}
	}

	@Override public void insert(int index, Node n) { add(n); } // ignore insertion index
	@Override public void replace(int index, Object o) { remove(index); add(o); }

	@Override public Node remove(int index) {
		if(children.get(index) instanceof Keyed && children.get(index).count() > 0) {
			Node key = children.get(index).get(0);
			map.remove(key);
		}
		return super.remove(index);
	}

	// Node //////////////////////////////////////////////////////////////

	@Override
	public boolean blindEquals(Object o) {
		if(!(o instanceof Metadata)) return false;
		return map.equals(((Metadata) o).map);
	}

	// Object ////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" w/");
		sb.append('(');
		for(Keyed i : map.values())
			sb.append(i.toString());
		sb.append(')');
		if(metadata != null) sb.append(metadata.toString());
		return sb.toString();
	}

}
