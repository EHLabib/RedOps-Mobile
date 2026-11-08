package com.redops.mobile.presentation.toolkit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.redops.mobile.data.remote.model.*
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.ToolObservation
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.ToolObservationRepository
import com.redops.mobile.domain.repository.ToolkitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class ToolkitCategory(val title: String, val iconEmoji: String, val subtitle: String, val badge: String) {
    RECON("Passive Recon", "🌐", "DNS queries, PTR, WHOIS/RDAP & Subnet CIDR", "4 LIVE TOOLS"),
    WEBSEC("Web Security", "🛡️", "Headers analysis, TLS/SSL certs & Redirect hops", "3 LIVE TOOLS"),
    UTILITIES("Security Utilities", "🧰", "Multi-encoder, Hash digests, ID & CVSS v3.1", "4 LIVE TOOLS"),
    WORKSPACE("Workspace", "📋", "Engagements, Targets, Findings, Notebook & Reports", "UNIFIED SUITE")
}

sealed class ToolExecutionState<out T> {
    object Idle : ToolExecutionState<Nothing>()
    object Loading : ToolExecutionState<Nothing>()
    data class Success<T>(val data: T) : ToolExecutionState<T>()
    data class Error(val message: String) : ToolExecutionState<Nothing>()
}

class ToolkitViewModel(
    private val toolkitRepository: ToolkitRepository,
    private val observationRepository: ToolObservationRepository,
    private val engagementRepository: EngagementRepository
) : ViewModel() {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    val engagements: StateFlow<List<Engagement>> = engagementRepository.getAllEngagements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val observations: StateFlow<List<ToolObservation>> = observationRepository.getAllObservations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow(ToolkitCategory.RECON)
    val selectedCategory: StateFlow<ToolkitCategory> = _selectedCategory.asStateFlow()

    private val _selectedToolId = MutableStateFlow("dns")
    val selectedToolId: StateFlow<String> = _selectedToolId.asStateFlow()

    // Authorization consent checkboxes
    var authCheckDns = MutableStateFlow(true)
    var authCheckReverse = MutableStateFlow(true)
    var authCheckRdap = MutableStateFlow(true)
    var authCheckIp = MutableStateFlow(true)
    var authCheckHeaders = MutableStateFlow(true)
    var authCheckSsl = MutableStateFlow(true)
    var authCheckRedirects = MutableStateFlow(true)

    // Tool Execution States
    val dnsState = MutableStateFlow<ToolExecutionState<JsonObject>>(ToolExecutionState.Idle)
    val reverseDnsState = MutableStateFlow<ToolExecutionState<JsonObject>>(ToolExecutionState.Idle)
    val rdapState = MutableStateFlow<ToolExecutionState<JsonObject>>(ToolExecutionState.Idle)
    val ipInfoState = MutableStateFlow<ToolExecutionState<JsonObject>>(ToolExecutionState.Idle)
    val subnetState = MutableStateFlow<ToolExecutionState<SubnetResultDto>>(ToolExecutionState.Idle)

    val headersState = MutableStateFlow<ToolExecutionState<HttpHeadersResultDto>>(ToolExecutionState.Idle)
    val sslState = MutableStateFlow<ToolExecutionState<TlsCertResultDto>>(ToolExecutionState.Idle)
    val redirectsState = MutableStateFlow<ToolExecutionState<List<RedirectHopDto>>>(ToolExecutionState.Idle)
    val urlAnalyzeState = MutableStateFlow<ToolExecutionState<UrlAnalyzeResultDto>>(ToolExecutionState.Idle)

    val hashState = MutableStateFlow<ToolExecutionState<HashResultDto>>(ToolExecutionState.Idle)
    val identifyState = MutableStateFlow<ToolExecutionState<List<HashMatchDto>>>(ToolExecutionState.Idle)
    val jwtState = MutableStateFlow<ToolExecutionState<JsonObject>>(ToolExecutionState.Idle)
    val cvssState = MutableStateFlow<ToolExecutionState<CvssResultDto>>(ToolExecutionState.Idle)
    val encoderState = MutableStateFlow<ToolExecutionState<CryptoConvertResultDto>>(ToolExecutionState.Idle)
    val passwordState = MutableStateFlow<ToolExecutionState<PasswordStrengthResultDto>>(ToolExecutionState.Idle)
    val jsonState = MutableStateFlow<ToolExecutionState<JsonFormatResultDto>>(ToolExecutionState.Idle)

    // Toast / Feedback message
    private val _feedbackMessage = MutableSharedFlow<String>()
    val feedbackMessage: SharedFlow<String> = _feedbackMessage.asSharedFlow()

    fun selectCategory(category: ToolkitCategory) {
        _selectedCategory.value = category
        when (category) {
            ToolkitCategory.RECON -> _selectedToolId.value = "dns"
            ToolkitCategory.WEBSEC -> _selectedToolId.value = "headers"
            ToolkitCategory.UTILITIES -> _selectedToolId.value = "hash"
            ToolkitCategory.WORKSPACE -> _selectedToolId.value = "observations"
        }
    }

    fun selectTool(toolId: String) {
        _selectedToolId.value = toolId
    }

    // 1. DNS
    fun runDnsLookup(domain: String, type: String) {
        if (domain.isBlank()) {
            dnsState.value = ToolExecutionState.Error("Please enter a valid domain name.")
            return
        }
        if (!authCheckDns.value) {
            dnsState.value = ToolExecutionState.Error("Target authorization confirmation is required.")
            return
        }
        viewModelScope.launch {
            dnsState.value = ToolExecutionState.Loading
            val types = if (type == "ALL") null else listOf(type)
            val result = toolkitRepository.resolveDns(domain.trim(), types, authCheckDns.value)
            result.fold(
                onSuccess = { dnsState.value = ToolExecutionState.Success(it) },
                onFailure = { dnsState.value = ToolExecutionState.Error(it.message ?: "DNS lookup failed") }
            )
        }
    }

    // 2. Reverse DNS
    fun runReverseDns(ip: String) {
        if (ip.isBlank()) {
            reverseDnsState.value = ToolExecutionState.Error("Please enter an IP address.")
            return
        }
        if (!authCheckReverse.value) {
            reverseDnsState.value = ToolExecutionState.Error("Target authorization confirmation is required.")
            return
        }
        viewModelScope.launch {
            reverseDnsState.value = ToolExecutionState.Loading
            val result = toolkitRepository.reverseDns(ip.trim(), authCheckReverse.value)
            result.fold(
                onSuccess = { reverseDnsState.value = ToolExecutionState.Success(it) },
                onFailure = { reverseDnsState.value = ToolExecutionState.Error(it.message ?: "Reverse DNS failed") }
            )
        }
    }

    // 3. RDAP
    fun runRdapLookup(domain: String) {
        if (domain.isBlank()) {
            rdapState.value = ToolExecutionState.Error("Please enter a valid domain name.")
            return
        }
        if (!authCheckRdap.value) {
            rdapState.value = ToolExecutionState.Error("Target authorization confirmation is required.")
            return
        }
        viewModelScope.launch {
            rdapState.value = ToolExecutionState.Loading
            val result = toolkitRepository.rdapLookup(domain.trim(), authCheckRdap.value)
            result.fold(
                onSuccess = { rdapState.value = ToolExecutionState.Success(it) },
                onFailure = { rdapState.value = ToolExecutionState.Error(it.message ?: "RDAP lookup failed") }
            )
        }
    }

    // 4. IP Info
    fun runIpInfo(ip: String) {
        if (ip.isBlank()) {
            ipInfoState.value = ToolExecutionState.Error("Please enter an IP address.")
            return
        }
        if (!authCheckIp.value) {
            ipInfoState.value = ToolExecutionState.Error("Target authorization confirmation is required.")
            return
        }
        viewModelScope.launch {
            ipInfoState.value = ToolExecutionState.Loading
            val result = toolkitRepository.getIpInfo(ip.trim(), authCheckIp.value)
            result.fold(
                onSuccess = { ipInfoState.value = ToolExecutionState.Success(it) },
                onFailure = { ipInfoState.value = ToolExecutionState.Error(it.message ?: "IP lookup failed") }
            )
        }
    }

    // 5. Subnet
    fun runSubnetCalc(cidr: String) {
        if (cidr.isBlank()) {
            subnetState.value = ToolExecutionState.Error("Please enter a CIDR notation (e.g. 192.168.1.0/24).")
            return
        }
        viewModelScope.launch {
            subnetState.value = ToolExecutionState.Loading
            val result = toolkitRepository.calculateSubnet(cidr.trim())
            result.fold(
                onSuccess = { subnetState.value = ToolExecutionState.Success(it) },
                onFailure = { subnetState.value = ToolExecutionState.Error(it.message ?: "Subnet calculation failed") }
            )
        }
    }

    // 6. Security Headers
    fun runAnalyzeHeaders(url: String, followRedirects: Boolean) {
        if (url.isBlank()) {
            headersState.value = ToolExecutionState.Error("Please enter a target URL.")
            return
        }
        if (!authCheckHeaders.value) {
            headersState.value = ToolExecutionState.Error("Target authorization confirmation is required.")
            return
        }
        viewModelScope.launch {
            headersState.value = ToolExecutionState.Loading
            val result = toolkitRepository.analyzeHeaders(url.trim(), followRedirects, authCheckHeaders.value)
            result.fold(
                onSuccess = { headersState.value = ToolExecutionState.Success(it) },
                onFailure = { headersState.value = ToolExecutionState.Error(it.message ?: "Headers analysis failed") }
            )
        }
    }

    // 7. TLS Certificate
    fun runInspectTls(host: String, portStr: String) {
        if (host.isBlank()) {
            sslState.value = ToolExecutionState.Error("Please enter a hostname.")
            return
        }
        if (!authCheckSsl.value) {
            sslState.value = ToolExecutionState.Error("Target authorization confirmation is required.")
            return
        }
        val port = portStr.toIntOrNull() ?: 443
        viewModelScope.launch {
            sslState.value = ToolExecutionState.Loading
            val result = toolkitRepository.inspectTlsCert(host.trim(), port, authCheckSsl.value)
            result.fold(
                onSuccess = { sslState.value = ToolExecutionState.Success(it) },
                onFailure = { sslState.value = ToolExecutionState.Error(it.message ?: "TLS inspection failed") }
            )
        }
    }

    // 8. Redirects
    fun runTraceRedirects(url: String) {
        if (url.isBlank()) {
            redirectsState.value = ToolExecutionState.Error("Please enter a target URL.")
            return
        }
        if (!authCheckRedirects.value) {
            redirectsState.value = ToolExecutionState.Error("Target authorization confirmation is required.")
            return
        }
        viewModelScope.launch {
            redirectsState.value = ToolExecutionState.Loading
            val result = toolkitRepository.traceRedirects(url.trim(), authCheckRedirects.value)
            result.fold(
                onSuccess = { redirectsState.value = ToolExecutionState.Success(it) },
                onFailure = { redirectsState.value = ToolExecutionState.Error(it.message ?: "Redirect trace failed") }
            )
        }
    }

    // 9. URL Analyzer
    fun runAnalyzeUrl(url: String) {
        if (url.isBlank()) {
            urlAnalyzeState.value = ToolExecutionState.Error("Please enter a URL to parse.")
            return
        }
        viewModelScope.launch {
            urlAnalyzeState.value = ToolExecutionState.Loading
            val result = toolkitRepository.analyzeUrl(url.trim())
            result.fold(
                onSuccess = { urlAnalyzeState.value = ToolExecutionState.Success(it) },
                onFailure = { urlAnalyzeState.value = ToolExecutionState.Error(it.message ?: "URL analysis failed") }
            )
        }
    }

    // 10. Hash Generator
    fun runGenerateHash(text: String, algorithm: String, hmacKey: String) {
        if (text.isEmpty()) {
            hashState.value = ToolExecutionState.Error("Please enter text to hash.")
            return
        }
        viewModelScope.launch {
            hashState.value = ToolExecutionState.Loading
            val result = toolkitRepository.generateHash(text, algorithm, hmacKey.ifBlank { null })
            result.fold(
                onSuccess = { hashState.value = ToolExecutionState.Success(it) },
                onFailure = { hashState.value = ToolExecutionState.Error(it.message ?: "Hash generation failed") }
            )
        }
    }

    // 11. Hash Identifier
    fun runIdentifyHash(hash: String) {
        if (hash.isBlank()) {
            identifyState.value = ToolExecutionState.Error("Please enter a hash digest.")
            return
        }
        viewModelScope.launch {
            identifyState.value = ToolExecutionState.Loading
            val result = toolkitRepository.identifyHash(hash.trim())
            result.fold(
                onSuccess = { identifyState.value = ToolExecutionState.Success(it) },
                onFailure = { identifyState.value = ToolExecutionState.Error(it.message ?: "Hash identification failed") }
            )
        }
    }

    // 12. CVSS Calculator
    fun runCalculateCvss(metrics: CvssMetrics) {
        viewModelScope.launch {
            cvssState.value = ToolExecutionState.Loading
            val result = toolkitRepository.calculateCvss(metrics)
            result.fold(
                onSuccess = { cvssState.value = ToolExecutionState.Success(it) },
                onFailure = { cvssState.value = ToolExecutionState.Error(it.message ?: "CVSS calculation failed") }
            )
        }
    }

    // 13. JWT Decoder
    fun runDecodeJwt(token: String) {
        if (token.isBlank()) {
            jwtState.value = ToolExecutionState.Error("Please enter a JWT token string.")
            return
        }
        viewModelScope.launch {
            jwtState.value = ToolExecutionState.Loading
            val result = toolkitRepository.decodeJwt(token.trim())
            result.fold(
                onSuccess = { jwtState.value = ToolExecutionState.Success(it) },
                onFailure = { jwtState.value = ToolExecutionState.Error(it.message ?: "JWT decode failed") }
            )
        }
    }

    // 14. Encoder/Decoder
    fun runConvertEncoding(input: String, from: String, to: String) {
        if (input.isEmpty()) {
            encoderState.value = ToolExecutionState.Error("Please enter input text to convert.")
            return
        }
        viewModelScope.launch {
            encoderState.value = ToolExecutionState.Loading
            val result = toolkitRepository.convertEncoding(input, from, to)
            result.fold(
                onSuccess = { encoderState.value = ToolExecutionState.Success(it) },
                onFailure = { encoderState.value = ToolExecutionState.Error(it.message ?: "Encoding conversion failed") }
            )
        }
    }

    // 15. Password Strength
    fun runEstimatePassword(password: String) {
        if (password.isEmpty()) {
            passwordState.value = ToolExecutionState.Error("Please enter a password to evaluate.")
            return
        }
        viewModelScope.launch {
            passwordState.value = ToolExecutionState.Loading
            val result = toolkitRepository.estimatePasswordStrength(password)
            result.fold(
                onSuccess = { passwordState.value = ToolExecutionState.Success(it) },
                onFailure = { passwordState.value = ToolExecutionState.Error(it.message ?: "Password evaluation failed") }
            )
        }
    }

    // 16. JSON Formatter
    fun runFormatJson(json: String, indent: Int) {
        if (json.isBlank()) {
            jsonState.value = ToolExecutionState.Error("Please enter a JSON string to format.")
            return
        }
        viewModelScope.launch {
            jsonState.value = ToolExecutionState.Loading
            val result = toolkitRepository.formatJson(json.trim(), indent)
            result.fold(
                onSuccess = { jsonState.value = ToolExecutionState.Success(it) },
                onFailure = { jsonState.value = ToolExecutionState.Error(it.message ?: "JSON formatting failed") }
            )
        }
    }

    // Save observation
    fun saveObservation(
        engagementId: String,
        toolName: String,
        toolCategory: String,
        target: String,
        title: String,
        summary: String,
        rawJson: String
    ) {
        if (engagementId.isBlank()) {
            viewModelScope.launch { _feedbackMessage.emit("Please select an engagement to link observation.") }
            return
        }
        viewModelScope.launch {
            val observation = ToolObservation(
                id = UUID.randomUUID().toString(),
                engagementId = engagementId,
                toolName = toolName,
                toolCategory = toolCategory,
                target = target,
                title = title.ifBlank { "$toolName Result for $target" },
                summary = summary,
                sourceInfo = "RedOps Toolkit ($toolName)",
                rawResultJson = rawJson,
                status = "Unverified"
            )
            observationRepository.saveObservation(observation)
            _feedbackMessage.emit("Observation saved successfully to assessment!")
        }
    }

    fun deleteObservation(id: String) {
        viewModelScope.launch {
            observationRepository.deleteObservation(id)
            _feedbackMessage.emit("Observation deleted.")
        }
    }

    fun updateObservationStatus(observation: ToolObservation, newStatus: String) {
        viewModelScope.launch {
            observationRepository.updateObservation(observation.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
            _feedbackMessage.emit("Observation marked as $newStatus.")
        }
    }
}

