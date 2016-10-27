package jiyuiydi.infra.markupModels;

import jiyuiydi.infra.*;

public abstract class AbstractMarkupModel {

	Node p2;
	Metadata p1;
	Keyed statements;

	public AbstractMarkupModel(Node n) {
		p2 = n;
		p1 = p2.getMetadata();
		if(p1 != null)
			statements = p1.getMetadataChannel(getMyLangID());
	}

	abstract Node getMyLangID();

}
