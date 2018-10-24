package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


//This flow is to be started by PartyA and either of two parties depending on the input state status.
//In this case it can be identified as PartyC

@InitiatingFlow
@StartableByRPC
class EndAgreementFlow(val inputState: LegalAgreementState,
                          val partyC: Party) : FlowLogic<Unit>() {

    /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
    override val progressTracker = ProgressTracker()

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable
    override fun call() {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val tx3Builder = TransactionBuilder(notary = notary)

        // We create the transaction components.
        val outputState = inputState.copy(status = LegalAgreementState.Status.COMPLETED)

        val outputAgreementContractState = StateAndContract(outputState, ID)
        val cmd = Command(DirectAgreementContract.Commands.Finalise(),
                listOf(ourIdentity.owningKey, partyC.owningKey))

        //We add the items ot the builder
        tx3Builder.withItems(outputAgreementContractState, cmd)

        //Verifying the transaction
        tx3Builder.verify(serviceHub)

        //Signing the transaction
        val signedTx3 = serviceHub.signInitialTransaction(tx3Builder)

        //Creating a session with the other party
        val otherPartySession = initiateFlow(partyC)

        //Obtaining the counterparty's signature
        val fullySignedtx3 = subFlow(CollectSignaturesFlow(signedTx3,
                listOf(otherPartySession), CollectSignaturesFlow.tracker()))

        // We finalise the transaction.
        subFlow(FinalityFlow(fullySignedtx3))
    }
}