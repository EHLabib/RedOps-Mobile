package com.redops.mobile.domain.repository

import com.google.gson.JsonObject
import com.redops.mobile.data.remote.model.*

interface ToolkitRepository {
    suspend fun resolveDns(domain: String, types: List<String>?, authorized: Boolean): Result<JsonObject>
    suspend fun reverseDns(ip: String, authorized: Boolean): Result<JsonObject>
    suspend fun rdapLookup(domain: String, authorized: Boolean): Result<JsonObject>
    suspend fun getIpInfo(ip: String, authorized: Boolean): Result<JsonObject>
    suspend fun calculateSubnet(cidr: String): Result<SubnetResultDto>
    suspend fun analyzeHeaders(url: String, followRedirects: Boolean, authorized: Boolean): Result<HttpHeadersResultDto>
    suspend fun inspectTlsCert(host: String, port: Int, authorized: Boolean): Result<TlsCertResultDto>
    suspend fun traceRedirects(url: String, authorized: Boolean): Result<List<RedirectHopDto>>
    suspend fun analyzeUrl(url: String): Result<UrlAnalyzeResultDto>
    suspend fun convertEncoding(input: String, from: String, to: String): Result<CryptoConvertResultDto>
    suspend fun generateHash(text: String, algorithm: String, hmacKey: String?): Result<HashResultDto>
    suspend fun identifyHash(hash: String): Result<List<HashMatchDto>>
    suspend fun calculateCvss(metrics: CvssMetrics): Result<CvssResultDto>
    suspend fun decodeJwt(token: String): Result<JsonObject>
    suspend fun estimatePasswordStrength(password: String): Result<PasswordStrengthResultDto>
    suspend fun formatJson(json: String, indent: Int): Result<JsonFormatResultDto>
}

