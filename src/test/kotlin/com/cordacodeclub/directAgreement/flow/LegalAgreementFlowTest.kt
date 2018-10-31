package com.cordacodeclub.directAgreement.flow

import com.cordacodeclub.directAgreement.flow.mock.BustPartySignHandlerMock
import com.cordacodeclub.directAgreement.flow.mock.LegalAgreementFlowResponderMock
import com.cordacodeclub.directAgreement.flow.mock.QueryBustPartyHandlerMock
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertFailsWith

class LegalAgreementFlowTest {
    private lateinit var network: MockNetwork
    private lateinit var testContractor: StartedMockNode
    private lateinit var testLender: StartedMockNode
    private lateinit var testSPV: StartedMockNode
    private val testIntermediary = TestIdentity(CordaX500Name("TestIntermediary", "London", "GB"))
    private val testPartyA = TestIdentity(CordaX500Name("TestPartyA", "London", "GB"))
    private val testPartyB = TestIdentity(CordaX500Name("TestPartyB", "London", "GB"))
    private val testOracle = TestIdentity(CordaX500Name("TestOracle", "London", "GB"))

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.cordacodeclub.directAgreement.contract"))
        testContractor = network.createPartyNode()
        testLender = network.createPartyNode()
        testSPV = network.createPartyNode()

        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(testContractor, testSPV, testLender).forEach {
            it.registerInitiatedFlow(LegalAgreementFlowResponderMock::class.java)
            it.registerInitiatedFlow(QueryBustPartyHandlerMock::class.java)
            it.registerInitiatedFlow(BustPartySignHandlerMock::class.java)
        }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flow rejects invalid Agreements`() {
        val flow = LegalAgreementFlow.LegalAgreementFlowInitiator(
                Amount(10, Currency.getInstance("GBP")),
                testPartyA.party, testIntermediary.party, testOracle.party)
        val future = testSPV.startFlow(flow)
        network.runNetwork()

        // The IOUContract specifies that IOUs cannot have negative values.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }
}