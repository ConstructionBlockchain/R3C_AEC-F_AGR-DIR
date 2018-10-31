package com.cordacodeclub.directAgreement

import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.finance.USD
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Ignore
import org.junit.Test
import java.util.*


class DirectAgreementContractTest {
    private val ledgerServices = MockServices()
    private val testIntermediary = TestIdentity(CordaX500Name("TestIntermediary", "London", "GB"))
    private val testPartyA = TestIdentity(CordaX500Name("TestPartyA", "London", "GB"))
    private val testPartyB = TestIdentity(CordaX500Name("TestPartyB", "London", "GB"))
    private val testOracle = TestIdentity(CordaX500Name("TestOracle", "London", "GB"))
    private val intermediaryAgreement = LegalAgreementState(
            intermediary = testIntermediary.party, partyA = testPartyA.party,
            partyB = testPartyB.party, oracle = testOracle.party,
            status = LegalAgreementState.Status.INTERMEDIATE,
            value = Amount(10, Currency.getInstance("GBP")))
    private val directAgreement = intermediaryAgreement.copy(status = LegalAgreementState.Status.DIRECT)
    private val completeAgreement = intermediaryAgreement.copy(status = LegalAgreementState.Status.COMPLETED)

    @Test @Ignore("This is testing the Corda framework here, not our code")
    fun `Create transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(ID, intermediaryAgreement)
                failsWith("must contain at least one command")
                command(
                        listOf(testIntermediary.publicKey, testPartyA.publicKey),
                        DirectAgreementContract.Commands.Create())
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
                command(
                        listOf(testIntermediary.publicKey, testPartyA.publicKey),
                        DirectAgreementContract.Commands.Create())
                failsWith("should be no input of type LegalAgreementState when creating via Intermediary")
            }
        }
    }

    @Test
    fun `Create transaction must be signed by Intermediary and PartyA only`() {
        ledgerServices.ledger {
            transaction {
                output(ID, intermediaryAgreement)
                command(
                        listOf(testIntermediary.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.Create())
                failsWith("Intermediary and partyA must be signers")
            }
            transaction {
                output(ID, intermediaryAgreement)
                command(
                        listOf(testIntermediary.publicKey, testPartyA.publicKey),
                        DirectAgreementContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `Create transaction output must have INTERMEDIATE status`() {
        ledgerServices.ledger {
            transaction {
                output(ID, directAgreement)
                command(
                        listOf(testIntermediary.publicKey, testPartyA.publicKey),
                        DirectAgreementContract.Commands.Create())
                failsWith("output should have status INTERMEDIATE")
            }
            transaction {
                output(ID, completeAgreement)
                command(
                        listOf(testIntermediary.publicKey, testPartyA.publicKey),
                        DirectAgreementContract.Commands.Create())
                failsWith("output should have status INTERMEDIATE")
            }
        }
    }

    @Test
    fun `Direct transaction should have one input`() {
        ledgerServices.ledger {
            transaction {
                output(ID, intermediaryAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("One LegalAgreementState should be consumed when creating Direct")
            }
        }
    }

    @Test @Ignore("This is the same test as Create transaction output must have INTERMEDIATE status")
    fun `Direct transaction can only be done via GoToDirect command`() {
        ledgerServices.ledger {
            transaction {
                output(ID, directAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.Create())
                failsWith("output should have status INTERMEDIATE")
            }
        }
    }

    @Test
    fun `Direct transaction input should be an intermediary agreement type`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                verifies()
            }
        }
    }

    @Test
    fun `Direct transaction input must have INTERMEDIATE status`() {
        ledgerServices.ledger {
            transaction {
                input(ID, directAgreement)
                output(ID, directAgreement)
                command(
                        listOf(testIntermediary.publicKey, testPartyA.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("input should have status INTERMEDIATE")
            }
            transaction {
                input(ID, completeAgreement)
                output(ID, directAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("input should have status INTERMEDIATE")
            }
        }
    }

    @Test
    fun `Direct transaction must be signed by PartyA, PartyB and oracle only`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement)
                command(
                        listOf(testIntermediary.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("must be only 3 signers")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testIntermediary.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("must be only 3 signers")
            }
        }
    }

    @Test
    fun `Direct transaction input and output values must be equal`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement.copy(value = Amount(11, USD)))
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("output value should be equal to the input value")
            }
        }
    }

    @Test
    fun `Direct transaction input and output parties must be the same entities`() {
        val testForeignParty = TestIdentity(CordaX500Name("TestForeign", "London", "US"))
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement.copy(intermediary = testForeignParty.party))
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("intermediary should be the same entity on both states")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement.copy(partyA = testForeignParty.party))
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("partyA should be the same entity on both states")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement.copy(partyB = testForeignParty.party))
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("partyB should be the same entity on both states")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, directAgreement.copy(oracle = testForeignParty.party))
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("oracle should be the same entity on both states")
            }
        }
    }

    @Test
    fun `Complete transaction must always have one input`() {
        ledgerServices.ledger {
            transaction {
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("LegalAgreementState should be consumed when creating Direct")
            }
        }
    }

    @Test
    fun `Complete transaction input must be of type intermediary or direct`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                verifies()
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.Finalise())
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
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("output should have status COMPLETED")
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement.copy(status = LegalAgreementState.Status.DIRECT))
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("output should have status COMPLETED")
            }
        }
    }

    @Test @Ignore("Same as previous tests")
    fun `Complete transaction can only be done via Finalise command`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.GoToDirect(testIntermediary.party, true))
                failsWith("output should have status DIRECT")
            }
            transaction {
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Create())
                failsWith("output should have status INTERMEDIATE")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
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
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("output value should be equal to the input value")
            }
        }
    }

    @Test
    fun `Complete transaction must have 2 signers`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("must be only 2 signers")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("must be only 2 signers")
            }
        }
    }

    @Test
    fun `Complete transaction must be signed by PartyA and Intermediate only if the input was of status Intermediate`() {
        ledgerServices.ledger {
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("PartyA and Intermediary must be the signers")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("PartyA and Intermediary must be the signers")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
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
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("PartyA and PartyB must be the signers")
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testOracle.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("PartyA and PartyB must be the signers")
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement)
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.Finalise())
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
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("intermediary should be the same entity on both states")
            }
            transaction {
                input(ID, intermediaryAgreement)
                output(ID, completeAgreement.copy(partyA = testForeignParty.party))
                command(
                        listOf(testPartyA.publicKey, testIntermediary.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("partyA should be the same entity on both states")
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement.copy(partyB = testForeignParty.party))
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("partyB should be the same entity on both states")
            }
            transaction {
                input(ID, directAgreement)
                output(ID, completeAgreement.copy(oracle = testForeignParty.party))
                command(
                        listOf(testPartyA.publicKey, testPartyB.publicKey),
                        DirectAgreementContract.Commands.Finalise())
                failsWith("oracle should be the same entity on both states")
            }
        }
    }

}