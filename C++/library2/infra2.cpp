#include "infra2.hpp"

#include <stack>
#include <algorithm> // max
#include <cstring>   // memcpy
#include <sstream>
#include <typeinfo>  // typeid

namespace Infra2 {

	using std::stack;
	using std::runtime_error;
	using std::stringstream;

	bool isContainerType(SegmentType st) { return (int)st >= 5 && (int)st <= 9; }

	const char* nameOf(SegmentType t) {
		switch (t) {
		case SegmentType::FREE: return "free";
		case SegmentType::BYTES: return "bytes";
		case SegmentType::STRING: return "text";
		case SegmentType::INTEGER: return "int";
		case SegmentType::FLOATING: return "float";
		case SegmentType::LIST: return "list";
		case SegmentType::KEYED: return "keyed";
		case SegmentType::PATCH: return "patch";
		case SegmentType::CONTINUATION: return "continuation";
		case SegmentType::METADATA: return "metadata";

		case SegmentType::BITS: return "bits";
		case SegmentType::NIBBLE: return "nibble";
		case SegmentType::SYMBOL: return "symbol";

		default: return "unallocated";
		}
	}

	// Header ////////////////////////////////////////////////////////////

	size_t Header::headLenFor(size_t bodyLen) {
		if (bodyLen < 0x0f) return 1;
		return 1 + VarUint::getEncodingLengthForQuantity(bodyLen);
	}

	Header Header::freeSegment(size_t segmentLen) {
		if (segmentLen == 0) {
			Header h(SegmentType::FREE, 0);
			h.headLen = 0; // a non-segment
			return h;
		}
		Header h(SegmentType::FREE, segmentLen - headLenFor(segmentLen));
		size_t shortBy = segmentLen - h.segmentLen();
		h.headLen += shortBy;
		return h;
	}

	Header::Header(byte* head) {
		type = static_cast<SegmentType>(*head >> 4);
		immediate = *head & 0x0f;
		if (type == SegmentType::SYMBOL) setContentLen(0);
		else
		if (immediate == 0x0f) {
			uint64_t q;
			size_t read = VarUint::read(head + 1, q);
			setContentLen((size_t) q);
			headLen = 1 + read;
		}
		else setContentLen(immediate);
	}

	Header::Header(istream& in) {
		int b = in.get();
		if (b < 0) { type = SegmentType::FREE; setContentLen(0); return; }
		type = static_cast<SegmentType>(b >> 4);
		immediate = b & 0x0f;
		if (immediate == 0x0f) {
			uint64_t q;
			size_t read = VarUint::read(in, q);
			setContentLen((size_t) q);
			headLen = 1 + read;
		}
		else setContentLen(immediate);
	}

	Header::Header(SegmentType t, size_t contentLen)
		: type(t) {
		setContentLen(contentLen);
	}

	void Header::setContentLen(size_t len) {
		contentLen = len;
		if (contentLen < 0x0f) headLen = 1;
		else headLen = 1 + VarUint::getEncodingLengthForQuantity(len);
	}

	size_t Header::write(byte* dst) const {
		if (headLen == 0) return 0;
		if (headLen == 1) {
			dst[0] = ((byte) type << 4) + (byte) contentLen;
			return 1;
		}
		dst[0] = ((byte)type << 4) + 0x0f;
		return 1 + VarUint::write(dst + 1, contentLen, headLen - 1);
	}

	size_t Header::write(ostream& out) const {
		byte buff[1+9]; // largest header length
		size_t count = write(buff);
		out.write((char*)buff, count);
		return count;
	}

	// EncodingPlan //////////////////////////////////////////////////////

