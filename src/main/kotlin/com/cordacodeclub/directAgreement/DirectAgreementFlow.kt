package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalArgumentException

@InitiatingFlow
@StartableByRPC
/**
 * This flow can be started by partyA or partyB.
 * And it has to be signed by partyA and partyB.
 */
class DirectAgreementFlow(val inputStateAndRef: StateAndRef<LegalAgreementState>) : FlowLogic<Unit>() {

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

        // We add the items ot the builder
        txBuilder.addOutputState(outputState, ID)
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