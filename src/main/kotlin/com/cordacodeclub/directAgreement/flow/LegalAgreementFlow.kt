package com.cordacodeclub.directAgreement.flow

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.state.LegalAgreementState
import com.cordacodeclub.directAgreement.contract.DirectAgreementContract.Commands.Create
import com.cordacodeclub.directAgreement.contract.DirectAgreementContract.Companion.ID
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
     * For example:
     * flow start com.cordacodeclub.directAgreement.flow.LegalAgreementFlow$LegalAgreementFlowInitiator\
     *     agreementValue: "20 USD",\
     *     partyA: "O=PartyA,L=London,C=GB",\
     *     partyB: "O=PartyB,L=New York,C=US",\
     *     oracle: "O=PartyC,L=Paris,C=FR"
     */
    class LegalAgreementFlowInitiator(
            val agreementValue: Amount<Currency>,
            val partyA: Party,
            val partyB: Party,
            val oracle: Party) : FlowLogic<SignedTransaction>() {

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new info.")
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

        override val progressTracker: ProgressTracker = tracker()

        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
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
                    listOf(otherPartySession),
                    GATHERING_SIGS.childProgressTracker()))

            progressTracker.currentStep = FINALISING_TRANSACTION
            // We finalise the transaction with the notary.
            return subFlow(FinalityFlow(
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
        open class LegalAgreementFlowResponder(
            val otherPartySession: FlowSession) : FlowLogic<Unit>() {

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object SIGNING_TRANSACTION: ProgressTracker.Step("Signing transaction with our private key.") {
                override fun childProgressTracker(): ProgressTracker = SignTransactionFlow.tracker()
            }
            object SIGNED: ProgressTracker.Step("Finished signing.")

            fun tracker() = ProgressTracker(SIGNING_TRANSACTION, SIGNED)
        }

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call() {

            val signTransactionFlow = object : SignTransactionFlow(
                    otherPartySession,
                    SIGNING_TRANSACTION.childProgressTracker()) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a LegalAgreement state." using (output is LegalAgreementState)
                    val legalAgreement = output as LegalAgreementState
                    "The legal agreement's value can't be negative." using (legalAgreement.value.quantity > 0)

                    // We need a way to have this value checked manually
//                    "The legal agreement's value should be as expected." using (legalAgreement.value.quantity == value)

                    "The intermediary should be the sender." using (legalAgreement.intermediary == otherPartySession.counterparty)
                    "PartyA should be me." using (legalAgreement.partyA == ourIdentity)

                    // We need a way to have this party checked manually
//                    "PartyB should be as expected." using (legalAgreement.partyB == partyB)

                    val contract = stx.tx.outputs.single().contract
                    "This must be a DirectAgreementContract." using (contract == ID)
                }
            }
            subFlow(signTransactionFlow)
            progressTracker.currentStep = SIGNED
        }
    }
}