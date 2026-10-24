/**
 * MetaShield - C++ Anti-Corruption Binary Stream Guard
 * High-Performance Integrity & Verification Core
 * Autonomously engineered for Charan Konda (2511CS040113) & J. Naga Phani (2511CS040132)
 * Malla Reddy University - CS BETA - 17
 */

#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <sstream>
#include <iomanip>
#include <cstdint>
#include <cstring>
#include <algorithm>

// Standalone portable SHA-256 implementation
namespace sha256 {
    static const uint32_t K[64] = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    inline uint32_t rotr(uint32_t x, uint32_t n) { return (x >> n) | (x << (32 - n)); }
    inline uint32_t ch(uint32_t x, uint32_t y, uint32_t z) { return (x & y) ^ (~x & z); }
    inline uint32_t maj(uint32_t x, uint32_t y, uint32_t z) { return (x & y) ^ (x & z) ^ (y & z); }
    inline uint32_t sig0(uint32_t x) { return rotr(x, 2) ^ rotr(x, 13) ^ rotr(x, 22); }
    inline uint32_t sig1(uint32_t x) { return rotr(x, 6) ^ rotr(x, 11) ^ rotr(x, 25); }
    inline uint32_t theta0(uint32_t x) { return rotr(x, 7) ^ rotr(x, 18) ^ (x >> 3); }
    inline uint32_t theta1(uint32_t x) { return rotr(x, 17) ^ rotr(x, 19) ^ (x >> 10); }

    std::string compute(const std::vector<uint8_t>& data) {
        uint32_t H[8] = {
            0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
            0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
        };

        uint64_t bit_len = (uint64_t)data.size() * 8;
        std::vector<uint8_t> msg = data;
        msg.push_back(0x80);

        while ((msg.size() * 8) % 512 != 448) {
            msg.push_back(0x00);
        }

        for (int i = 7; i >= 0; --i) {
            msg.push_back((uint8_t)((bit_len >> (i * 8)) & 0xff));
        }

        for (size_t chunk = 0; chunk < msg.size(); chunk += 64) {
            uint32_t W[64];
            for (int t = 0; t < 16; ++t) {
                W[t] = ((uint32_t)msg[chunk + t * 4] << 24) |
                       ((uint32_t)msg[chunk + t * 4 + 1] << 16) |
                       ((uint32_t)msg[chunk + t * 4 + 2] << 8) |
                       ((uint32_t)msg[chunk + t * 4 + 3]);
            }
            for (int t = 16; t < 64; ++t) {
                W[t] = theta1(W[t - 2]) + W[t - 7] + theta0(W[t - 15]) + W[t - 16];
            }

            uint32_t a = H[0], b = H[1], c = H[2], d = H[3];
            uint32_t e = H[4], f = H[5], g = H[6], h = H[7];

            for (int t = 0; t < 64; ++t) {
                uint32_t T1 = h + sig1(e) + ch(e, f, g) + K[t] + W[t];
                uint32_t T2 = sig0(a) + maj(a, b, c);
                h = g;
                g = f;
                f = e;
                e = d + T1;
                d = c;
                c = b;
                b = a;
                a = T1 + T2;
            }

            H[0] += a; H[1] += b; H[2] += c; H[3] += d;
            H[4] += e; H[5] += f; H[6] += g; H[7] += h;
        }

        std::ostringstream ss;
        for (int i = 0; i < 8; ++i) {
            ss << std::hex << std::setw(8) << std::setfill('0') << H[i];
        }
        return ss.str();
    }
}

struct StreamValidationResult {
    bool is_valid;
    std::string format;
    size_t byte_size;
    std::string sha256_hash;
    std::string magic_bytes;
    std::string integrity_status;
    std::string error_detail;
};

