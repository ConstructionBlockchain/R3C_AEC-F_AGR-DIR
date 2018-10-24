package com.cordacodeclub.directAgreement

import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class DirectAgreementContractTest {
    private val ledgerServices = MockServices()
    private val testIntermediary = TestIdentity(CordaX500Name("TestIntermediary", "London", "GB"))
    private val testPartyA = TestIdentity(CordaX500Name("TestPartyA", "London", "GB"))
    private val testPartyB = TestIdentity(CordaX500Name("TestPartyB", "London", "GB"))

    @Test
    fun `transaction must include CreateSpv command`() {
        val directAgreement = 1
        ledgerServices.ledger {
            transaction {
                output(ID, LegalAgreementState(testIntermediary.party, testPartyA.party, testPartyB.party,
                        LegalAgreementState.Status.INTERMEDIATE, Amount(10, Currency.getInstance("GBP"))))
                fails()
                command(listOf(testIntermediary.publicKey, testPartyA.publicKey), DirectAgreementContract.Commands.Create())
                verifies()
            }
        }
    }
}