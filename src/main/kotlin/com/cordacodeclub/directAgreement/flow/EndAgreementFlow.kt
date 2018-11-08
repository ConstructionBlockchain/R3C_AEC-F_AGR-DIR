package com.cordacodeclub.directAgreement.flow

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.state.LegalAgreementState
import com.cordacodeclub.directAgreement.contract.DirectAgreementContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalArgumentException


//This flow is to be started by PartyA and either of two parties depending on the input state status.
//In this case it can be identified as PartyC

object EndAgreementFlow{

    @InitiatingFlow
    @StartableByRPC
    /**
     * This flow can be started by partyA or PartyB
     * And it has to be signed by partyA or PartyB]
     * Example:
     * flow start com.cordacodeclub.directAgreement.flow.EndAgreementFlow$EndAgreementFlowInitiator\
     *     txhash: "2A7C7A70B7EBF9D4915BD83828C391586F6582E5E2448214ECA1483ACDEF56E7",\
     *     index: 0
     */

    class EndAgreementFlowInitiator(
            val inputStateRef: StateRef,
            override val progressTracker: ProgressTracker = tracker()): FlowLogic<SignedTransaction>() {

        // Useful for the shell
        constructor(txSecurehash: SecureHash, index: Int) : this(StateRef(txSecurehash, index))
        constructor(txhash: String, index: Int) : this(SecureHash.parse(txhash), index)

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
        override fun call(): SignedTransaction {
            // We retrieve the notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            progressTracker.currentStep = GENERATING_TRANSACTION
            // We create the transaction output state from the input

            val inputStateAndRef = serviceHub.toStateAndRef<LegalAgreementState>(inputStateRef)
            val inputState = inputStateAndRef.state.data
            val outputState = inputState.copy(status = LegalAgreementState.Status.COMPLETED)

            // We create the transaction components based on the input state
            val requiredSigners = when(inputState.status) {
                LegalAgreementState.Status.INTERMEDIATE -> setOf(inputState.partyA, inputState.intermediary)
                LegalAgreementState.Status.DIRECT -> setOf(inputState.partyA, inputState.partyB)
                else -> throw IllegalArgumentException("Unexpected Status:" + inputState.status.name)
            }

            val cmd = Command(
                    DirectAgreementContract.Commands.Finalise(),
                    requiredSigners.map { it.owningKey }.toList())
            val otherPartySet = requiredSigners.minus(ourIdentity)
            requireThat {
                "There should be only one other party" using(otherPartySet.size == 1)
            }
            val otherParty = otherPartySet.single()

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
            progressTracker.currentStep = GATHERING_SIGS
            val otherPartySession = initiateFlow(otherParty)

            // Obtaining the counterparty's signature
            val fullySignedTx = subFlow(CollectSignaturesFlow(
                    signedTx,
                    listOf(otherPartySession),
                    GATHERING_SIGS.childProgressTracker()))

            progressTracker.currentStep = FINALISING_TRANSACTION
            // We finalise the transaction with the notary
            return subFlow(FinalityFlow(
                    fullySignedTx,
                    FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    /**
     * Define EndAgreementFlowResponder
     * We want to be able to call this flow only when we confirm that we are expecting it.
     */
    @InitiatedBy(EndAgreementFlowInitiator::class)
    class EndAgreementFlowResponder(
            val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object SIGNING_TRANSACTION: ProgressTracker.Step("Signing transaction with our private key.") {
                override fun childProgressTracker(): ProgressTracker {
                    return SignTransactionFlow.tracker()
                }
            }
            object CHECKING_VALIDITY: ProgressTracker.Step("Checking transaction validity.")

            fun tracker() = ProgressTracker(
                    SIGNING_TRANSACTION,
                    CHECKING_VALIDITY)
        }

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = SIGNING_TRANSACTION
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SIGNING_TRANSACTION.childProgressTracker()) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    this@EndAgreementFlowResponder.progressTracker.currentStep = CHECKING_VALIDITY
                    // We need to have this info placed in the vault beforehand
//                    "This flow must have been approved" using (isOk)

                    // How to confirm that this builds on an input we agreed on and of which we are party

                    // Do we need to check that the output "matches" the input as if the tx was verified?
                }
            }
            return subFlow(signTransactionFlow)
        }
    }
}
