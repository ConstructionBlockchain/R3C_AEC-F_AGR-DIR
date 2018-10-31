package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


//This flow is to be started by PartyA and either of two parties depending on the input state status.
//In this case it can be identified as PartyC

object EndAgreementFlow{

    @InitiatingFlow
    @StartableByRPC
    /**
     * This flow can be started by partyA or PartyB
     * And it has to be signed by partyA or PartyB]
     */

    class EndAgreementFlowInitiator(
            val inputStateAndRef: StateAndRef<LegalAgreementState>,
            override val progressTracker: ProgressTracker = tracker()) : FlowLogic<Unit>() {

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION)
        }

        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call() {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            progressTracker.currentStep = GENERATING_TRANSACTION
            // We create the transaction output state from the input
            val inputState = inputStateAndRef.state.data
            val outputState = inputState.copy(status = LegalAgreementState.Status.COMPLETED)

            // We create the transaction components based on the input state
            val cmd = if (inputState.status == LegalAgreementState.Status.INTERMEDIATE) {
                Command(DirectAgreementContract.Commands.Finalise(),
                        listOf(inputState.partyA.owningKey, inputState.intermediary.owningKey))
            } else {
                Command(DirectAgreementContract.Commands.Finalise(),
                        listOf(inputState.partyA.owningKey, inputState.partyB.owningKey))
            }

            val txBuilder = TransactionBuilder(notary = notary)

            // We add the items of the builder
            txBuilder.addOutputState(outputState, DirectAgreementContract.ID)
            txBuilder.addCommand(cmd)
            txBuilder.addInputState(inputStateAndRef)

            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verifying the transaction
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            // Signing the transaction
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // Creating a session with the other party depending on the input state
            val otherParty = when (inputState.status) {
                LegalAgreementState.Status.INTERMEDIATE ->
                    if (ourIdentity == inputState.partyA)
                        inputState.partyB
                    else
                        inputState.partyA
                LegalAgreementState.Status.DIRECT ->
                    if (ourIdentity == inputState.partyA)
                        inputState.intermediary
                    else
                        inputState.partyA
                else -> throw java.lang.IllegalArgumentException("Unexpected party")
            }
            progressTracker.currentStep = GATHERING_SIGS
            val otherPartySession = initiateFlow(otherParty)

            // Obtaining the counterparty's signature
            val fullySignedTx = subFlow(CollectSignaturesFlow(
                    signedTx,
                    listOf(otherPartySession),
                    GATHERING_SIGS.childProgressTracker()))

            progressTracker.currentStep = FINALISING_TRANSACTION
            // We finalise the transaction with the notary
            subFlow(FinalityFlow(
                    fullySignedTx,
                    FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    /**
     * Define EndAgreementFlowResponder
     * We want to be able to call this flow only when we confirm that we are expecting it.
     */
    @InitiatedBy(EndAgreementFlow.EndAgreementFlowInitiator::class)
    class EndAgreementFlowResponder(
            val otherPartySession: FlowSession,
            val isOk: Boolean,
            override val progressTracker: ProgressTracker = tracker()) : FlowLogic<Unit>() {

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object CHECKING_VALIDITY: ProgressTracker.Step("Checking transaction validity.")
            object SIGNING_TRANSACTION: ProgressTracker.Step("Signing transaction with our private key.")

            fun tracker() = ProgressTracker(CHECKING_VALIDITY, SIGNING_TRANSACTION)
        }

        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    progressTracker.currentStep = CHECKING_VALIDITY
                    "This flow must have been approved" using (isOk)

                    // How to confirm that this builds on an input we agreed on and of which we are party

                    // Do we need to check that the output "matches" the input as if the tx was verified?
                    progressTracker.currentStep = SIGNING_TRANSACTION
                }
            }
            subFlow(signTransactionFlow)
        }
    }
}
