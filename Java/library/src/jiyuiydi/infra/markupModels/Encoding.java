package jiyuiydi.infra.markupModels;

import jiyuiydi.infra.*;

public class Encoding extends AbstractMarkupModel {

	static final String propID_pad = "pad to";

	static public Node padTo(Node n, int net) {
		Keyed ch = n.getMetadataChannel(Metadata.lang_encodingHints);
		Keyed prop = ch == null ? null : ch.findKeyed(propID_pad);
		if(prop != null) { prop.replace(1, net); return n; }
		return n.with(Metadata.lang_encodingHints, new Keyed(propID_pad, net));
	}

	// instance //////////////////////////////////////////////////////////

	Keyed padTo;

	public Encoding(Node n) {
		super(n);
		if(statements != null) padTo = statements.findKeyed(propID_pad);
	}

	@Override Node getMyLangID() { return Metadata.lang_encodingHints; }

}
