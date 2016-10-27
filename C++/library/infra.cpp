#include "infra.hpp"
#include <stdexcept>
#include <iostream> // clog

namespace Infra {

	const char* toString(iType t) {
		switch (t) {
		case FREE: return "free";
		case BYTES: return "bytes";
		case STRING: return "text";
		case INTEGER: return "int";
		case FLOATING: return "float";
		case LIST: return "list";
		case KEYED: return "keyed";
		case PATCH: return "patch";
		case CONTINUATION: return "continuation";
		case META: return "metadata";

		case BITS: return "bits";
		case NIBBLE: return "nibble";
		case SYMBOL: return "symbol";

		default: return "unallocated";
		}
	}

	// iNode /////////////////////////////////////////////////////////////

	iNode* iNode::construct(byte* data) {
		int type = data[0] >> 4;
		if (type == iType::INTEGER) return new iInt(data);
		if (type == iType::STRING) return new iString(data);
		if (type == iType::LIST) return new iList(data);
		if (type == iType::KEYED) return new iKeyed(data);
		throw std::runtime_error("unimplemented feature");
	}

	iNode* iNode::construct(istream& in) {
		byte head;
		in >> head;
		iType type = static_cast<iType>(head >> 4);
		int bodyLen;
		if (type < 10) {
			bodyLen = head & 0x0f;
			if (bodyLen == 0x0f)
				throw std::runtime_error("unimplemented feature");
		}
		else {
			throw std::runtime_error("unimplemented feature");
		}

		switch (type) {
		case STRING: return new iString(in, bodyLen); break;
		case INTEGER: return new iInt(in, bodyLen); break;
		case LIST: return new iList(in, bodyLen); break;
		case KEYED: return new iKeyed(in, bodyLen); break;
		default: throw std::runtime_error("unimplemented feature");
		}

	}

	iNode::iNode(size_t bodyLen) : bodyLen(bodyLen) {
		headerLen = 0;
		body = new byte[bodyLen];
	}

	iNode::iNode(byte* data) {
		if (data == nullptr) {
			body = nullptr;
			headerLen = 0;
			bodyLen = 0;
		}
		else {
			int immediate = data[0] & 0x0f;
			if (immediate == 0x0f) throw std::runtime_error("unimplemented feature"); //TODO
			headerLen = 1;
			bodyLen = immediate;
			body = data + headerLen;
		}
	}

	iNode::~iNode() { if (body && !hasHeader()) delete[] body; }

	iType iNode::type() {
		if (hasHeader()) return static_cast<iType>(header()[0] >> 4);
		throw std::runtime_error("iNode::type() function not implemented in subclass");
	}

	void iNode::write(ostream& out) {
		if (hasHeader())
			out.write((char*) header(), bodyLen);
		else {
			byte header = type() << 4;
			if (bodyLen < 0x0f) header += (byte)bodyLen;
			else
				throw std::runtime_error("unimplemented functionality");
			out.put(header);
			out.write((char*) body, bodyLen);
		}
	}

	string iNode::toString() {
		return string(Infra::toString(type()))
			+ std::to_string(bodyLen)
			+ "{"
			+ Utils::byte_2_str((char*)body, bodyLen)
			+ "}";
	}

	ostream& operator<< (ostream& out, iNode& n) {
		n.write(out);
		return out;
	}

	istream& operator>> (istream& in, iNode*& n) {
		n = iNode::construct(in);
		return in;
	}

	// Data //////////////////////////////////////////////////////////////

	Data::Data(const char* str) { ptr = new iString(str); }

	Data::Data(int v) { ptr = new iInt(v); }

	ostream& operator<< (ostream& out, Data& d) {
		return out << *d;
	}

	istream& operator >> (istream& in, Data& d) {
		d = iNode::construct(in);
		return in;
	}

	// iList /////////////////////////////////////////////////////////////

	iList::iList(byte* data) : iNode(data) {
		size_t mark = 0;
		while (mark < bodyLen) {
			iNode* n = construct(body + mark);
			mark += n->encodedSize();
			sub.push_back(n);
		}
		if (mark > bodyLen) throw std::runtime_error("parse error");
	}

	iList::iList(istream& in, size_t length) : iNode(length) {
		while (length > 0) {
			std::streampos start = in.tellg();
			sub.push_back(construct(in));
			length -= (size_t)(in.tellg() - start);
		}
		if (length < 0) throw std::runtime_error("parse error");
	}

}
