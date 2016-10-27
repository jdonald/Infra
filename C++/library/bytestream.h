#pragma once

#include <streambuf>
#include <istream>

struct membuf : std::streambuf {
	membuf(char* begin, char* end) {
		setg(begin, begin, end);
	}

	int_type underflow() override {
		return  gptr() == egptr() ?
			traits_type::eof() :
			traits_type::to_int_type(*gptr());
	}
};

class bytestream : public std::istream {
public:
	bytestream(char* source, size_t size) :
		std::istream(new membuf(source, source+size))
	{}
};
