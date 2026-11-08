package com.redops.mobile.data.repository

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.redops.mobile.data.remote.ApiClient
import com.redops.mobile.data.remote.model.*
import com.redops.mobile.domain.repository.ToolkitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

class ToolkitRepositoryImpl(
    private val apiClient: ApiClient? = null,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) : ToolkitRepository {

    override suspend fun resolveDns(domain: String, types: List<String>?, authorized: Boolean): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            try {
                if (apiClient == null) return@withContext Result.failure(Exception("ApiClient not configured"))
                val resp = apiClient.getService().resolveDns(DnsResolveRequest(domain, types, authorized))
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    Result.success(resp.body()!!.data!!)
                } else {
                    val err = resp.body()?.error ?: resp.errorBody()?.string() ?: "DNS lookup failed (${resp.code()})"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error connecting to backend: ${e.message}", e))
            }
        }

    override suspend fun reverseDns(ip: String, authorized: Boolean): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            try {
                if (apiClient == null) return@withContext Result.failure(Exception("ApiClient not configured"))
                val resp = apiClient.getService().reverseDns(ReverseDnsRequest(ip, authorized))
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    Result.success(resp.body()!!.data!!)
                } else {
                    val err = resp.body()?.error ?: resp.errorBody()?.string() ?: "Reverse DNS lookup failed (${resp.code()})"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error connecting to backend: ${e.message}", e))
            }
        }

    override suspend fun rdapLookup(domain: String, authorized: Boolean): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            try {
                if (apiClient == null) return@withContext Result.failure(Exception("ApiClient not configured"))
                val resp = apiClient.getService().rdapLookup(RdapRequest(domain, authorized))
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    Result.success(resp.body()!!.data!!)
                } else {
                    val err = resp.body()?.error ?: resp.errorBody()?.string() ?: "RDAP lookup failed (${resp.code()})"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error connecting to backend: ${e.message}", e))
            }
        }

    override suspend fun getIpInfo(ip: String, authorized: Boolean): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            try {
                if (apiClient == null) return@withContext Result.failure(Exception("ApiClient not configured"))
                val resp = apiClient.getService().getIpInfo(IpInfoRequest(ip, authorized))
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    Result.success(resp.body()!!.data!!)
                } else {
                    val err = resp.body()?.error ?: resp.errorBody()?.string() ?: "IP lookup failed (${resp.code()})"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error connecting to backend: ${e.message}", e))
            }
        }

    override suspend fun calculateSubnet(cidr: String): Result<SubnetResultDto> =
        withContext(Dispatchers.Default) {
            try {
                // Try remote API first if configured
                apiClient?.let { client ->
                    try {
                        val resp = client.getService().calculateSubnet(SubnetRequest(cidr))
                        if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                            return@withContext Result.success(resp.body()!!.data!!)
                        }
                    } catch (_: Exception) {}
                }
                // Native RFC-compliant subnet computation
                val parts = cidr.trim().split("/")
                if (parts.size != 2) throw IllegalArgumentException("CIDR must be in format <ip>/<prefix> (e.g. 192.168.1.0/24)")
                val ipStr = parts[0]
                val prefix = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid prefix: ${parts[1]}")
                if (prefix < 0 || prefix > 32) throw IllegalArgumentException("Prefix must be between 0 and 32")

                val ipOctets = ipStr.split(".").map { it.toIntOrNull() ?: throw IllegalArgumentException("Invalid IP: $ipStr") }
                if (ipOctets.size != 4 || ipOctets.any { it < 0 || it > 255 }) throw IllegalArgumentException("Invalid IPv4 address: $ipStr")

                val ipInt = (ipOctets[0].toLong() shl 24) or (ipOctets[1].toLong() shl 16) or (ipOctets[2].toLong() shl 8) or ipOctets[3].toLong()
                val maskInt = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
                val wildcardInt = maskInt.inv() and 0xFFFFFFFFL
                val netInt = ipInt and maskInt
                val bcastInt = netInt or wildcardInt

                fun intToIp(v: Long) = "${(v shr 24) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 8) and 0xFF}.${v and 0xFF}"

                val totalHosts = if (prefix == 32) 1L else (1L shl (32 - prefix))
                val usableHosts = if (prefix >= 31) totalHosts else (totalHosts - 2).coerceAtLeast(0)
                val firstHost = if (prefix >= 31) intToIp(netInt) else intToIp(netInt + 1)
                val lastHost = if (prefix >= 31) intToIp(bcastInt) else intToIp(bcastInt - 1)

                val firstOctet = ipOctets[0]
                val ipClass = when {
                    firstOctet in 1..126 -> "Class A"
                    firstOctet in 128..191 -> "Class B"
                    firstOctet in 192..223 -> "Class C"
                    firstOctet in 224..239 -> "Class D (Multicast)"
                    else -> "Class E (Reserved)"
                }
                val isPrivate = (firstOctet == 10) ||
                        (firstOctet == 172 && ipOctets[1] in 16..31) ||
                        (firstOctet == 192 && ipOctets[1] == 168)

                Result.success(
                    SubnetResultDto(
                        cidr = cidr,
                        ip = ipStr,
                        prefixLength = prefix,
                        netmask = intToIp(maskInt),
                        wildcard = intToIp(wildcardInt),
                        networkAddress = intToIp(netInt),
                        broadcastAddress = intToIp(bcastInt),
                        firstHost = firstHost,
                        lastHost = lastHost,
                        totalHosts = totalHosts,
                        usableHosts = usableHosts,
                        ipClass = ipClass,
                        isPrivate = isPrivate
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun analyzeHeaders(url: String, followRedirects: Boolean, authorized: Boolean): Result<HttpHeadersResultDto> =
        withContext(Dispatchers.IO) {
            try {
                val resp = apiClient.getService().analyzeHeaders(HttpHeadersRequest(url, followRedirects, authorized))
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    Result.success(resp.body()!!.data!!)
                } else {
                    val err = resp.body()?.error ?: resp.errorBody()?.string() ?: "Headers analysis failed (${resp.code()})"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error connecting to backend: ${e.message}", e))
            }
        }

    override suspend fun inspectTlsCert(host: String, port: Int, authorized: Boolean): Result<TlsCertResultDto> =
        withContext(Dispatchers.IO) {
            try {
                val resp = apiClient.getService().inspectTlsCert(TlsCertRequest(host, port, authorized))
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    Result.success(resp.body()!!.data!!)
                } else {
                    val err = resp.body()?.error ?: resp.errorBody()?.string() ?: "TLS certificate inspection failed (${resp.code()})"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error connecting to backend: ${e.message}", e))
            }
        }

    override suspend fun traceRedirects(url: String, authorized: Boolean): Result<List<RedirectHopDto>> =
        withContext(Dispatchers.IO) {
            try {
                val resp = apiClient.getService().traceRedirects(RedirectTraceRequest(url, authorized))
                if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                    Result.success(resp.body()!!.data!!)
                } else {
                    val err = resp.body()?.error ?: resp.errorBody()?.string() ?: "Redirect trace failed (${resp.code()})"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error connecting to backend: ${e.message}", e))
            }
        }

    override suspend fun analyzeUrl(url: String): Result<UrlAnalyzeResultDto> =
        withContext(Dispatchers.Default) {
            try {
                try {
                    val resp = apiClient.getService().analyzeUrl(UrlAnalyzeRequest(url))
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        return@withContext Result.success(resp.body()!!.data!!)
                    }
                } catch (_: Exception) {}

                val parsed = URL(if (!url.startsWith("http://") && !url.startsWith("https://")) "http://$url" else url)
                val paramsMap = mutableMapOf<String, String>()
                val query = parsed.query
                if (!query.isNullOrEmpty()) {
                    query.split("&").forEach { pair ->
                        val parts = pair.split("=", limit = 2)
                        if (parts.size == 2) {
                            paramsMap[URLDecoder.decode(parts[0], "UTF-8")] = URLDecoder.decode(parts[1], "UTF-8")
                        } else if (parts.size == 1) {
                            paramsMap[URLDecoder.decode(parts[0], "UTF-8")] = ""
                        }
                    }
                }

                val notes = mutableListOf<String>()
                if (parsed.protocol == "http") {
                    notes.add("Cleartext HTTP protocol in use; lacks TLS confidentiality and integrity.")
                }
                if (paramsMap.any { it.key.contains("token", true) || it.key.contains("key", true) || it.key.contains("pwd", true) }) {
                    notes.add("Sensitive credentials detected in query parameters; susceptible to shoulder-surfing, browser history leakage, and access log exposure.")
                }
                if (parsed.port != -1 && parsed.port !in listOf(80, 443)) {
                    notes.add("Non-standard port (${parsed.port}) observed.")
                }

                Result.success(
                    UrlAnalyzeResultDto(
                        url = url,
                        protocol = parsed.protocol,
                        hostname = parsed.host,
                        port = if (parsed.port == -1) (if (parsed.protocol == "https") "443" else "80") else parsed.port.toString(),
                        pathname = parsed.path.ifEmpty { "/" },
                        search = if (query != null) "?$query" else "",
                        hash = parsed.ref ?: "",
                        params = paramsMap,
                        securityNotes = notes
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun convertEncoding(input: String, from: String, to: String): Result<CryptoConvertResultDto> =
        withContext(Dispatchers.Default) {
            try {
                try {
                    val resp = apiClient.getService().convertEncoding(CryptoConvertRequest(input, from, to))
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        return@withContext Result.success(resp.body()!!.data!!)
                    }
                } catch (_: Exception) {}

                // Decode to raw bytes
                val rawBytes: ByteArray = when (from.lowercase()) {
                    "plain", "text" -> input.toByteArray(StandardCharsets.UTF_8)
                    "base64" -> Base64.getDecoder().decode(input.trim())
                    "hex" -> {
                        val clean = input.replace(" ", "").replace("0x", "")
                        ByteArray(clean.length / 2) { i ->
                            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                        }
                    }
                    "url" -> URLDecoder.decode(input, "UTF-8").toByteArray(StandardCharsets.UTF_8)
                    else -> input.toByteArray(StandardCharsets.UTF_8)
                }

                // Encode to destination
                val output: String = when (to.lowercase()) {
                    "plain", "text" -> String(rawBytes, StandardCharsets.UTF_8)
                    "base64" -> Base64.getEncoder().encodeToString(rawBytes)
                    "hex" -> rawBytes.joinToString("") { "%02x".format(it) }
                    "url" -> URLEncoder.encode(String(rawBytes, StandardCharsets.UTF_8), "UTF-8")
                    "binary" -> rawBytes.joinToString(" ") { b ->
                        String.format("%8s", Integer.toBinaryString(b.toInt() and 0xFF)).replace(' ', '0')
                    }
                    else -> String(rawBytes, StandardCharsets.UTF_8)
                }

                Result.success(CryptoConvertResultDto(input, output, from, to))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun generateHash(text: String, algorithm: String, hmacKey: String?): Result<HashResultDto> =
        withContext(Dispatchers.Default) {
            try {
                try {
                    val resp = apiClient.getService().generateHash(HashGenerateRequest(text, algorithm, hmacKey))
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        return@withContext Result.success(resp.body()!!.data!!)
                    }
                } catch (_: Exception) {}

                val algoUpper = algorithm.uppercase().replace("-", "")
                val (jceAlgo, isHmac) = if (!hmacKey.isNullOrEmpty()) {
                    when (algoUpper) {
                        "MD5" -> "HmacMD5" to true
                        "SHA1" -> "HmacSHA1" to true
                        "SHA256" -> "HmacSHA256" to true
                        "SHA512" -> "HmacSHA512" to true
                        else -> "HmacSHA256" to true
                    }
                } else {
                    when (algoUpper) {
                        "MD5" -> "MD5" to false
                        "SHA1" -> "SHA-1" to false
                        "SHA256" -> "SHA-256" to false
                        "SHA512" -> "SHA-512" to false
                        else -> "SHA-256" to false
                    }
                }

                val digestBytes: ByteArray = if (isHmac) {
                    val keySpec = SecretKeySpec(hmacKey!!.toByteArray(StandardCharsets.UTF_8), jceAlgo)
                    val mac = Mac.getInstance(jceAlgo).apply { init(keySpec) }
                    mac.doFinal(text.toByteArray(StandardCharsets.UTF_8))
                } else {
                    val md = MessageDigest.getInstance(jceAlgo)
                    md.digest(text.toByteArray(StandardCharsets.UTF_8))
                }

                val hashHex = digestBytes.joinToString("") { "%02x".format(it) }
                Result.success(HashResultDto(algorithm = algorithm, hash = hashHex, isHmac = isHmac))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun identifyHash(hash: String): Result<List<HashMatchDto>> =
        withContext(Dispatchers.Default) {
            try {
                try {
                    val resp = apiClient.getService().identifyHash(HashIdentifyRequest(hash))
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        return@withContext Result.success(resp.body()!!.data!!)
                    }
                } catch (_: Exception) {}

                val clean = hash.trim()
                val matches = mutableListOf<HashMatchDto>()
                val len = clean.length

                if (clean.matches(Regex("^[0-9a-fA-F]{32}$"))) {
                    matches.add(HashMatchDto("MD5", "HIGH", "32 hex characters standard digest"))
                    matches.add(HashMatchDto("NTLM", "MEDIUM", "Windows NT Lan Manager hash (MD4-based)"))
                } else if (clean.matches(Regex("^[0-9a-fA-F]{40}$"))) {
                    matches.add(HashMatchDto("SHA-1", "HIGH", "40 hex characters digest"))
                    matches.add(HashMatchDto("RIPEMD-160", "MEDIUM", "160-bit cryptographic hash"))
                } else if (clean.matches(Regex("^[0-9a-fA-F]{64}$"))) {
                    matches.add(HashMatchDto("SHA-256", "HIGH", "256-bit SHA-2 family digest"))
                    matches.add(HashMatchDto("HMAC-SHA256", "MEDIUM", "HMAC output or Keccak-256"))
                } else if (clean.matches(Regex("^[0-9a-fA-F]{128}$"))) {
                    matches.add(HashMatchDto("SHA-512", "HIGH", "512-bit SHA-2 family digest"))
                    matches.add(HashMatchDto("Whirlpool", "LOW", "512-bit hash function"))
                } else if (clean.startsWith("\$2a\$") || clean.startsWith("\$2b\$") || clean.startsWith("\$2y\$")) {
                    matches.add(HashMatchDto("bcrypt", "VERY HIGH", "OpenBSD Blowfish-based adaptive password hash"))
                } else if (clean.startsWith("\$6\$")) {
                    matches.add(HashMatchDto("SHA-512 Crypt", "VERY HIGH", "glibc standard salted SHA-512 crypt"))
                } else {
                    matches.add(HashMatchDto("Unknown", "LOW", "Does not match standard hex or modular crypt formats"))
                }

                Result.success(matches)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun calculateCvss(metrics: CvssMetrics): Result<CvssResultDto> =
        withContext(Dispatchers.Default) {
            try {
                try {
                    val resp = apiClient.getService().calculateCvss(CvssCalculateRequest(metrics))
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        return@withContext Result.success(resp.body()!!.data!!)
                    }
                } catch (_: Exception) {}

                // CVSS v3.1 Specification Formula
                val avVal = when (metrics.av) { "N" -> 0.85; "A" -> 0.62; "L" -> 0.55; "P" -> 0.2; else -> 0.85 }
                val acVal = when (metrics.ac) { "L" -> 0.77; "H" -> 0.44; else -> 0.77 }
                val prVal = when (metrics.pr) {
                    "N" -> 0.85
                    "L" -> if (metrics.s == "C") 0.68 else 0.62
                    "H" -> if (metrics.s == "C") 0.50 else 0.27
                    else -> 0.85
                }
                val uiVal = when (metrics.ui) { "N" -> 0.85; "R" -> 0.62; else -> 0.85 }
                val cVal = when (metrics.c) { "H" -> 0.56; "L" -> 0.22; else -> 0.0 }
                val iVal = when (metrics.i) { "H" -> 0.56; "L" -> 0.22; else -> 0.0 }
                val aVal = when (metrics.a) { "H" -> 0.56; "L" -> 0.22; else -> 0.0 }

                val iss = 1.0 - ((1.0 - cVal) * (1.0 - iVal) * (1.0 - aVal))
                val impact = if (metrics.s == "U") {
                    6.42 * iss
                } else {
                    7.52 * (iss - 0.029) - 3.25 * (iss - 0.02).pow(15)
                }
                val exploitability = 8.22 * avVal * acVal * prVal * uiVal

                val score = if (impact <= 0.0) {
                    0.0
                } else if (metrics.s == "U") {
                    min(round((impact + exploitability) * 10) / 10, 10.0)
                } else {
                    min(round((1.08 * (impact + exploitability)) * 10) / 10, 10.0)
                }

                val severity = when {
                    score == 0.0 -> "None"
                    score <= 3.9 -> "Low"
                    score <= 6.9 -> "Medium"
                    score <= 8.9 -> "High"
                    else -> "Critical"
                }
                val vector = "CVSS:3.1/AV:${metrics.av}/AC:${metrics.ac}/PR:${metrics.pr}/UI:${metrics.ui}/S:${metrics.s}/C:${metrics.c}/I:${metrics.i}/A:${metrics.a}"

                Result.success(CvssResultDto(baseScore = score, severity = severity, vector = vector))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun decodeJwt(token: String): Result<JsonObject> =
        withContext(Dispatchers.Default) {
            try {
                try {
                    val resp = apiClient.getService().decodeJwt(JwtDecodeRequest(token))
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        return@withContext Result.success(resp.body()!!.data!!)
                    }
                } catch (_: Exception) {}

                val parts = token.trim().split(".")
                if (parts.size < 2) throw IllegalArgumentException("Invalid JWT token format; expected header.payload.signature")

                fun decodePart(part: String): JsonObject {
                    val clean = part.replace("-", "+").replace("_", "/")
                    val padLen = (4 - (clean.length % 4)) % 4
                    val padded = clean + "=".repeat(padLen)
                    val jsonStr = String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8)
                    return JsonParser.parseString(jsonStr).asJsonObject
                }

                val headerObj = decodePart(parts[0])
                val payloadObj = decodePart(parts[1])
                val result = JsonObject().apply {
                    add("header", headerObj)
                    add("payload", payloadObj)
                    addProperty("signature", if (parts.size >= 3) parts[2] else "")
                    val exp = payloadObj.get("exp")?.asLong
                    if (exp != null) {
                        val isExpired = System.currentTimeMillis() / 1000 > exp
                        addProperty("isExpired", isExpired)
                    }
                }

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun estimatePasswordStrength(password: String): Result<PasswordStrengthResultDto> =
        withContext(Dispatchers.Default) {
            try {
                try {
                    val resp = apiClient.getService().estimatePasswordStrength(PasswordStrengthRequest(password))
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        return@withContext Result.success(resp.body()!!.data!!)
                    }
                } catch (_: Exception) {}

                var pool = 0
                if (password.any { it.isLowerCase() }) pool += 26
                if (password.any { it.isUpperCase() }) pool += 26
                if (password.any { it.isDigit() }) pool += 10
                if (password.any { !it.isLetterOrDigit() }) pool += 33

                val len = password.length
                val entropy = if (pool > 0 && len > 0) len * (ln(pool.toDouble()) / ln(2.0)) else 0.0

                val (score, label, crackTime) = when {
                    entropy < 28 -> Triple(0, "Very Weak", "< 1 second")
                    entropy < 40 -> Triple(1, "Weak", "a few minutes")
                    entropy < 60 -> Triple(2, "Moderate", "several days")
                    entropy < 80 -> Triple(3, "Strong", "several months to years")
                    else -> Triple(4, "Very Strong", "centuries")
                }

                val feedback = mutableListOf<String>()
                if (len < 12) feedback.add("Increase length to at least 12-16 characters.")
                if (pool < 50) feedback.add("Mix uppercase, numbers, and symbols to maximize character variety.")

                Result.success(
                    PasswordStrengthResultDto(
                        score = score,
                        label = label,
                        entropyBits = round(entropy * 10) / 10,
                        crackTimeText = crackTime,
                        feedback = feedback
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun formatJson(json: String, indent: Int): Result<JsonFormatResultDto> =
        withContext(Dispatchers.Default) {
            try {
                try {
                    val resp = apiClient.getService().formatJson(JsonFormatRequest(json, indent))
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        return@withContext Result.success(resp.body()!!.data!!)
                    }
                } catch (_: Exception) {}

                val element = JsonParser.parseString(json)
                val formatted = gson.toJson(element)
                Result.success(JsonFormatResultDto(formatted = formatted, valid = true))
            } catch (e: Exception) {
                Result.success(JsonFormatResultDto(formatted = json, valid = false, error = e.message))
            }
        }
}

