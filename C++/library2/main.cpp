#include <cassert>
#include <iostream>
#include <sstream>
#include <typeinfo>

#include "infra2.hpp"

using namespace Infra2;
using std::cout;
using std::endl;
using std::stringstream;

//                  keyd  int       text
byte example2[] = { 0x66, 0x31,  1, 0x23, 'r' , 'e' , 'd' };
//                  free  int       free  free  int
byte example3[] = { 0x00, 0x31, 42, 0x00, 0x00, 0x31, 0x02 };
//                  text            meta  keyd
byte example4[] = { 0x22, 'J', 'o', 0x91, 0x60 };

void testHeader_freeSegment() {
	Header h;
	h = Header::freeSegment(0);
	assert(h.segmentLen() == 0);

	h = Header::freeSegment(1);
	assert(h.segmentLen() == 1);

	h = Header::freeSegment(15);
	assert(h.segmentLen() == 15);

	h = Header::freeSegment(16); // a tricky one
	assert(h.segmentLen() == 16);
	std::stringstream ss;
	assert(h.write(ss) == 2);
	Header h16(ss);
	assert(h16.segmentLen() == 16);

	for (int i = 0; i < 256; ++i) {
		h = Header::freeSegment(i);
		assert(h.segmentLen() == i);
	}
}

void testHeader_construct() {
	{ // Construct from array
		Header h(example2);
		assert(h.type == SegmentType::KEYED);
		assert(h.headLen == 1);
		assert(h.contentLen == 6);
	}
	{ // Construct from stream
		stringstream ss;
		ss.write((const char*) example2, sizeof(example2));
		Header h(ss);
		assert(h.type == SegmentType::KEYED);
		assert(h.headLen == 1);
		assert(h.contentLen == 6);
	}

	// Multi-byte Headers

	byte hl2[] = { 0x0f, 0x7f };
	{ // Construct from array
		Header h(hl2);
		assert(h.type == SegmentType::FREE);
		assert(h.headLen == 2);
		assert(h.contentLen == 127);
	}
	{ // Construct from stream
		stringstream ss;
		ss.write((const char*) hl2, sizeof(hl2));
		Header h(ss);
		assert(h.type == SegmentType::FREE);
		assert(h.headLen == 2);
		assert(h.contentLen == 127);
	}
}

void testLeaf_readWriteStream() {
	#define check(_t, _l, _e, _s) {           \
	  stringstream ss;                        \
	  ss << _t;                               \
	  assert((size_t)ss.tellp() == _l);       \
	  unique_ptr<Segment> s;                  \
	  ss >> s;                                \
	  assert(s->type() == SegmentType::_e);   \
	  assert(s->toString() == _s);            \
	}                                         \

	check(String("hello"), 6, STRING, "hello");
	check(Integer((int8_t) 42), 1+1, INTEGER, "42");
	check(Integer((int16_t)42), 1+2, INTEGER, "42");
	check(Integer(         42), 1+4, INTEGER, "42");
	check(Integer((int64_t)42), 1+8, INTEGER, "42");
	check(Floating(42.42f     ), 1 + 4, FLOATING, "42.42");
	check(Floating(42.42424242), 1 + 8, FLOATING, "42.4242");
}

void testLeaf_readArray() {
	//                 meta  keyd  text       text       text
	byte example[] = { 0x95, 0x64, 0x21, 'a', 0x21, 'b', 0x21, 'c' };
	auto s = Segment::factory(example);
	assert(Inspector(*s).body() == example + 7); // 'c'
	assert(s->getMetadata(String("a"))->operator[](1) == String("b"));
	//delete s;
}

void testEncodingPlan() {
	{
		String s("hi");
		EncodingPlan ht(s);
		assert(ht.me.type == SegmentType::STRING);
		assert(ht.me.contentLen == 2);
		assert(ht.total() == 3);
	}
	{
		List l;
		l.add(new String("a"));
		l.add(new String("b"));
		EncodingPlan ht(l);
		assert(ht.total() == 5);
	}
	{ // with freeSpace
		List l;
		String* s;
		l.add(move(s = new String("a"))); s->setFreeSpace(4);
		l.add(move(s = new String("b"))); s->setFreeSpace(5);
		EncodingPlan ht(l, false);
		assert(ht.total() == 5 + 9);
		EncodingPlan ht2(l, true);
		assert(ht2.total() == 5);
	}
	{ // with Metadata
		List l;
		l << &((new String("a"))->with(String("b"), String("c")));
		EncodingPlan ep(l);
		assert(ep.metadata != nullptr);
		assert(ep.metadata->total() == 6);
		assert(ep.total() == 9);
	}
}

void testData_construct() {
	{ // Consruct from array
		Data d(example2, sizeof(example2));
		assert(d.count() == 1);
		assert(d[0].type() == SegmentType::KEYED);
		assert(d[0].count() == 2);
		Integer& i = (Integer&)d[0][0]; // get a Segment pointer copy
		assert(i.value<int>() == 1);
		assert((int)d[0][0] == 1); // implicit (int)
		example2[2] = 7;
		assert(i.value<int>() == 7);
		d.becomeMemoryOwner(); // cause memory allocation, and later delete[] when d goes out of scope
		example2[2] = 70;
		assert(i.value<int>() == 7);
	}

}

void testEqualityOperator () {
	{ // Test == operator
		assert(String("") == String(""));
		assert(String("") != String("a"));
		assert(Integer(0) == Integer(0));
		assert((Integer(5) != Integer(5)) == false);
		assert(Integer(0) != Integer(-1));

		assert(List() == List());
		assert(Patch() != List());
		assert(List() != Patch()); // tricky polymorphism case

		List l1, l2;
		l1.add("Hi");
		l2.add("Hi");
		assert(l1 == l2);
		l2.add("Bye");
		assert(l1 != l2);
	}
}

