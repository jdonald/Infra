#pragma once

#include <streambuf>
#include <istream>
#include <cstdint>
#include <vector>
#include <string>
#include <stdexcept>
#include <memory>
#include <utility> // std::move

/*
A Segment's Layout in Memory:

	Metadata      Free Segment                        Free
	[##____________][#__][###__________________________][#__________]
	^metadata->body          ^body*
	                         |---------bodyLen---------||-freespace-|
*/

namespace Infra2 {

	using std::vector;
	using std::ostream;
	using std::istream;
	using std::unique_ptr;
	using std::move;

	typedef uint8_t byte;

	//struct Membuf : std::streambuf {
	//	Membuf(char* begin, char* end) { setg(begin, begin, end); }
	//	int_type underflow() override { return  gptr() == egptr() ? traits_type::eof() : traits_type::to_int_type(*gptr()); }
	//};

	//struct Bytestream : public std::istream {
	//	Bytestream(char* source, size_t size)
	//		: std::istream(new Membuf(source, source + size)) {
	//	}
	//};

	class VarUint {
		//  prefix-bits bytes data-bits           max-value
		//("0__ __ ___"),  1,   7,                     127L), // 0
		//("10_ __ ___"),  2,  14,                  16_383L), // 1
		//("110 __ ___"),  3,  21,               2_097_151L), // 2
		//("111 00 ___"),  4,  27,             134_217_727L), // 3
		//("111 01 ___"),  5,  35,          34_359_738_367L), // 4
		//("111 10 ___"),  8,  59, 576_460_752_303_423_487L), // 5
		//("111 11 000"),  6,  40,       1_099_511_627_775L), // 6
		//("111 11 001"),  9,  64,                         ), // 7
		//("111 11 010"), 17, 128,                         ), // 8
		//("111 11 111"),  ?,   ?,                         ), // 9
	public:
		static int getEncodingLengthForQuantity(size_t v) {
			if (v <= 127) return 1;
			if (v <= 16383) return 2;
			if (v <= 2097151) return 3;
			if (v <= 134217727) return 4;
			if (v <= 34359738367L) return 5;
			if (v <= 1099511627775L) return 6;
			if (v <= 576460752303423487L) return 8;
			return 9;
		}
		static size_t write(byte* dst, size_t value, size_t expectedEncodingLength) {
			if (value <= 127 && expectedEncodingLength == 1) {
				dst[0] = (byte)value; return 1;
			}
			throw std::runtime_error("VarUint not implemented");
		}
		static size_t read(byte* src, uint64_t& quantity) {
			quantity = src[0];
			if (src[0] < 128) return 1;
			throw std::runtime_error("VarUint not implemented");
		}
		static size_t read(istream& in, uint64_t& quantity) {
			int h = in.get();
			if (h < 0) { quantity = 0; return 0; }
			quantity = (uint64_t) h;
			if (h < 128) return 1;
			#define more(n) for(int i = 0; i < n; ++i) { int b = in.get(); quantity <<= 8; quantity += b; }
			if (h < 192 /*110 00 000*/) { /*2-byte*/ more(1); quantity &=             0x3fff; return 2; } // 00...
			if (h < 224 /*111 00 000*/) { /*3-byte*/ more(2); quantity &=           0x1fffff; return 3; } // 000...
			if (h < 232 /*111 01 000*/) { /*4-byte*/ more(3); quantity &=         0x07ffffff; return 4; } // 000 00...
			if (h < 240 /*111 10 000*/) { /*5-byte*/ more(4); quantity &=       0x07ffffffff; return 5; } // 000 00...
			if (h < 248 /*111 11 000*/) { /*8-byte*/ more(7); quantity &= 0x07ffffffffffffff; return 8; } // 000 00...
			if (h < 249 /*111 11 001*/) { /*6-byte*/ more(5); quantity &=     0xffffffffffff; return 6; } // 000 00...
			if (h < 250 /*111 11 010*/) { /*9-byte*/ more(8);                                 return 9; }
			throw std::runtime_error("VarUint not implemented");
		}
	};

