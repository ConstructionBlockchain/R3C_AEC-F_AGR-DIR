package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.DirectAgreementContract.Commands.Create
import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*

object LegalAgreementFlow {

    @InitiatingFlow
    @StartableByRPC
    /**
     * This flow is to be started by the intermediary.
     */
    class LegalAgreementFlowInitiator(
            val agreementValue: Amount<Currency>,
            val partyA: Party,
            val partyB: Party,
            val oracle: Party,
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
            // We retrieve the first notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            progressTracker.currentStep = GENERATING_TRANSACTION
            // We create the transaction output.
            val outputState = LegalAgreementState(
                    intermediary = ourIdentity,
                    partyA = partyA,
                    partyB = partyB,
                    oracle = oracle,
                    status = LegalAgreementState.Status.INTERMEDIATE,
                    value = agreementValue)
            val cmd = Command(Create(), listOf(ourIdentity.owningKey, partyA.owningKey))

            val txBuilder = TransactionBuilder(notary = notary)

            //We add the output state to the builder
            txBuilder.addOutputState(outputState, ID)
            txBuilder.addCommand(cmd)

            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verifying the transaction
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            // Signing the transaction
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = GATHERING_SIGS
            // Creating a session with the other party
            val otherPartySession = initiateFlow(partyA)

            // Obtaining the counterparty's signature
            val fullySignedTx = subFlow(CollectSignaturesFlow(
                    signedTx,
                    listOf(otherPartySession), CollectSignaturesFlow.tracker()))

            progressTracker.currentStep = FINALISING_TRANSACTION
            // We finalise the transaction with the notary.
            subFlow(FinalityFlow(
                    fullySignedTx,
                    FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    /**
     * Define LegalAgreementFlowResponder
     * We want to be able to pass the expected parameters partyB and value so that we confirm we are not hoodwinked into
     * accepting values that we do not agree with.
     * As seen here https://docs.corda.net/flow-state-machines.html#a-two-party-trading-flow
     */
    @InitiatedBy(LegalAgreementFlowInitiator::class)
    class LegalAgreementFlowResponder(
            val otherPartySession: FlowSession,
            val partyB: Party,
            val value: Long,
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
                    val output = stx.tx.outputs.single().data
                    "This must be a LegalAgreement state." using (output is LegalAgreementState)
                    val legalAgreement = output as LegalAgreementState
                    "The legal agreement's value can't be negative." using (legalAgreement.value.quantity > 0)
                    "The legal agreement's value should be as expected." using (legalAgreement.value.quantity == value)
                    "The intermediary should be the sender." using (legalAgreement.intermediary == otherPartySession.counterparty)
                    "PartyA should be me." using (legalAgreement.partyA == ourIdentity)
                    "PartyB should be as expected." using (legalAgreement.partyB == partyB)
                    val contract = stx.tx.outputs.single().contract
                    "This must be a DirectAgreementContract." using (contract == ID)
                    progressTracker.currentStep = SIGNING_TRANSACTION
                }
            }
            subFlow(signTransactionFlow)
        }
    }
}