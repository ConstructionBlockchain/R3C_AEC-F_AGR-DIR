package com.cordacodeclub.directAgreement.contract

import com.cordacodeclub.directAgreement.state.LegalAgreementState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.security.PublicKey

class DirectAgreementContract : Contract {
    companion object {
        val ID = "com.cordacodeclub.directAgreement.contract.DirectAgreementContract"
    }

    fun verifyState(state: LegalAgreementState, signers: List<PublicKey>) {
        requireThat {
            "There must be only 2 signers" using (signers.toSet().size == 2)
            "Intermediary and partyA must be signers" using (signers.containsAll(listOf(
                    state.intermediary.owningKey, state.partyA.owningKey)))
        }
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val inputCount = tx.inputsOfType<LegalAgreementState>().count()
        val outputCount = tx.outputsOfType<LegalAgreementState>().count()

        requireThat {
            "There should be one output state of type LegalAgreementState" using (outputCount == 1)
        }
        val output = tx.outputsOfType<LegalAgreementState>().single()

        when (command.value) {
            is Commands.Create -> requireThat {
                "There must be only 2 signers" using (command.signers.toSet().size == 2)
                "There should be no input of type LegalAgreementState when creating via Intermediary" using (inputCount == 0)
                "The output should have status INTERMEDIATE" using (output.status == LegalAgreementState.Status.INTERMEDIATE)
                "Intermediary and partyA must be signers" using (command.signers.containsAll(listOf(
                        output.intermediary.owningKey, output.partyA.owningKey)))
            }

            is Commands.GoToDirect -> requireThat {
                "There must be only 3 signers" using (command.signers.toSet().size == 3)
                "One LegalAgreementState should be consumed when creating Direct" using (inputCount == 1)
                "There should be one output state of type LegalAgreementState" using (outputCount == 1)
                val input = tx.inputsOfType<LegalAgreementState>().single()
                "The input should have status INTERMEDIATE" using (input.status == LegalAgreementState.Status.INTERMEDIATE)
                val commandValue = command.value as Commands.GoToDirect
                "The intermediary needs to be mentioned in the command" using (input.intermediary == commandValue.party)
                "The intermediate needs to be bust" using (commandValue.isBust)
                // Can we assume that the requirements in `Create` are fulfilled?
                "The output should have status DIRECT" using (output.status == LegalAgreementState.Status.DIRECT)
                "The output value should be equal to the input value" using (output.value == input.value)
                "The intermediary should be the same entity on both states" using (input.intermediary == output.intermediary)
                "The partyA should be the same entity on both states" using (input.partyA == output.partyA)
                "The partyB should be the same entity on both states" using (input.partyB == output.partyB)
                "The oracle should be the same entity on both states" using (input.oracle == output.oracle)
                "PartyA, partyB and the oracle must be signers" using (command.signers.containsAll(listOf(
                        output.partyA.owningKey, output.partyB.owningKey, output.oracle.owningKey)))
            }

            is Commands.Finalise -> {
                requireThat {
                    "There must be only 2 signers" using (command.signers.toSet().size == 2)
                    "One LegalAgreementState should be consumed when creating Direct" using (inputCount == 1)
                }

                val input = tx.inputsOfType<LegalAgreementState>().single()
                requireThat {
                    "The input should have status INTERMEDIATE or DIRECT" using
                            (input.status == LegalAgreementState.Status.INTERMEDIATE || input.status == LegalAgreementState.Status.DIRECT)
                    "The output should have status COMPLETED" using (output.status == LegalAgreementState.Status.COMPLETED)
                    "The output value should be equal to the input value" using (output.value == input.value)
                    "The intermediary should be the same entity on both states" using (input.intermediary == output.intermediary)
                    "The partyA should be the same entity on both states" using (input.partyA == output.partyA)
                    "The partyB should be the same entity on both states" using (input.partyB == output.partyB)
                    "The oracle should be the same entity on both states" using (input.oracle == output.oracle)

                    "There must be only 2 signers" using (command.signers.toSet().size == 2)
                }

                when (input.status) {
                    LegalAgreementState.Status.INTERMEDIATE -> requireThat {
                        "PartyA and Intermediary must be the signers" using (command.signers.containsAll(listOf(input.partyA.owningKey, input.intermediary.owningKey)))
                    }
                    LegalAgreementState.Status.DIRECT -> requireThat {
                        "PartyA and PartyB must be the signers" using (command.signers.containsAll(listOf(input.partyA.owningKey, input.partyB.owningKey)))
                    }
                    LegalAgreementState.Status.COMPLETED -> throw IllegalArgumentException("Should not be Completed input")
                }
            }

            else -> throw IllegalArgumentException("Unrecognised command")
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class GoToDirect(val party: Party, val isBust: Boolean) : Commands
        class Finalise : Commands
        // Will want an oracle on Intermediary being bust
    }
}