package com.cordacodeclub.directAgreement

import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.finance.USD
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
    private val intermediaryAgreement = LegalAgreementState(testIntermediary.party, testPartyA.party, testPartyB.party,
            LegalAgreementState.Status.INTERMEDIATE, Amount(10, Currency.getInstance("GBP")))
    private val directAgreement = LegalAgreementState(testIntermediary.party, testPartyA.party, testPartyB.party,
            LegalAgreementState.Status.DIRECT, Amount(10, Currency.getInstance("GBP")))
    private val completeAgreement = LegalAgreementState(testIntermediary.party, testPartyA.party, testPartyB.party,
            LegalAgreementState.Status.COMPLETED, Amount(10, Currency.getInstance("GBP")))

    @Test
    fun `Create transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(ID, intermediaryAgreement)
                fails()
                command(listOf(testIntermediary.publicKey, testPartyA.publicKey), DirectAgreementContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Create transaction must have no input`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, intermediaryAgreement)
                command(listOf(testIntermediary.publicKey, testPartyA.publicKey), DirectAgreementContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Create transaction must be signed by Intermediary and PartyA only`() {
        ledgerServices.ledger {
            transaction {
                output(ID, intermediaryAgreement)
                command(listOf(testIntermediary.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.Create())
                fails()
                output(ID, intermediaryAgreement)
                command(listOf(testIntermediary.publicKey, testPartyA.publicKey), DirectAgreementContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Create transaction must have INTERMEDIATE status`() {
        ledgerServices.ledger {
            transaction {
                output(ID, directAgreement)
                command(listOf(testIntermediary.publicKey, testPartyA.publicKey), DirectAgreementContract.Commands.Create())
                fails()
                output(ID, completeAgreement)
                command(listOf(testIntermediary.publicKey, testPartyA.publicKey), DirectAgreementContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Direct transaction should have one input`() {
        ledgerServices.ledger {
            transaction {
                output(ID, intermediaryAgreement)
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.GoToDirect())
                fails()

            }
        }
    }

    @Test
    fun `Direct transaction can only be done via GoToDirect command`() {
        ledgerServices.ledger {
            transaction {
                output(ID, directAgreement)
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Direct transaction input should be an intermediary agreement type `() {
        ledgerServices.ledger {
            transaction {
                input(ID,intermediaryAgreement)
                output(ID, directAgreement)
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.GoToDirect())
                verifies()

            }
        }
    }

    @Test
    fun `Direct transaction input must have INTERMEDIATE status`() {
        ledgerServices.ledger {
            transaction {
                output(ID, intermediaryAgreement)
                command(listOf(testIntermediary.publicKey, testPartyA.publicKey), DirectAgreementContract.Commands.GoToDirect())
                fails()
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.GoToDirect())
                fails()
            }
        }
    }

    @Test
    fun `Direct transaction must be signed by PartyA and PartyB only`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement)
                command(listOf(testIntermediary.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.Create())
                fails()
                input(ID, intermediaryAgreement)
                output(ID, directAgreement)
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun `Direct transaction input and output values must be equal`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement.copy(value = Amount(11, USD)))
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.GoToDirect())
                fails()
            }
        }
    }

    @Test
    fun `Direct transaction input and output parties must be the same entities`() {
        val testForeignParty = TestIdentity(CordaX500Name("TestForeign", "London", "US"))
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement.copy(partyB = testForeignParty.party))
                command(listOf(testPartyA.publicKey, testForeignParty.publicKey), DirectAgreementContract.Commands.GoToDirect())
                fails()
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement.copy(partyA = testForeignParty.party))
                command(listOf(testForeignParty.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.GoToDirect())
                fails()
            }
        }
    }

    @Test
    fun `Complete transaction must always have one input `() {
        ledgerServices.ledger {
            transaction {
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()

            }
        }
    }

    @Test
    fun `Complete transaction input must of type intermediary or direct`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                verifies()
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.Finalise())
                verifies()
            }
        }
    }

    @Test
    fun `Complete transaction output must have Completed status`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement.copy(status = LegalAgreementState.Status.INTERMEDIATE))
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement.copy(status = LegalAgreementState.Status.DIRECT))
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
        }
    }

    @Test
    fun `Complete transaction can only be done via Finalise command`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.GoToDirect())
                fails()
            }
            transaction {
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Create())
                fails()
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                verifies()
            }
        }
    }

    @Test
    fun `Complete transaction input and output values must be equal`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement.copy(value = Amount(11, USD)))
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
        }
    }

    @Test
    fun `Complete transaction must have 2 signers`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testPartyB.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
        }
    }

    @Test
    fun `Complete transaction must be signed by PartyA and Intermediate only if the input was of status Intermediate`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                verifies()
            }
        }
    }

    @Test
    fun `Complete transaction must be signed by PartyA and PartyB only if the input was of status Direct`() {
        ledgerServices.ledger {
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement)
                command(listOf(testPartyA.publicKey, testPartyB.publicKey), DirectAgreementContract.Commands.Finalise())
                verifies()
            }
        }
    }

    @Test
    fun `Complete transaction input and output parties must be the same entities`() {
        val testForeignParty = TestIdentity(CordaX500Name("TestForeign", "London", "US"))
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement.copy(intermediary = testForeignParty.party))
                command(listOf(testPartyA.publicKey, testForeignParty.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement.copy(partyA = testForeignParty.party))
                command(listOf(testForeignParty.publicKey, testIntermediary.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement.copy(partyB = testForeignParty.party))
                command(listOf(testPartyA.publicKey, testForeignParty.publicKey), DirectAgreementContract.Commands.Finalise())
                fails()
            }
        }
    }

}