package jiyuiydi.infra.converter;

import jiyuiydi.infra.Node;

public interface Converter {

	static class Response {
		public boolean suggested;
		public byte confidence; // -128 - 127
	}

	Response identify(Node n);
	Node convert(Node n);

}