	EncodingPlan::EncodingPlan(const Segment& s, bool excludeFreeSpace)
		: me(s.type(), 0) {
		if (s.metadata) metadata = new EncodingPlan(*s.metadata, excludeFreeSpace);
		else metadata = nullptr;

		if (s.type() == SegmentType::SYMBOL)
			me.immediate = dynamic_cast<const Symbol&>(s).value();

		freeSpace = (excludeFreeSpace ? 0 :s.freeSpace);

		if (isContainerType(s.type())) {
			encodeBodyAsIs = s.body && !excludeFreeSpace;
			if (!encodeBodyAsIs) {
				const List& l = dynamic_cast<const List&>(s);
				for (Segment* s : l) {
					EncodingPlan* sht = new EncodingPlan(*s, excludeFreeSpace);
					sub.push_back(sht);
					me.setContentLen(me.contentLen + sht->total() + sht->freeSpace);
				}
			} else
				me.setContentLen(s.bodyLen);
		} else {
			encodeBodyAsIs = true;
			me.setContentLen(s.bodyLen);
		}

		if (typeid(s) == typeid(Data))
			me.headLen = 0; // Prevent Data objects from actually writing a header
	}

	EncodingPlan::~EncodingPlan() {
		if (metadata) delete metadata;
		for (EncodingPlan* p : sub) delete p;
	}

	size_t EncodingPlan::total() {
		return me.segmentLen() + (metadata ? metadata->total() + metadata->freeSpace : 0);
	}

	// Segment ///////////////////////////////////////////////////////////

	unique_ptr<Segment> Segment::factory(byte* src, Segment* memOwner) {
		Segment* newSeg;
		Header h(src);
		byte* body = src + h.headLen;
		switch (h.type) {
		case SegmentType::STRING  : newSeg = new String  (body, h.contentLen, memOwner); break;
		case SegmentType::INTEGER : newSeg = new Integer (body, h.contentLen, memOwner); break;
		case SegmentType::FLOATING: newSeg = new Floating(body, h.contentLen, memOwner); break;
		case SegmentType::LIST    : newSeg = new List    (body, h.contentLen, memOwner); break;
		case SegmentType::KEYED   : newSeg = new Keyed   (body, h.contentLen, memOwner); break;
		case SegmentType::PATCH   : newSeg = new Patch   (body, h.contentLen, memOwner); break;
		case SegmentType::METADATA: {
			Metadata* md = new Metadata(body, h.contentLen, memOwner);
			newSeg = factory(src + h.segmentLen(), memOwner).release();
			{ // Get 'most' meta segment
				Segment* s = newSeg;
				while (s->metadata) s = s->metadata;
				s->setMetadata(md);
			}
			break;
		}
		case SegmentType::SYMBOL  : newSeg = new Symbol  ((Symbol::Symbols) h.immediate); break;
		default: throw runtime_error("type not implemented");
		}
		return unique_ptr<Segment>(newSeg);
	}

	unique_ptr<Segment> Segment::factory(istream& in) {
		Segment* newSeg;
		Header h(in);
		switch (h.type) {
		case SegmentType::STRING  : newSeg = new String  (in, h.contentLen); break;
		case SegmentType::INTEGER : newSeg = new Integer (in, h.contentLen); break;
		case SegmentType::FLOATING: newSeg = new Floating(in, h.contentLen); break;
		case SegmentType::LIST    : newSeg = new List    (in, h.contentLen); break;
		case SegmentType::KEYED   : newSeg = new Keyed   (in, h.contentLen); break;
		case SegmentType::PATCH   : newSeg = new Patch   (in, h.contentLen); break;
		case SegmentType::METADATA: {
			Metadata* md = new Metadata(in, h.contentLen);
			newSeg = factory(in).release();
			{ // Get 'most' meta segment
				Segment* s = newSeg;
				while (s->metadata) s = s->metadata;
				s->setMetadata(md);
			}
		}
		case SegmentType::SYMBOL  : newSeg = new Symbol((Symbol::Symbols)h.immediate); break;
		default: throw runtime_error("type not implemented");
		}
		return unique_ptr<Segment>(newSeg);
	}

	Segment::Segment(byte* body, size_t bodyLen, Segment* memOwner)
		: body(body)
		, bodyLen(bodyLen)
		, memOwner(memOwner) {
	}

	Segment::Segment(istream& in, size_t bodyLen)
		: body(new byte[bodyLen])
		, bodyLen(bodyLen)
		, memOwner(this) {
		in.read((char*) body, bodyLen);
	}

	Segment::~Segment() {
		if (memOwner == this) {
			byte* ownedMem = body;
			Segment* ptr = this;
			while (ptr->metadata && ptr->metadata->memOwner == this) {
				ownedMem = ptr->metadata->body; // metadata holds start of segment memory
				ptr = ptr->metadata;
			}
			delete[] ownedMem;
		}
		if (metadata) delete metadata;
	}