StreamValidationResult validate_file_stream(const std::string& path) {
    StreamValidationResult res;
    res.is_valid = false;
    res.format = "UNKNOWN";
    res.byte_size = 0;
    res.sha256_hash = "";
    res.magic_bytes = "";
    res.integrity_status = "CORRUPTED";
    res.error_detail = "";

    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) {
        res.error_detail = "Failed to open file: " + path;
        return res;
    }

    file.seekg(0, std::ios::end);
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);

    if (size <= 0) {
        res.error_detail = "Empty file stream";
        return res;
    }

    res.byte_size = (size_t)size;
    std::vector<uint8_t> buffer((size_t)size);
    if (!file.read((char*)buffer.data(), size)) {
        res.error_detail = "Failed to read full byte stream";
        return res;
    }

    res.sha256_hash = sha256::compute(buffer);

    // Format Inspection & Delimiter Checks
    // JPEG Check (SOI 0xFFD8, EOI 0xFFD9)
    if (size >= 4 && buffer[0] == 0xFF && buffer[1] == 0xD8) {
        res.format = "JPEG";
        res.magic_bytes = "FF D8";
        // Check for JPEG EOI marker near end
        bool has_eoi = false;
        size_t search_start = size > 256 ? size - 256 : 0;
        for (size_t i = search_start; i < (size_t)size - 1; ++i) {
            if (buffer[i] == 0xFF && buffer[i + 1] == 0xD9) {
                has_eoi = true;
                break;
            }
        }
        if (has_eoi) {
            res.is_valid = true;
            res.integrity_status = "VALID_STREAM";
        } else {
            res.error_detail = "JPEG EOI (End of Image 0xFFD9) delimiter missing";
        }
    }
    // PNG Check (89 50 4E 47 0D 0A 1A 0A, and IEND chunk)
    else if (size >= 8 && buffer[0] == 0x89 && buffer[1] == 'P' && buffer[2] == 'N' && buffer[3] == 'G') {
        res.format = "PNG";
        res.magic_bytes = "89 50 4E 47";
        // Look for IEND chunk marker
        bool has_iend = false;
        size_t search_start = size > 64 ? size - 64 : 0;
        for (size_t i = search_start; i < (size_t)size - 4; ++i) {
            if (buffer[i] == 'I' && buffer[i+1] == 'E' && buffer[i+2] == 'N' && buffer[i+3] == 'D') {
                has_iend = true;
                break;
            }
        }
        if (has_iend) {
            res.is_valid = true;
            res.integrity_status = "VALID_STREAM";
        } else {
            res.error_detail = "PNG IEND chunk termination marker missing";
        }
    }
    // PDF Check (%PDF- header and %EOF trailer)
    else if (size >= 5 && buffer[0] == '%' && buffer[1] == 'P' && buffer[2] == 'D' && buffer[3] == 'F' && buffer[4] == '-') {
        res.format = "PDF";
        res.magic_bytes = "%PDF-";
        bool has_eof = false;
        size_t search_start = size > 1024 ? size - 1024 : 0;
        for (size_t i = search_start; i < (size_t)size - 4; ++i) {
            if (buffer[i] == '%' && buffer[i+1] == '%' && buffer[i+2] == 'E' && buffer[i+3] == 'O' && buffer[i+4] == 'F') {
                has_eof = true;
                break;
            }
        }
        if (has_eof) {
            res.is_valid = true;
            res.integrity_status = "VALID_STREAM";
        } else {
            res.error_detail = "PDF %%EOF trailer delimiter missing or truncated";
        }
    }
    // ZIP / OOXML Check (PK 0x03 0x04)
    else if (size >= 4 && buffer[0] == 'P' && buffer[1] == 'K' && buffer[2] == 0x03 && buffer[3] == 0x04) {
        res.format = "OOXML/ZIP";
        res.magic_bytes = "PK 03 04";
        // Check for End of Central Directory record (0x50 0x4B 0x05 0x06)
        bool has_eocd = false;
        size_t search_start = size > 1024 ? size - 1024 : 0;
        for (size_t i = search_start; i < (size_t)size - 3; ++i) {
            if (buffer[i] == 'P' && buffer[i+1] == 'K' && buffer[i+2] == 0x05 && buffer[i+3] == 0x06) {
                has_eocd = true;
                break;
            }
        }
        if (has_eocd) {
            res.is_valid = true;
            res.integrity_status = "VALID_STREAM";
        } else {
            res.error_detail = "ZIP End of Central Directory record missing";
        }
    }
    else {
        // Generic binary
        res.format = "GENERIC_BINARY";
        res.magic_bytes = "RAW";
        res.is_valid = true;
        res.integrity_status = "VALID_STREAM";
    }

    return res;
}

std::string format_json(const StreamValidationResult& r) {
    std::ostringstream ss;
    ss << "{"
       << "\"is_valid\":" << (r.is_valid ? "true" : "false") << ","
       << "\"format\":\"" << r.format << "\","
       << "\"byte_size\":" << r.byte_size << ","
       << "\"sha256\":\"" << r.sha256_hash << "\","
       << "\"magic_bytes\":\"" << r.magic_bytes << "\","
       << "\"integrity_status\":\"" << r.integrity_status << "\","
       << "\"error_detail\":\"" << r.error_detail << "\""
       << "}";
    return ss.str();
}

int main(int argc, char* argv[]) {
    // Mode 1: One-shot CLI verify
    if (argc >= 3 && std::string(argv[1]) == "--verify") {
        std::string filepath = argv[2];
        StreamValidationResult res = validate_file_stream(filepath);
        std::cout << format_json(res) << std::endl;
        return res.is_valid ? 0 : 1;
    }

    // Mode 2: Persistent Daemon Mode (Line-delimited JSON stream)
    if (argc >= 2 && std::string(argv[1]) == "--daemon") {
        std::string line;
        while (std::getline(std::cin, line)) {
            if (line.empty()) continue;
            if (line == "PING") {
                std::cout << "PONG" << std::endl;
                continue;
            }
            if (line == "QUIT") {
                break;
            }
            // Parse simple filepath or json
            std::string filepath = line;
            size_t p = line.find("\"file\":\"");
            if (p != std::string::npos) {
                size_t start = p + 8;
                size_t end = line.find("\"", start);
                if (end != std::string::npos) {
                    filepath = line.substr(start, end - start);
                }
            }
            StreamValidationResult res = validate_file_stream(filepath);
            std::cout << format_json(res) << std::endl;
        }
        return 0;
    }

    std::cout << "Usage: stream_guard --verify <file_path> | --daemon" << std::endl;
    return 0;
}
