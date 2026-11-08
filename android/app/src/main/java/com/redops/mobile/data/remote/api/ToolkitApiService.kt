package com.redops.mobile.data.remote.api

import com.google.gson.JsonObject
import com.redops.mobile.data.remote.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ToolkitApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponseData>>

    // Reconnaissance
    @POST("tools/dns/resolve")
    suspend fun resolveDns(
        @Body request: DnsResolveRequest
    ): Response<ApiResponse<JsonObject>>

    @POST("tools/dns/reverse")
    suspend fun reverseDns(
        @Body request: ReverseDnsRequest
    ): Response<ApiResponse<JsonObject>>

    @POST("tools/dns/rdap")
    suspend fun rdapLookup(
        @Body request: RdapRequest
    ): Response<ApiResponse<JsonObject>>

    @POST("tools/dns/ip-info")
    suspend fun getIpInfo(
        @Body request: IpInfoRequest
    ): Response<ApiResponse<JsonObject>>

    @POST("tools/network/subnet")
    suspend fun calculateSubnet(
        @Body request: SubnetRequest
    ): Response<ApiResponse<SubnetResultDto>>

    // Web Security
    @POST("tools/web/headers")
    suspend fun analyzeHeaders(
        @Body request: HttpHeadersRequest
    ): Response<ApiResponse<HttpHeadersResultDto>>

    @POST("tools/web/ssl")
    suspend fun inspectTlsCert(
        @Body request: TlsCertRequest
    ): Response<ApiResponse<TlsCertResultDto>>

    @POST("tools/web/redirects")
    suspend fun traceRedirects(
        @Body request: RedirectTraceRequest
    ): Response<ApiResponse<List<RedirectHopDto>>>

    @POST("tools/web/url-analyze")
    suspend fun analyzeUrl(
        @Body request: UrlAnalyzeRequest
    ): Response<ApiResponse<UrlAnalyzeResultDto>>

    // Security Utilities
    @POST("tools/crypto/convert")
    suspend fun convertEncoding(
        @Body request: CryptoConvertRequest
    ): Response<ApiResponse<CryptoConvertResultDto>>

    @POST("tools/crypto/hash")
    suspend fun generateHash(
        @Body request: HashGenerateRequest
    ): Response<ApiResponse<HashResultDto>>

    @POST("tools/crypto/identify")
    suspend fun identifyHash(
        @Body request: HashIdentifyRequest
    ): Response<ApiResponse<List<HashMatchDto>>>

    @POST("tools/cvss/calculate")
    suspend fun calculateCvss(
        @Body request: CvssCalculateRequest
    ): Response<ApiResponse<CvssResultDto>>

    @POST("tools/crypto/jwt-decode")
    suspend fun decodeJwt(
        @Body request: JwtDecodeRequest
    ): Response<ApiResponse<JsonObject>>

    @POST("tools/crypto/password-strength")
    suspend fun estimatePasswordStrength(
        @Body request: PasswordStrengthRequest
    ): Response<ApiResponse<PasswordStrengthResultDto>>

    @POST("tools/format/json")
    suspend fun formatJson(
        @Body request: JsonFormatRequest
    ): Response<ApiResponse<JsonFormatResultDto>>
}