	unique_ptr<Segment> Segment::clone() {
		stringstream ss;
		ss << *this;
		unique_ptr<Segment> s;
		ss >> s;
		return s;
	}

	void Segment::becomeMemoryOwner() {
		if (!body) return; // no current encoding to own
		if (memOwner == this) return; // already is its own memory owner
		size_t mdRegion = 0;
		if (metadata)
			mdRegion += metadata->bodyLen + metadata->freeSpace;
		byte* newMem = new byte[mdRegion + bodyLen + freeSpace];
		if (metadata) {
			memcpy(newMem, metadata->body, metadata->bodyLen);
			Header::freeSegment(metadata->freeSpace).write(newMem + metadata->bodyLen);
		}
		byte* newBody = newMem + mdRegion;
		memcpy(newBody, body, bodyLen);
		Header::freeSegment(freeSpace).write(newBody + bodyLen);
		relocate(newMem, this); //changeBody(body, newBody);
	}

	void Segment::setFreeSpace(size_t amount) {
		if (!body) { freeSpace = amount; return; }
		if (!memOwner) {
			freeSpace = amount;
			becomeMemoryOwner();
			return;
		}
		throw runtime_error("Unimplemented");
	}

	byte* Segment::after() { return body + bodyLen + freeSpace; }

	void Segment::setMetadata(Metadata* md) {
		if (metadata) delete metadata;
		metadata = md;
	}

	Keyed* Segment::getMetadata(Segment& lang, bool createIfMissing) {
		if (!metadata) {
			if (!createIfMissing) return nullptr;
			Keyed& kv = *new Keyed << lang.clone().release();
			setMetadata(&(*new Metadata << &kv));
			return &kv;
		}
		for (Segment* s : *metadata) {
			Keyed* k = dynamic_cast<Keyed*>(s);
			if (!k) continue;
			Iter i = k->begin();
			if (!(i != k->end())) continue; // k is empty
			Segment& key = (*k)[0];
			if (key == lang) return k;
		}
		return nullptr;
	}

	Segment& Segment::with(Segment& lang, Segment& entry) {
		getMetadata(lang, true)->add(entry.clone().release());
		return *this;
	}

	size_t Segment::write(byte* dst, const EncodingPlan& plan) {
		size_t written = 0;
		if (plan.metadata)
			written += metadata->write(dst, *plan.metadata);
		written += plan.me.write(dst);
		if (plan.encodeBodyAsIs) {
			memcpy(dst + written, body, bodyLen);
			written += bodyLen;
		} else {
			for (size_t i = 0; i < count(); ++i)
				written += operator[](i).write(dst + written, *plan.sub[i]);
		}
		return written;
	}

	size_t Segment::write(ostream& out, const EncodingPlan& ht) {
		size_t written = 0;
		if (ht.metadata)
			written += metadata->write(out, *ht.metadata);
		written += ht.me.write(out);
		if (ht.encodeBodyAsIs) {
			out.write((const char*) body, bodyLen);
			written += bodyLen;
		} else {
			for (size_t i = 0; i < count(); ++i)
				written += operator[](i).write(out, *ht.sub[i]);
		}
		return written;
	}

	std::string Segment::toString() {
		std::stringstream ss;
		ss << nameOf(type());
		ss << ":" << bodyLen;
		return ss.str();
	}

	bool Segment::resize(size_t requestedBodyLen) {
		if (!memOwner) return false;
		return memOwner->resizeSubordinate(*this, requestedBodyLen);
	}

	bool Segment::resizeSubordinate(Segment& target, size_t requestedBodyLen) {
		if (target.bodyLen == requestedBodyLen) return true;

		if (&target == this) {
			throw runtime_error("unimplemented");
		}

		return false;
	}

	void Segment::relocate(byte* newMemory, Segment* newMemOwner) {
		size_t mdRegion = 0;
		if (metadata) {
			metadata->relocate(newMemory, newMemOwner);
			mdRegion = metadata->bodyLen + metadata->freeSpace;
		}
		body = newMemory + mdRegion;
		memOwner = newMemOwner;
	}

