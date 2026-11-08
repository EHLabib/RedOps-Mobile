package com.redops.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

// Generic API Wrapper
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("details") val details: String? = null
)

// Auth Login
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponseData(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto? = null
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("role") val role: String
)

// 1. DNS Resolve
data class DnsResolveRequest(
    @SerializedName("domain") val domain: String,
    @SerializedName("types") val types: List<String>? = null,
    @SerializedName("authorized") val authorized: Boolean = true
)

data class DnsRecordDto(
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String,
    @SerializedName("ttl") val ttl: Int? = null,
    @SerializedName("priority") val priority: Int? = null
)

// 2. Reverse DNS
data class ReverseDnsRequest(
    @SerializedName("ip") val ip: String,
    @SerializedName("authorized") val authorized: Boolean = true
)

// 3. RDAP WHOIS
data class RdapRequest(
    @SerializedName("domain") val domain: String,
    @SerializedName("authorized") val authorized: Boolean = true
)

// 4. IP Info
data class IpInfoRequest(
    @SerializedName("ip") val ip: String,
    @SerializedName("authorized") val authorized: Boolean = true
)

// 5. Subnet Calc
data class SubnetRequest(
    @SerializedName("cidr") val cidr: String
)

data class SubnetResultDto(
    @SerializedName("cidr") val cidr: String,
    @SerializedName("ip") val ip: String,
    @SerializedName("prefixLength") val prefixLength: Int,
    @SerializedName("netmask") val netmask: String,
    @SerializedName("wildcard") val wildcard: String,
    @SerializedName("networkAddress") val networkAddress: String,
    @SerializedName("broadcastAddress") val broadcastAddress: String,
    @SerializedName("firstHost") val firstHost: String,
    @SerializedName("lastHost") val lastHost: String,
    @SerializedName("totalHosts") val totalHosts: Long,
    @SerializedName("usableHosts") val usableHosts: Long,
    @SerializedName("ipClass") val ipClass: String,
    @SerializedName("isPrivate") val isPrivate: Boolean
)

// 6. HTTP Headers
data class HttpHeadersRequest(
    @SerializedName("url") val url: String,
    @SerializedName("followRedirects") val followRedirects: Boolean = true,
    @SerializedName("authorized") val authorized: Boolean = true
)

data class HeaderAnalysisItem(
    @SerializedName("header") val header: String,
    @SerializedName("present") val present: Boolean,
    @SerializedName("value") val value: String? = null,
    @SerializedName("status") val status: String, // PASS, WARN, FAIL
    @SerializedName("recommendation") val recommendation: String? = null
)

data class HttpHeadersResultDto(
    @SerializedName("url") val url: String,
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("statusMessage") val statusMessage: String? = null,
    @SerializedName("rawHeaders") val rawHeaders: Map<String, String>? = null,
    @SerializedName("securityHeaders") val securityHeaders: List<HeaderAnalysisItem>? = null,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("score") val score: Int? = null
)

// 7. TLS Certificate
data class TlsCertRequest(
    @SerializedName("host") val host: String,
    @SerializedName("port") val port: Int = 443,
    @SerializedName("authorized") val authorized: Boolean = true
)

data class TlsCertResultDto(
    @SerializedName("host") val host: String,
    @SerializedName("port") val port: Int,
    @SerializedName("subject") val subject: Map<String, String>? = null,
    @SerializedName("issuer") val issuer: Map<String, String>? = null,
    @SerializedName("validFrom") val validFrom: String? = null,
    @SerializedName("validTo") val validTo: String? = null,
    @SerializedName("daysRemaining") val daysRemaining: Int? = null,
    @SerializedName("protocol") val protocol: String? = null,
    @SerializedName("cipher") val cipher: String? = null,
    @SerializedName("sans") val sans: List<String>? = null,
    @SerializedName("isValid") val isValid: Boolean = true,
    @SerializedName("validationErrors") val validationErrors: List<String>? = null
)

// 8. Redirects
data class RedirectTraceRequest(
    @SerializedName("url") val url: String,
    @SerializedName("authorized") val authorized: Boolean = true
)

data class RedirectHopDto(
    @SerializedName("hop") val hop: Int,
    @SerializedName("url") val url: String,
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("location") val location: String? = null
)

// 9. Crypto Convert
data class CryptoConvertRequest(
    @SerializedName("input") val input: String,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String
)

data class CryptoConvertResultDto(
    @SerializedName("input") val input: String,
    @SerializedName("output") val output: String,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String
)

// 10. Hash Generator
data class HashGenerateRequest(
    @SerializedName("text") val text: String,
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("hmacKey") val hmacKey: String? = null
)

data class HashResultDto(
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("hash") val hash: String,
    @SerializedName("isHmac") val isHmac: Boolean = false
)

// 11. Hash Identifier
data class HashIdentifyRequest(
    @SerializedName("hash") val hash: String
)

data class HashMatchDto(
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("confidence") val confidence: String,
    @SerializedName("description") val description: String? = null
)

// 12. CVSS Calculator
data class CvssMetrics(
    @SerializedName("AV") val av: String,
    @SerializedName("AC") val ac: String,
    @SerializedName("PR") val pr: String,
    @SerializedName("UI") val ui: String,
    @SerializedName("S") val s: String,
    @SerializedName("C") val c: String,
    @SerializedName("I") val i: String,
    @SerializedName("A") val a: String
)

data class CvssCalculateRequest(
    @SerializedName("metrics") val metrics: CvssMetrics
)

data class CvssResultDto(
    @SerializedName("baseScore") val baseScore: Double,
    @SerializedName("severity") val severity: String,
    @SerializedName("vector") val vector: String
)

// 13. URL Analyzer
data class UrlAnalyzeRequest(
    @SerializedName("url") val url: String
)

data class UrlAnalyzeResultDto(
    @SerializedName("url") val url: String,
    @SerializedName("protocol") val protocol: String? = null,
    @SerializedName("hostname") val hostname: String? = null,
    @SerializedName("port") val port: String? = null,
    @SerializedName("pathname") val pathname: String? = null,
    @SerializedName("search") val search: String? = null,
    @SerializedName("hash") val hash: String? = null,
    @SerializedName("params") val params: Map<String, String>? = null,
    @SerializedName("securityNotes") val securityNotes: List<String>? = null
)

// 14. JWT Decoder
data class JwtDecodeRequest(
    @SerializedName("token") val token: String
)

data class JwtDecodeResultDto(
    @SerializedName("header") val header: Map<String, Any>? = null,
    @SerializedName("payload") val payload: Map<String, Any>? = null,
    @SerializedName("signature") val signature: String? = null,
    @SerializedName("isExpired") val isExpired: Boolean? = null,
    @SerializedName("expirationDate") val expirationDate: String? = null,
    @SerializedName("issuedAt") val issuedAt: String? = null
)

// 15. Password Strength
data class PasswordStrengthRequest(
    @SerializedName("password") val password: String
)

data class PasswordStrengthResultDto(
    @SerializedName("score") val score: Int, // 0 - 4
    @SerializedName("label") val label: String,
    @SerializedName("entropyBits") val entropyBits: Double,
    @SerializedName("crackTimeText") val crackTimeText: String,
    @SerializedName("feedback") val feedback: List<String>? = null
)

// 16. JSON Formatter
data class JsonFormatRequest(
    @SerializedName("json") val json: String,
    @SerializedName("indent") val indent: Int = 2
)

data class JsonFormatResultDto(
    @SerializedName("formatted") val formatted: String,
    @SerializedName("valid") val valid: Boolean = true,
    @SerializedName("error") val error: String? = null
)

