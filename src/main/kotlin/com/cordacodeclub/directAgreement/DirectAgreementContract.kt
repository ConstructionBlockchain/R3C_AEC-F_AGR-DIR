package com.cordacodeclub.directAgreement

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.security.PublicKey

class DirectAgreementContract : Contract {
    companion object {
        val ID = "com.cordacodeclub.directAgreement.DirectAgreementContract"
    }

    fun verifyState(state: LegalAgreementState, signers: List<PublicKey>) {
        requireThat {

            "There must be only 2 signers" using (signers.toSet().size == 2)
            "SPV and contractor must be signers" using (signers.containsAll(listOf(
                    state.spv.owningKey, state.contractor.owningKey)))
        }
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val inputCount = tx.inputsOfType<LegalAgreementState>().count()
        requireThat {
            val outputCount = tx.outputsOfType<LegalAgreementState>().count()
            "There should be one output state of type LegalAgreementState" using (outputCount == 1)
        }

        val output = tx.outputsOfType<LegalAgreementState>().single()
        requireThat {
            "There must be only 2 signers" using (command.signers.toSet().size == 2)
        }

        when (command.value) {
            is Commands.CreateViaSpv -> {
                requireThat {
                    "There should be no input of type LegalAgreementState when creating via SPV" using (inputCount == 0)
                    "The output should have status VIASPV" using (output.status == LegalAgreementState.Status.VIASPV)
                    "SPV and contractor must be signers" using (command.signers.containsAll(listOf(
                            output.spv.owningKey, output.contractor.owningKey)))
                }
            }

            is Commands.GoToDirect -> {
                requireThat {
                    "One LegalAgreementState should be consumed when creating Direct" using (inputCount == 1)
                }

                val input = tx.inputsOfType<LegalAgreementState>().single()
                requireThat {
                    "The input should have status VIASPV" using (input.status == LegalAgreementState.Status.VIASPV)
                    // Can we assume that the requirements in `CreateViaSpv` are fulfilled?
                }

                requireThat {
                    "The output should have status DIRECT" using (output.status == LegalAgreementState.Status.DIRECT)
                    "The output value should be equal to the input value" using (output.value == input.value)
                    "The SPV should be the same entity on both states" using (input.spv == output.spv)
                    "The contractor should be the same entity on both states" using (input.contractor == output.contractor)
                    "The lender should be the same entity on both states" using (input.lender == output.lender)

                    "Lender and contractor must be signers" using (command.signers.containsAll(listOf(
                            output.lender.owningKey, output.contractor.owningKey)))
                }
            }

            else -> throw IllegalArgumentException("Unrecognised command")
        }
    }

    interface Commands : CommandData {
        class CreateViaSpv : Commands
        class GoToDirect : Commands // Will want an oracle on SPV being bust
    }
}