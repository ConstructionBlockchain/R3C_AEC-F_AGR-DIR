package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalArgumentException

object DirectAgreementFlow {

    @InitiatingFlow
    @StartableByRPC
    /**
     * This flow can be started by partyA or partyB.
     * And it has to be signed by partyA and partyB.
     */
    class DirectAgreementFlowInitiator(val inputStateAndRef: StateAndRef<LegalAgreementState>) : FlowLogic<Unit>() {

        /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
        override val progressTracker = ProgressTracker()

        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call() {
            // We retrieve the first notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // We create the transaction output state from the input.
            val inputState = inputStateAndRef.state.data
            val outputState = inputState.copy(status = LegalAgreementState.Status.DIRECT)

            val cmd = Command(DirectAgreementContract.Commands.GoToDirect(),
                    listOf(inputState.partyA.owningKey, inputState.partyB.owningKey))

            val txBuilder = TransactionBuilder(notary = notary)

            // We add the items of the builder
            txBuilder.addOutputState(outputState, DirectAgreementContract.ID)
            txBuilder.addCommand(cmd)
            txBuilder.addInputState(inputStateAndRef)

            // Verifying the transaction
            txBuilder.verify(serviceHub)

            // Signing the transaction
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // Creating a session with the other party
            val otherParty = if (ourIdentity == inputState.partyA)
                inputState.partyB
            else (if (ourIdentity == inputState.partyB)
                inputState.partyA
            else throw IllegalArgumentException("Unexpected party"))
            val otherPartySession = initiateFlow(otherParty)

            // Obtaining the counterparty's signature
            val fullySignedTx = subFlow(CollectSignaturesFlow(
                    signedTx,
                    listOf(otherPartySession), CollectSignaturesFlow.tracker()))

            // We finalise the transaction with the notary.
            subFlow(FinalityFlow(fullySignedTx))
        }
    }

    /**
     * Define LegalAgreementFlowResponder
     * We want to be able to call this flow only when we confirm that we are expecting it.
     */
    @InitiatedBy(DirectAgreementFlowInitiator::class)
    class DirectAgreementFlowResponder(val otherPartySession: FlowSession,
                                       val isOk: Boolean) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    "This flow must have been approved" using (isOk)

                    // How to confirm that this builds on an input we agreed on and of which we are party

                    // Do we need to check that the output "matches" the input as if the tx was verified?
                }
            }
            subFlow(signTransactionFlow)
        }
    }
}