void testSegment_Clone() {
	{
		String s("test");
		String c = *s.clone();
		assert(&c != &s);
		assert(Inspector(c).body() != Inspector(s).body());
		assert(Inspector(c).memOwner() != Inspector(s).memOwner());
		assert(c.toString() == "test");
	}
	{ // With Metadata
		String s("test");
		s.with(String("id"), String("myMetadata"));
		String c = *s.clone();
		assert(c.getMetadata() != nullptr);
		assert(c.getMetadata(String("id"))->operator[](1) == String("myMetadata"));
	}
}

//void testMetadata() {
//	{
//	  Segment& s = String("myData").with(String("id"), String("myMetadata"));
//	  //cout << s;
//	}
//}

//std::string byte_2_str(char* bytes, size_t size) {
//	static const char hex[16] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
//
//	std::string str;
//	str.reserve(2 + size * 3);
//	for (size_t i = 0; i < size; ++i) {
//		if (i) str.append(" ", 1);
//		const char ch = bytes[i];
//		str.append(&hex[(ch & 0xF0) >> 4], 1);
//		str.append(&hex[ch & 0xF], 1);
//	}
//	return str;
//}

int main() {
	testHeader_freeSegment();
	testHeader_construct();
	testLeaf_readWriteStream();
	testLeaf_readArray();
	testEncodingPlan();
	testData_construct();
	testSegment_Clone();
	//testMetadata();

	{
		Data d(example3, sizeof(example3));
		assert(d.count() == 2);
		assert(d[0].getFreeSpace() == 2);
		//Integer& i = (Integer&) d.get(0); //d.getAs<Integer>(0);
		//int v = i.value<int>();
		//assert(v == 42);
		assert((int) d[0] == 42); // implicit (int)
	}
	{
		Data d(12);
		d.add("hello");

		size_t len = d.getEncodingPlan().total(); // d.getEncodingLength();
		assert(len == 1 + 5);
		char* buf = new char[len];
		d.write((byte*) buf, d.getEncodingPlan());
		//cout << byte_2_str(buf, len).c_str() << endl;
		assert(buf[0] == 0x25);
		assert(buf[1] == 'h'); assert(buf[5] == 'o');
	}
	{
		Data d(12);
		d.add(new List());
		((List&)d[0]).add("hi");

		size_t len = d.getEncodingPlan().total(); // d.getEncodingLength();
		assert(len == 1 + 1+2);
		char* buf = new char[len];
		d.write((byte*)buf, d.getEncodingPlan());
		//cout << byte_2_str(buf, len).c_str() << endl;
		assert(buf[0] == 0x53); // list3
		assert(buf[1] == 0x22); // text2
		assert(buf[2] == 'h');
		assert(buf[3] == 'i');
	}
	{
		Data d(example4, sizeof(example4));
		assert(d.count() == 1);
		assert(d[0].getMetadata()->count() == 1);
	}
	{
		Data l(10);
		char* sa[3] = {"a", "b", "c"};
		l.add(sa[0]); l.add(sa[1]); l.add(sa[2]);
		int i = 0;
		for (const Segment* s : l)
			//cout << ((String*)s)->value() << endl;
			assert(((String*)s)->value() == sa[i++]);
	}
	{ // Test list.remove
		Data d(example2, sizeof(example2)); // keyed[1 red]
		Segment& s = d[0][1];
		assert((std::string) s == "red");
		Inspector ins(s);
		assert(ins.body() == example2 + 4);
		assert(ins.memOwner() == nullptr);
		Segment* red = ((List&)d[0]).remove(1); // remove 'red'
		assert(red == &s);
		assert((std::string) *red == "red");
		assert(ins.body() != example2 + 4);
		assert(ins.memOwner() == red);
		delete red;
	}
	{ // Test Symbol
		byte encoding[] = {0xf1}; // symbol:True
		Data d(encoding, sizeof(encoding));
		assert(d[0].type() == SegmentType::SYMBOL);
		std::stringstream ss;
		ss << d;
	}

	testEqualityOperator();

	//{
	//	std::stringstream ss;
	//	cout << "tellg: " << ss.tellg() << ", tellp: " << ss.tellp() << endl;
	//	cout << "tellg: " << ss.tellg() << ", tellp: " << ss.tellp() << endl;
	//	ss << "Skull Island";
	//	cout << "tellg: " << ss.tellg() << ", tellp: " << ss.tellp() << endl;
	//	ss << "Galapagos";
	//	cout << "tellg: " << ss.tellg() << ", tellp: " << ss.tellp() << endl;
	//	std::string s1, s2;
	//	ss >> s1 >> s2;
	//	ss.tellg();
	//	cout << "tellg: " << ss.tellg() << ", tellp: " << ss.tellp() << endl;
	//	cout << "tellg: " << ss.tellg() << ", tellp: " << ss.tellp() << endl;
	//	cout << s1 << endl << s2 << endl;
	//	ss.clear();
	//	ss << "Third";
	//	cout << "tellg: " << ss.tellg() << ", tellp: " << ss.tellp() << endl;
	//	std::string s3;
	//	ss >> s3;
	//	cout << "tellg: " << ss.tellg() << ", tellp: " << ss.tellp() << endl;
	//	cout << s3 << endl;
	//}

	{ // Test multi-byte headers
		Data d(21); // (Free19)
		//assert(d.getEncodingLength(false) == 0);
		assert(d.getEncodingPlan().total() == 0);
	}
	{ // Test stream write and stream read (simple)
		std::stringstream ss;
		ss << String("Skull Island") << String("Galapagos");
		Data d;
		ss >> d >> d;
		assert(d.toString() == "[`Skull Island' Galapagos]");
	}

	return 0;
}
