#pragma once

#include <string>
#include <vector>
#include "utils.hpp"

namespace Infra {

	using std::ostream;
	using std::istream;
	using std::string;

	typedef uint8_t byte;

	enum iType {
		FREE, BYTES, STRING, INTEGER, FLOATING,      // Leaves      [ 0 -  4]
		LIST, KEYED, PATCH, CONTINUATION, META,      // Containers  [ 5 -  9]
		UNALLOCATED_A, UNALLOCATED_B, UNALLOCATED_C, // Unallocated [10 - 12]
		BITS, NIBBLE, SYMBOL,                        // Special     [13 - 15]
	};

	const char* toString(iType);

	class VarUint {
	public:
		static int getEncodingLengthForQuantity(size_t v) {
			if (v <=                127 ) return 1;
			if (v <=              16383 ) return 2;
			if (v <=            2097151 ) return 3;
			if (v <=          134217727 ) return 4;
			if (v <=        34359738367L) return 5;
			if (v <=      1099511627775L) return 6;
			if (v <= 576460752303423487L) return 8;
			return 9;
		}
	};

	class iNode {

	protected:
		//iNode* metadata;
		byte headerLen; // header byte count
		byte* body; // to first byte of body (after the header)
		size_t bodyLen;

		byte* header() { return body - headerLen; }

	public:
		static iNode* construct(byte* data);
		static iNode* construct(istream&);

	protected:
		iNode(byte* data);

	public:
		iNode(size_t bodyLen);
		~iNode();

		bool hasHeader() { return headerLen > 0; }
		int encodedHeaderSize() {
			if (hasHeader()) return headerLen;
			return bodyLen < 0x0f ? 1 : VarUint::getEncodingLengthForQuantity(bodyLen);
		}
		size_t encodedBodySize() { return bodyLen; }
		size_t encodedSize() { return encodedHeaderSize() + bodyLen; }

		virtual iType type();

		virtual size_t count() { return 0; }
		virtual iNode* get(size_t index) { return nullptr; }

		void write(ostream&);

		virtual string toString();

	};

	ostream& operator<< (ostream&, iNode&);
	istream& operator>> (istream&, iNode*&);

	class Data {
		iNode* ptr;
	public:
		Data() : ptr(nullptr) {}
		Data(const char* str);
		Data(int v);
		virtual ~Data() { set(nullptr);  }
		void set(iNode* n) {
			if (ptr) delete ptr;
			ptr = n;
		}
		iNode& operator* () { return *ptr; }
		void operator= (iNode* n) { set(n); }
	};

	ostream& operator<< (ostream&, Data&);
	istream& operator >> (istream&, Data&);

	class iInt : public iNode {

		friend iNode;

	protected:
		iInt(byte* data) : iNode(data) {}

	public:
		iInt(int v) : iNode(4) { memcpy(body, &v, 4); }

		iInt(istream& in, size_t length) : iNode(4) {
			in.read((char*)body, length);
			for (++length; length <= 4; ++length)
				body[length-1] = in.get();
		}

		int getInt() {
			if (bodyLen == 4) return *reinterpret_cast<int*>(body);
			if (bodyLen == 1) return *reinterpret_cast<char*>(body);
			if (bodyLen == 2) return *reinterpret_cast<short*>(body);
			throw std::runtime_error("unimplemented functionality"); //TODO
		}

		string toString() override {
			return std::to_string(getInt());
		}

		iType type() override { return iType::INTEGER; }

	};

	class iString : public iNode {

		friend iNode;

	protected:
		iString(byte* data) : iNode(data) {}

	public:
		iString(const char* str) : iNode(strnlen(str, 1024)) {
			memcpy(body, str, bodyLen);
		}

		iString(istream& in, size_t length) : iNode(length) {
			in.read((char*) body, length);
		}

		iType type() override { return iType::STRING; }

		string toString() override {
			return string((char*) body, bodyLen);
		}

	};

	class iList : public iNode {

		friend iNode;

	protected:
		std::vector<iNode*> sub;
		iList(byte* data);

	public:
		iList(istream& in, size_t length);

		iList() : iNode(nullptr) {}
		iList(iNode* a) : iNode(nullptr) { add(a); }
		iList(iNode* a, iNode* b) : iNode(nullptr) { add(a); add(b); }
		virtual ~iList() { for (iNode* n : sub) delete n; }

		void add(iNode* n) { sub.push_back(n); }
		void add(size_t index, iNode* n) { sub.insert(sub.begin() + index, n); }

		// iNode /////////////////////////////////////////////////////////

		size_t count() override { return sub.size(); }
		iNode* get(size_t index) override { return sub[index]; }

		iType type() override { return iType::LIST; }

		string toString() override {
			string s("[");
			for (unsigned int i = 0; i < sub.size(); ++i) {
				if (i) s += ' ';
				s += sub[i]->toString();
			}

			s += "]";
			return s;
		}
	};

	class iKeyed : public iList {

		friend iNode;

	protected:
		iKeyed(byte* data) : iList(data) {}

	public:
		iKeyed(istream& in, size_t length) : iList(in, length) {}

		iType type() override { return iType::KEYED; }

		string toString() override {
			string s("<");
			if(sub.size() > 0) s += sub[0]->toString();
			s += ">[";
			for (unsigned int i = 1; i < sub.size(); ++i) {
				if (i > 1) s += ' ';
				s += sub[i]->toString();
			}
			s += "]";
			return s;
		}
	};

}