	Segment::operator int() const { return dynamic_cast<const Integer*>(this)->value<int>(); }
	Segment::operator std::string() const { return dynamic_cast<const String*>(this)->value(); }

	// Symbol ////////////////////////////////////////////////////////////

	Symbol::Symbol(Symbols s)
		: Segment(nullptr, 0, nullptr)
		, sym(s) {
	}

	bool Symbol::operator== (Segment& rhs) {
		Symbol* s = dynamic_cast<Symbol*>(&rhs);
		return s ? sym == s->sym : false;
	}

	std::string Symbol::toString() {
		std::stringstream ss;
		ss << nameOf(type()) << ":";
		switch (sym) {
		case False: ss << "False"; break;
		case True: ss << "True"; break;
		case Void: ss << "Void"; break;
		case Null: ss << "Null"; break;
		case Any: ss << "Any"; break;
		case Parameter: ss << "Parameter"; break;
		case Error: ss << "Error"; break;
		case MathConstant: ss << "Math Constant"; break;
		default: throw runtime_error("Symbol unimplemented");
		}
		return ss.str();
	}

	//size_t Symbol::write(ostream& out, bool eliminatingInternalFreeSpace) {
	//	size_t written = 0;
	//	written += Header(type(), (size_t) sym).write(out);
	//	// No body
	//	if (metadata) {
	//		if (!eliminatingInternalFreeSpace) {
	//			size_t fc = Header::freeSegment(freeSpace).write(out);
	//			for (; fc < freeSpace; ++fc)
	//				out.put(0);
	//			written += freeSpace;
	//		}
	//		written += metadata->write(out, eliminatingInternalFreeSpace);
	//	}
	//	return written;
	//}

	// String ////////////////////////////////////////////////////////////

	String::String(const char* str)
		: Segment((byte*)str, 0, nullptr) {
		bodyLen = strlen(str);
	}
	
	bool String::operator== (Segment& rhs) {
		String* s = dynamic_cast<String*>(&rhs);
		if (!s) return false;
		if (bodyLen != rhs.bodyLen) return false;
		for (size_t i = 0; i < bodyLen; ++i)
			if (body[i] != rhs.body[i]) return false;
		return true;
	}

	std::string String::toString() {
		std::string s = value();
		if (s.find(' ') == std::string::npos) return s;
		std::stringstream ss;
		ss << '`' << s << '\'';
		return ss.str();
	}

	// Integer ///////////////////////////////////////////////////////////

	Integer::Integer(int8_t val)
		: Segment(new byte[1], 1, this) {
		*reinterpret_cast<int8_t*>(body) = val;
	}

	Integer::Integer(int16_t val)
		: Segment(new byte[2], 2, this) {
		*reinterpret_cast<int16_t*>(body) = val;
	}

	Integer::Integer(int32_t val)
		: Segment(new byte[4], 4, this) {
		*reinterpret_cast<int32_t*>(body) = val;
	}

	Integer::Integer(int64_t val)
		: Segment(new byte[8], 8, this) {
		*reinterpret_cast<int64_t*>(body) = val;
	}

	bool Integer::operator== (Segment& rhs) {
		Integer* s = dynamic_cast<Integer*>(&rhs);
		if (!s) return false;
		return value<int64_t>() == s->value<int64_t>();
	}

	//bool Integer::write(int32_t v) {
	//	if (bodyLen == 4) { *reinterpret_cast<int*>(body) = v; return; }
	//	int bytesNeeded;
	//	     if (v <     128 && v >=     -128) bytesNeeded = 1;
	//	else if (v <   32767 && v >=   -32768) bytesNeeded = 2;
	//	//else if (v < 8388607 && v >= -8388608) bytesNeeded = 3;
	//	else                                   bytesNeeded = 4;
	//	//if (!resize(bytesNeeded)) return false;
	//	//if (bytesNeeded > bodyLen) ;
	//}

	void Integer::operator= (int32_t val) {
		if(bodyLen == 4) { *reinterpret_cast<int32_t*>(body) = val; return; }
		throw runtime_error("unimplemented");
	}