	enum class SegmentType {
		FREE, BYTES, STRING, INTEGER, FLOATING,      // Leaves      [ 0 -  4]
		LIST, KEYED, PATCH, METADATA, CONTINUATION,  // Containers  [ 5 -  9]
		UNALLOCATED_A, UNALLOCATED_B, UNALLOCATED_C, // Unallocated [10 - 12]
		BITS, NIBBLE, SYMBOL,                        // Special     [13 - 15]
	};

	bool isContainerType(SegmentType);
	const char* nameOf(SegmentType);

	struct Header {
		static size_t headLenFor(size_t bodyLen);
		static Header freeSegment(size_t segmentLen);

		SegmentType type;
		size_t headLen;
		size_t contentLen;
		byte immediate;

		Header() : type(SegmentType::FREE), headLen(1), contentLen(0) {}
		Header(byte* src);
		Header(istream&);
		Header(SegmentType t, size_t contentLen);
		void setContentLen(size_t);
		size_t segmentLen() { return headLen + contentLen; }
		size_t write(byte* dst) const; // returns number of bytes written
		size_t write(ostream&) const;
	};

	class Segment;

	struct EncodingPlan {
		EncodingPlan* metadata;
		Header me;
		bool encodeBodyAsIs;
		vector<EncodingPlan*> sub;
		size_t freeSpace;

		EncodingPlan(const Segment&, bool excludeFreeSpace = false);
		~EncodingPlan();
		size_t total();
	};

	class Inspector;
	class String;
	class List; // metadata
	class Keyed; // metadata entry
	class Metadata;
	class Data;

	class Segment { // base class
		friend EncodingPlan;
		friend Inspector;
		friend String;
		friend List;
		friend Data;

		// Statics
	public:
		static unique_ptr<Segment> factory(byte* src, Segment* memOwner = nullptr);
		static unique_ptr<Segment> factory(istream&);

	protected:
		Metadata* metadata = nullptr;
		byte* body;
		size_t bodyLen;
		size_t freeSpace = 0; // num free bytes available after body
		Segment* memOwner;

	public:
		virtual ~Segment();

		unique_ptr<Segment> clone();
		virtual void becomeMemoryOwner(); // become stand-alone. stop sharing or copy body buffer.
		size_t getFreeSpace() { return freeSpace; }
		void setFreeSpace(size_t);

		void setMetadata(Metadata*);
		Metadata* getMetadata() { return metadata; }
		Keyed* getMetadata(Segment& lang, bool createIfMissing = false);
		Segment& with(Segment& lang, Segment& entry);

		EncodingPlan getEncodingPlan(bool eliminatingInternalFreeSpace = false) const { return EncodingPlan(*this, eliminatingInternalFreeSpace); }
		size_t write(byte* dst, const EncodingPlan&);
		size_t write(ostream&, const EncodingPlan&);

		bool operator!= (Segment& s) { return !operator==(s); }
		virtual SegmentType type() const = 0;
		virtual bool operator== (Segment&) = 0;
		virtual size_t count() const { return 0; }
		virtual Segment& operator[] (size_t index) { throw std::runtime_error("Not a container type"); }
		virtual std::string toString();

		explicit operator int() const;
		explicit operator std::string() const;

	protected:
		Segment(byte* body, size_t bodyLen, Segment* memOwner);
		Segment(istream&, size_t bodyLen);

		bool resize(size_t requestedBodyLen);
		virtual bool resizeSubordinate(Segment& target, size_t requestedBodyLen);
		byte* after();
		virtual void relocate(byte* newMemory, Segment* newMemOwner);
	};

	class Inspector {
		Segment& seg;
	public:
		Inspector(Segment& s) : seg(s) {}
		byte* body() { return seg.body; }
		size_t bodyLen() { return seg.bodyLen; }
		Segment* memOwner() { return seg.memOwner; }
	};

