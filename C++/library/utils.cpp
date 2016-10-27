#include "utils.hpp"

#include <string>

namespace Utils {

	std::string byte_2_str(char* bytes, size_t size) {
		static const char hex[16] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

		std::string str;
		str.reserve(2 + size * 3);
		for (size_t i = 0; i < size; ++i) {
			if (i) str.append(" ", 1);
			const char ch = bytes[i];
			str.append(&hex[(ch & 0xF0) >> 4], 1);
			str.append(&hex[ch & 0xF], 1);
		}
		return str;
	}

}