	std::string Integer::toString() {
		std::stringstream ss;
		ss << value<int>();
		return ss.str();
	}

	// Floating //////////////////////////////////////////////////////////

	Floating::Floating(float val)
		: Segment(new byte[4], 4, this) {
		*reinterpret_cast<float*>(body) = val;
	}

	Floating::Floating(double val)
		: Segment(new byte[8], 8, this) {
		*reinterpret_cast<double*>(body) = val;
	}

	bool Floating::operator== (Segment& rhs) {
		Floating* s = dynamic_cast<Floating*>(&rhs);
		if (!s) return false;
		return value<double>() == s->value<double>();
	}

	std::string Floating::toString() {
		std::stringstream ss;
		ss << value<double>();
		return ss.str();
	}

	// List //////////////////////////////////////////////////////////////

	bool Iter::operator!= (const Iter& rhs) const { return index != rhs.index; }

	void Iter::operator++ () { ++index; }

	Segment* Iter::operator* () const { return home->sub[index]; }

	List::List()
		: Segment(nullptr, 0, nullptr) {
		continueLoadingFrom = nullptr;
	}

	List::List(byte* body, size_t bodyLen, Segment* memOwner)
		: Segment(body, bodyLen, memOwner) {
		continueLoadingFrom = body;
		initChildren();
	}

	List::List(byte* body, size_t maxLength)
		: Segment(body, maxLength, nullptr) {
		continueLoadingFrom = body;
		initChildren();
	}

	List::~List() { for (Segment* s : sub) delete s; }

	void List::becomeMemoryOwner() {
		if (!body) return; // no current encoding to own
		if (memOwner == this) return; // already is its own memory owner
		for (Segment* s : *this) s->becomeMemoryOwner();
		if (metadata) metadata->becomeMemoryOwner();
		body = nullptr;
		bodyLen = 0;
		memOwner = nullptr;
	}

	bool List::operator== (Segment& rhs) {
		if (type() != rhs.type()) return false; // take care of List subclass polymorphism
		List* l = dynamic_cast<List*>(&rhs);
		if (!l) return false;
		Iter i1 = begin(), i2 = l->begin();
		Iter e1 = end(), e2 = l->end();
		while (i1 != e1 && i2 != e2 && **i1 == **i2) {
			++i1;
			++i2;
		}
		return !(i1 != e1) && !(i2 != e2); // must have made it to the end of both iterators
	}

	size_t List::count() const {
		if (continueLoadingFrom) const_cast<List*>(this)->initChildren();
		return sub.size();
	}

	std::string List::toString() {
		std::stringstream ss;
		ss << '[';
		int i = 0;
		for (Segment* s : sub) {
			if (i++) ss << ' ';
			ss << s->toString();
		}
		ss << ']';
		return ss.str();
	}

	void List::relocate(byte* newBody, Segment* newMemOwner) {
		for (Segment* s : sub) {
			size_t offset = s->body - body; // where was this child's body relative to parent (this)
			s->relocate(newBody + offset, newMemOwner); // keep same relative position
		}
		Segment::relocate(newBody, newMemOwner);
	}

	void List::initChildren() {
		sub.clear();
		Segment* prevChild = this;
		byte* src = body;
		byte* end = body + bodyLen;
		while (src < end) {
			Header h(src);
			if (src + h.segmentLen() > end)
				throw runtime_error("parse error");

			if (h.type == SegmentType::METADATA)
				prevChild->setMetadata(new Metadata(src + h.headLen, h.contentLen, memOwner));
			else if (h.type == SegmentType::FREE)
				freeSpace += h.segmentLen(); // temporarily assign to this, the container
			else {
				// Another child found. Give List's previously found freeSpace to the previous child
				if(prevChild != this) // ignore preceeding freespace
					prevChild->freeSpace = freeSpace;
				freeSpace = 0;
				
				sub.push_back(prevChild = factory(src, memOwner).release());
			}

			src += h.segmentLen();
		}
		continueLoadingFrom = nullptr;
	}