	class Symbol : public Segment {
	public:
		enum Symbols { False, True, Void, Null, Any, Parameter, Error, MathConstant, };
	protected:
		Symbols sym;
	public:
		Symbol(Symbols);
		SegmentType type() const override { return SegmentType::SYMBOL; }
		bool operator== (Segment&) override;
		std::string toString() override;
		//size_t write(ostream&, bool eliminatingInternalFreeSpace = false) override;

		Symbols value() const { return sym; }
	};

	class String : public Segment {
		friend Segment;
	protected:
		String(byte* body, size_t bodyLen, Segment* memOwner) : Segment(body, bodyLen, memOwner) {}
		String(istream& in, size_t bodyLen) : Segment(in, bodyLen) {}
	public:
		String(const char* str);

		unique_ptr<String> clone() { return unique_ptr<String>(static_cast<String*>(Segment::clone().release())); }
		SegmentType type() const override { return SegmentType::STRING; }
		bool operator== (Segment&) override;
		std::string toString() override;

		std::string value() const { return std::string((const char*)body, bodyLen); }
	};

	class Integer : public Segment {
		friend Segment;
	protected:
		Integer(byte* body, size_t bodyLen, Segment* memOwner) : Segment(body, bodyLen, memOwner) {}
		Integer(istream& in, size_t bodyLen) : Segment(in, bodyLen) {}
	public:
		Integer(int8_t);
		Integer(int16_t);
		Integer(int32_t);
		Integer(int64_t);
		SegmentType type() const override { return SegmentType::INTEGER; }
		bool operator== (Segment&) override;
		std::string toString() override;

		template<typename T> T value() const;
		//bool write(int32_t);
		//bool write(int64_t);

		void operator= (int32_t);
	};

	class Floating : public Segment {
		friend Segment;
	protected:
		Floating(byte* body, size_t bodyLen, Segment* memOwner) : Segment(body, bodyLen, memOwner) {}
		Floating(istream& in, size_t bodyLen) : Segment(in, bodyLen) {}
	public:
		Floating(float);
		Floating(double);
		SegmentType type() const override { return SegmentType::FLOATING; }
		bool operator== (Segment&) override;
		std::string toString() override;

		template<typename T> T value() const;
	};

	class Iter {
		const List* home;
		int index;
	public:
		bool operator!= (const Iter& rhs) const;
		void operator++ ();
		Segment* operator* () const;
	protected:
		friend List;
		Iter(const List* l, int i) : home(l), index(i) {}
	};

	class List : public Segment {
		friend Segment;
		friend Iter;
		friend Data;
	
	protected:
		vector<Segment*> sub;
		byte* continueLoadingFrom; // for delayed loading

	public:
		List();
		virtual ~List();

		Iter begin() const { return Iter(this, 0); }
		Iter end() const { return Iter(this, count()); }

		unique_ptr<List> clone() { return unique_ptr<List>(static_cast<List*>(Segment::clone().release())); }
		void insert(Segment*const&&, int index = -1);
		Segment* remove(int index);
		void erase(int index) { delete remove(index); }
		void add(Segment*const&& s) { insert(move(s), -1); }
		void add(const char* str); // convenience method
		List& operator<< (Segment*const&& s) { add(move(s)); return *this; }

		// Segment
		SegmentType type() const override { return SegmentType::LIST; }
		void becomeMemoryOwner() override;
		bool operator== (Segment&) override;
		size_t count() const override;
		Segment& operator[] (size_t index) override { return *sub[index]; }
		std::string toString() override;

	protected:
		List(byte* body, size_t bodyLen, Segment* memOwner);
		List(byte* body, size_t maxLength); // static buffer
		List(istream& in, size_t bodyLen) : Segment(in, bodyLen) {}
		void initChildren();
		void relocate(byte* newBody, Segment* newMemOwner) override;
	};

