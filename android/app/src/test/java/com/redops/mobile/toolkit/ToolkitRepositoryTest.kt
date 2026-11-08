package com.redops.mobile.toolkit

import com.redops.mobile.data.remote.ApiClient
import com.redops.mobile.data.remote.model.CvssMetrics
import com.redops.mobile.data.repository.ToolkitRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ToolkitRepositoryTest {

    private lateinit var repository: ToolkitRepositoryImpl

    @Before
    fun setup() {
        val dummyApiClient = ApiClient(
            baseUrlProvider = { "http://10.0.2.2:5000/api/v1/" },
            tokenProvider = { "" }
        )
        repository = ToolkitRepositoryImpl(dummyApiClient)
    }

    @Test
    fun testSubnetCalculation() = runBlocking {
        val res = repository.calculateSubnet("192.168.1.0/24")
        assertTrue(res.isSuccess)
        val data = res.getOrThrow()
        assertEquals("192.168.1.0", data.networkAddress)
        assertEquals("192.168.1.255", data.broadcastAddress)
        assertEquals("255.255.255.0", data.netmask)
        assertEquals("0.0.0.255", data.wildcard)
        assertEquals("192.168.1.1", data.firstHost)
        assertEquals("192.168.1.254", data.lastHost)
        assertEquals(256L, data.totalHosts)
        assertEquals(254L, data.usableHosts)
        assertEquals("Class C", data.ipClass)
        assertTrue(data.isPrivate)
    }

    @Test
    fun testSubnetCalculationSlash30() = runBlocking {
        val res = repository.calculateSubnet("10.0.0.0/30")
        assertTrue(res.isSuccess)
        val data = res.getOrThrow()
        assertEquals(4L, data.totalHosts)
        assertEquals(2L, data.usableHosts)
        assertEquals("10.0.0.1", data.firstHost)
        assertEquals("10.0.0.2", data.lastHost)
        assertTrue(data.isPrivate)
    }

    @Test
    fun testHashGeneration() = runBlocking {
        val resSha256 = repository.generateHash("admin", "SHA-256", null)
        assertTrue(resSha256.isSuccess)
        assertEquals("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", resSha256.getOrThrow().hash)

        val resMd5 = repository.generateHash("admin", "MD5", null)
        assertTrue(resMd5.isSuccess)
        assertEquals("21232f297a57a5a743894a0e4a801fc3", resMd5.getOrThrow().hash)
    }

    @Test
    fun testHashIdentification() = runBlocking {
        val md5Id = repository.identifyHash("21232f297a57a5a743894a0e4a801fc3")
        assertTrue(md5Id.isSuccess)
        assertTrue(md5Id.getOrThrow().any { it.algorithm == "MD5" })

        val sha256Id = repository.identifyHash("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918")
        assertTrue(sha256Id.isSuccess)
        assertTrue(sha256Id.getOrThrow().any { it.algorithm == "SHA-256" })

        val bcryptId = repository.identifyHash("\$2a\$12\$e8/xV2e.R3u0R")
        assertTrue(bcryptId.isSuccess)
        assertTrue(bcryptId.getOrThrow().any { it.algorithm == "bcrypt" })
    }

    @Test
    fun testCvssV31Calculation() = runBlocking {
        // Critical RCE: AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H -> 9.8
        val criticalMetrics = CvssMetrics(av = "N", ac = "L", pr = "N", ui = "N", s = "U", c = "H", i = "H", a = "H")
        val res = repository.calculateCvss(criticalMetrics)
        assertTrue(res.isSuccess)
        val data = res.getOrThrow()
        assertEquals(9.8, data.baseScore, 0.05)
        assertEquals("Critical", data.severity)
        assertTrue(data.vector.contains("CVSS:3.1"))
    }

    @Test
    fun testEncodingConversion() = runBlocking {
        val toBase64 = repository.convertEncoding("RedOps", "Plain", "Base64")
        assertTrue(toBase64.isSuccess)
        assertEquals("UmVkT3Bz", toBase64.getOrThrow().output)

        val toHex = repository.convertEncoding("RedOps", "Plain", "Hex")
        assertTrue(toHex.isSuccess)
        assertEquals("5265644f7073", toHex.getOrThrow().output.lowercase())

        val backToPlain = repository.convertEncoding("5265644f7073", "Hex", "Plain")
        assertTrue(backToPlain.isSuccess)
        assertEquals("RedOps", backToPlain.getOrThrow().output)
    }

    @Test
    fun testPasswordStrength() = runBlocking {
        val weak = repository.estimatePasswordStrength("123456")
        assertTrue(weak.isSuccess)
        assertEquals("Very Weak", weak.getOrThrow().label)

        val strong = repository.estimatePasswordStrength("P@ssw0rd_Super_Secure_2026!#%")
        assertTrue(strong.isSuccess)
        assertTrue(strong.getOrThrow().entropyBits > 60.0)
    }

    @Test
    fun testUrlAnalyzer() = runBlocking {
        val res = repository.analyzeUrl("https://auth.acme.corp:8443/login?token=secret123#view")
        assertTrue(res.isSuccess)
        val data = res.getOrThrow()
        assertEquals("https", data.protocol)
        assertEquals("auth.acme.corp", data.hostname)
        assertEquals("8443", data.port)
        assertEquals("/login", data.pathname)
        assertEquals("secret123", data.params?.get("token"))
        assertTrue(data.securityNotes?.any { it.contains("credentials") } == true)
    }

    @Test
    fun testJsonFormatter() = runBlocking {
        val raw = "{\"key\":\"val\",\"num\":123}"
        val res = repository.formatJson(raw, 2)
        assertTrue(res.isSuccess)
        assertTrue(res.getOrThrow().valid)
        assertTrue(res.getOrThrow().formatted.contains("\n"))
    }
}