	void List::insert(Segment*const&& subtree, int index) {
		if (index < 0) index += count() + 1; // support negative indexing from rear

		if (body) {
			EncodingPlan plan = subtree->getEncodingPlan();
			//size_t segSize = subtree->getEncodingLength(false);
			size_t segSize = plan.total();
			bool ok = resize(bodyLen + segSize);
			if (!ok) throw runtime_error("unable to resize");

			byte* const newMem = body + bodyLen - segSize;
			byte* dst = (index == count()) ? newMem : sub[index - 1]->after();
			int shift = newMem - dst;
			if (shift) {
				memmove(dst + shift, dst, segSize);
				for (size_t i = index + 1; i < sub.size(); ++i)
					sub[i]->relocate(sub[i]->body + shift, memOwner); //sub[i]->bodyMoved(shift);
			}
			subtree->write(dst, plan);

			//subtree->relocate(dst + Header(dst).headLen, memOwner);
			subtree->relocate(dst + plan.me.headLen, memOwner);
		}

		sub.insert(sub.begin() + index, subtree);
	}

	Segment* List::remove(int index) {
		if (index < 0) index += count() + 1; // support negative indexing from rear
		Segment* s = sub[index];
		sub.erase(sub.begin() + index);
		s->becomeMemoryOwner();
		return s;
	}

	void List::add(const char* str) { insert(new String(str), -1); }

	// Data //////////////////////////////////////////////////////////////

	Data::Data()
		: List(nullptr, 0, this)
		, arraySize(0) {
	}

	Data::Data(size_t initialAllocation)
		: List(nullptr, 0, this) {
		body = new byte[initialAllocation];
		arraySize = initialAllocation;
		freeSpace = initialAllocation;
		Header::freeSegment(initialAllocation).write(body);
	}

	Data::Data(byte* src, size_t maxLength)
		: List(src, maxLength)
		, arraySize(maxLength) {
	}

	bool Data::resizeSubordinate(Segment& target, size_t requestedBodyLen) {
		if (target.bodyLen == requestedBodyLen) return true;

		if (&target == this) { // Tree root: no header to update
			if (requestedBodyLen < bodyLen) { // shrinking
				freeSpace += bodyLen - requestedBodyLen;
				bodyLen = requestedBodyLen;
				Header::freeSegment(freeSpace).write(body + bodyLen);
				return true;
			}
			if (freeSpace >= requestedBodyLen) {
				freeSpace -= requestedBodyLen - bodyLen;
				bodyLen = requestedBodyLen;
				Header::freeSegment(freeSpace).write(body + bodyLen);
				return true;
			}
			throw runtime_error("unimplemented functionality");
			//TODO: resize array. changeBody();
		}

		byte* currHead = body;
		size_t targetIndex;

		struct PathEntry {
			byte* head;
			List* container;
			size_t indexInParent;
		};
		stack<PathEntry> path;
		path.push(PathEntry{ body, this, 0 });

		// Get position of target's head (to calc headLen) and get ancestors along the way
		bool found = false;
		while (!found) {
			//TODO: make this a binary search
			//vector<Segment*>& children = path.top().container->sub;
			//for (size_t i = 0; i < children.size(); ++i) {
			//	Segment* s = children.at(i);
			//	if (s == &target) { targetIndex = i; found = true; break; }
			//	if (s->body + s->bodyLen > target.body) {
			//		path.push(PathEntry{ currHead, (List*)s, i });
			//		break;
			//	}
			//	currHead = s->body + s->bodyLen;
			//}
			size_t i = 0;
			for(Segment* s : *path.top().container) {
				if (s == &target) { targetIndex = i; found = true; break; }
				if (s->body + s->bodyLen > target.body) {
					path.push(PathEntry{ currHead, (List*)s, i });
					break;
				}
				currHead = s->body + s->bodyLen;
				++i;
			}
			if (currHead >= body + bodyLen) // -> for-loop completed without 'break'
				return false; // target not in any subtree
		}

		// State: target's head == currHead

		size_t currHeadLen = target.body - currHead;
		size_t currSpace = currHeadLen + target.bodyLen;
		Header newHeader(target.type(), requestedBodyLen);
		size_t goalSpace = newHeader.segmentLen();

		if (goalSpace < currSpace) { // shrinking
			newHeader.write(currHead);
			if (newHeader.headLen != currHeadLen)
				memmove(currHead + newHeader.headLen, target.body, requestedBodyLen);
			target.body = currHead + newHeader.headLen;
			target.bodyLen = requestedBodyLen;
			target.freeSpace += currSpace - goalSpace;
			Header::freeSegment(target.freeSpace).write(target.body + target.bodyLen);
			return true;
		}

		if (goalSpace > currSpace + target.freeSpace) { // Transfer freeSpace recursively down from parent
			size_t additionalFreespaceNeeded = goalSpace - currSpace - target.freeSpace;
			additionalFreespaceNeeded += (path.size()-1) * 2; // padding to avoid complexity from increasing headerLengths at each layer (may still not be sufficicent if moving a large amount)

			// Find closest free space threshold
			size_t found = path.top().container->freeSpace; // immediate parent
			stack<PathEntry> backDown;
			while (found < additionalFreespaceNeeded && path.size() > 1) {
				path.pop();
				found += path.top().container->freeSpace;
				backDown.push(path.top());
			}

			if (found < additionalFreespaceNeeded)
				return false; //TODO: reallocate top level array

			// Pass freeSpace down
			PathEntry from = path.top();
			while(!backDown.empty()) {
				PathEntry to = backDown.top();
				backDown.pop();
				//updateSegmentLayout(*e.container, e.head,
				//	e.container->bodyLen + additionalFreespaceNeeded, // newBodyLen
				//	); // newSegmentTotal
				transferFreeSpaceToChild(from.container, from.head, to.indexInParent, additionalFreespaceNeeded);
				from = to;
			}
			transferFreeSpaceToChild(from.container, from.head, targetIndex, additionalFreespaceNeeded);
		}

		if (currSpace + target.freeSpace < goalSpace) return false;

		// Pull from freespace
		if (newHeader.headLen > currHeadLen) { // make room for new header
			memmove(currHead + newHeader.headLen, target.body, target.bodyLen);
			target.body = currHead + newHeader.headLen;
		}
		newHeader.write(currHead);
		target.freeSpace -= requestedBodyLen - target.bodyLen;
		target.bodyLen = requestedBodyLen;
		Header::freeSegment(target.freeSpace).write(target.body + target.bodyLen);
		return true;
	}

