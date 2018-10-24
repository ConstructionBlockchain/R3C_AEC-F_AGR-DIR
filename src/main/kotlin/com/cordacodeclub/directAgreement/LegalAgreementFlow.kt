package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.DirectAgreementContract.Commands.Create
import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*

@InitiatingFlow
@StartableByRPC
/**
 * This flow is to be started by the intermediary.
 */
class LegalAgreementFlow(val agreementValue: Amount<Currency>,
                         val partyA: Party,
                         val partyB: Party) : FlowLogic<Unit>() {

    /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
    override val progressTracker = ProgressTracker()

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable
    override fun call() {
        // We retrieve the first notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        // We create the transaction output.
        val outputState = LegalAgreementState(
                intermediary = ourIdentity,
                partyA = partyA,
                partyB = partyB,
                status = LegalAgreementState.Status.INTERMEDIATE,
                value = agreementValue)
        val cmd = Command(Create(), listOf(ourIdentity.owningKey, partyA.owningKey))

        val txBuilder = TransactionBuilder(notary = notary)

        //We add the output state to the builder
        txBuilder.addOutputState(outputState, ID)
        txBuilder.addCommand(cmd)

        // Verifying the transaction
        txBuilder.verify(serviceHub)

        // Signing the transaction
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // Creating a session with the other party
        val otherPartySession = initiateFlow(partyA)

        // Obtaining the counterparty's signature
        val fullySignedTx = subFlow(CollectSignaturesFlow(
                signedTx,
                listOf(otherPartySession), CollectSignaturesFlow.tracker()))

        // We finalise the transaction with the notary.
        subFlow(FinalityFlow(fullySignedTx))
    }
}