	class Keyed : public List {
		friend Segment;

	public:
		Keyed() : List() {}
		Keyed& operator<< (Segment*const&& s) { add(move(s)); return *this; }

		// Segment
		SegmentType type() const override { return SegmentType::KEYED; }

	protected:
		Keyed(byte* body, size_t bodyLen, Segment* memOwner) : List(body, bodyLen, memOwner) {}
		Keyed(istream& in, size_t bodyLen) : List(in, bodyLen) {}
	};

	class Metadata : public List {
		friend Segment;
		friend List;

	public:
		Metadata() : List() {}

		Metadata& operator<< (Segment*const&& s) { add(move(s)); return *this; }
		SegmentType type() const override { return SegmentType::METADATA; }

	protected:
		Metadata(byte* body, size_t bodyLen, Segment* memOwner) : List(body, bodyLen, memOwner) {}
		Metadata(istream& in, size_t bodyLen) : List(in, bodyLen) {}
	};

	class Patch : public List {
		friend Segment;
	public:
		Patch() : List() {}
		SegmentType type() const override { return SegmentType::PATCH; }
	protected:
		Patch(byte* body, size_t bodyLen, Segment* memOwner) : List(body, bodyLen, memOwner) {}
		Patch(istream& in, size_t bodyLen) : List(in, bodyLen) {}
	};

	class Data : public List {
		friend Segment;
		friend List;

	protected:
		size_t arraySize;

	public:
		Data();
		Data(size_t initialAllocation);
		Data(byte* src, size_t maxLength);

		//size_t getEncodingLength(bool eliminatingInternalFreeSpace = false) override;
		//size_t write(byte* dst, bool eliminatingInternalFreeSpace = false) override;

	protected:
		bool resizeSubordinate(Segment&, size_t requestedBodyLen) override;
		size_t transferFreeSpaceToChild(List* from, byte* fromHead, size_t toIndex, size_t amount);
	};

	ostream& operator<< (ostream&, Segment&);
	
	istream& operator>> (istream&, unique_ptr<Segment>&);
	istream& operator>> (istream&, List&);

	template<typename T> T Integer::value() const {
		// assuming little-endian
		if (bodyLen == 4) return (T) *reinterpret_cast<int32_t*>(body);
		if (bodyLen == 1) return (T) *reinterpret_cast<int8_t*>(body);
		if (bodyLen == 8) return (T) *reinterpret_cast<int64_t*>(body);
		if (bodyLen == 2) return (T) *reinterpret_cast<int16_t*>(body);
		if (bodyLen == 0) return 0;
		if (bodyLen == 3) /*2+1*/   return (T)(*reinterpret_cast<int16_t*>(body) + (static_cast<int32_t>(*reinterpret_cast<int8_t*>(body + 2)) << 16));
		if (bodyLen == 5) /*4+1*/   return (T)(*reinterpret_cast<int32_t*>(body) + (static_cast<int64_t>(*reinterpret_cast<int8_t*>(body + 4)) << 32));
		if (bodyLen == 6) /*4+2*/   return (T)(*reinterpret_cast<int32_t*>(body) + (static_cast<int64_t>(*reinterpret_cast<int16_t*>(body + 4)) << 32));
		if (bodyLen == 7) /*4+2+1*/ return (T)(*reinterpret_cast<int32_t*>(body) + (static_cast<int64_t>(*reinterpret_cast<int16_t*>(body + 4)) << 32) + (static_cast<int64_t>(*reinterpret_cast<int8_t*>(body + 6)) << 48));
		throw std::runtime_error("unimplemented byte width");
	}

	template<typename T> T Floating::value() const {
		// assuming little-endian
		if (bodyLen == 4) return (T) *reinterpret_cast<float*>(body);
		if (bodyLen == 8) return (T) *reinterpret_cast<double*>(body);
		if (bodyLen == 0) return 0;
		throw std::runtime_error("unimplemented byte width");
	}

}