	size_t Data::transferFreeSpaceToChild(List* from, byte* fromHead, size_t toIndex, size_t amount) {
		//if (toIndex >= from->count()) return 0;
		if (from->freeSpace < amount) amount = from->freeSpace;
		if (amount == 0) return 0;
		size_t currHeadLen = from->body - fromHead;
		if(currHeadLen) { // this != a Data
			Header newHeader(from->type(), from->bodyLen + amount);
			int headLenChange = (int) newHeader.headLen - (int) currHeadLen;
			if (headLenChange) {
				memmove(fromHead + newHeader.headLen, fromHead + currHeadLen, from->bodyLen);
				from->relocate(from->body + headLenChange, from->memOwner); //from->bodyMoved(headLenChange);
			}
			newHeader.write(fromHead);
			if (headLenChange > 0) amount -= headLenChange; // 'shrink'
		}
		from->freeSpace -= amount;
		from->bodyLen += amount;

		Segment* to = from->sub[toIndex];
		if(toIndex + 1 < from->count()) { // move trailing children over
			byte* nextHead = to->after();
			memmove(nextHead + amount, nextHead, from->body + from->bodyLen - nextHead);
			for (size_t i = toIndex + 1; i < from->sub.size(); ++i)
				from->sub[i]->relocate(from->sub[i]->body + amount, from->sub[i]->memOwner); //from->sub[i]->bodyMoved(amount);
		}
		to->freeSpace += amount;
		Header::freeSegment(from->freeSpace).write(from->body + from->bodyLen);
		return amount;
	}

	//////////////////////////////////////////////////////////////////////

	ostream& operator<< (ostream& out, Segment& s) { EncodingPlan plan = s.getEncodingPlan(true); s.write(out, plan); return out; }

	istream& operator>> (istream& in, unique_ptr<Segment>& s) { s = Segment::factory(in); return in; }

	istream& operator>> (istream& in, List& l) { l.add(Segment::factory(in).release()); return in; }

}
