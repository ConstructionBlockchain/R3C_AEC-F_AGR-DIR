package com.cordacodeclub.directAgreement.flow

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.contract.DirectAgreementContract
import com.cordacodeclub.directAgreement.oracle.BustPartyOracle.Companion.filterForBustPartyOracle
import com.cordacodeclub.directAgreement.state.LegalAgreementState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object DirectAgreementFlow {

    @InitiatingFlow
    @StartableByRPC
    /**
     * This flow can be started by partyA or partyB.
     * And it has to be signed by partyA and partyB.
     * Example:
     * flow start com.cordacodeclub.directAgreement.flow.DirectAgreementFlow$DirectAgreementFlowInitiator\
     *     txhash: "C4D1AE65784CA5D7BDB56E7C6CFF2F210FE0A2F7D910120E3816BC20739F8CCE",\
     *     index: 0
     */
    class DirectAgreementFlowInitiator(
            val inputStateRef: StateRef,
            override val progressTracker: ProgressTracker = tracker()) : FlowLogic<SignedTransaction>() {

        // Useful for the shell
        constructor(txhash: String, index: Int) : this(StateRef(SecureHash.parse(txhash), index))

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object QUERYING_ORACLE: ProgressTracker.Step("Querying the BustPartyOracle.") {
                override fun childProgressTracker() = BustPartyOracleFlow.QueryBustPartyInitiator.tracker()
            }
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_ORACLE_SIG : ProgressTracker.Step("Gathering the oracle's signature.") {
                override fun childProgressTracker(): ProgressTracker = BustPartyOracleFlow.SignBustParty.tracker()
            }
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    QUERYING_ORACLE,
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_ORACLE_SIG,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION)
        }

        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // We retrieve the first notary identity from the network map.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            progressTracker.currentStep = QUERYING_ORACLE
            // We create the transaction output state from the input.
            val inputStateAndRef = serviceHub.toStateAndRef<LegalAgreementState>(inputStateRef)
            val inputState = inputStateAndRef.state.data
            val otherPartySet = setOf(inputState.partyA, inputState.partyB).minus(ourIdentity)
            requireThat {
                "You are neither partyA nor partyB" using(otherPartySet.size == 1)
                "The state has to be INTERMEDIATE" using(inputState.status == LegalAgreementState.Status.INTERMEDIATE)
            }
            val outputState = inputState.copy(status = LegalAgreementState.Status.DIRECT)

            val isBustFromOracle = subFlow(BustPartyOracleFlow.QueryBustPartyInitiator(
                    inputState.oracle,
                    inputState.intermediary,
                    QUERYING_ORACLE.childProgressTracker()))

            requireThat {
                "The oracle has to identify the intermediary as bust" using(isBustFromOracle)
            }

            progressTracker.currentStep = GENERATING_TRANSACTION
            // We create the transaction components based on the input state
            val requiredSigners = setOf(inputState.partyA, inputState.partyB, inputState.oracle)
            val cmd = Command(
                    DirectAgreementContract.Commands.GoToDirect(inputState.intermediary, isBustFromOracle),
                    requiredSigners.map { it.owningKey }.toList())
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
            val signedTx1 = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = GATHERING_ORACLE_SIG
            val oracleSignature = subFlow(BustPartyOracleFlow.SignBustParty(
                    inputState.oracle,
                    signedTx1.filterForBustPartyOracle(inputState.oracle.owningKey),
                    GATHERING_ORACLE_SIG.childProgressTracker()))
            val signedTx = signedTx1.withAdditionalSignature(oracleSignature)

            progressTracker.currentStep = GATHERING_SIGS
            val otherPartySession = initiateFlow(otherParty)

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
     * Define DirectAgreementFlowResponder
     * We want to be able to call this flow only when we confirm that we are expecting it.
     */
    @InitiatedBy(DirectAgreementFlowInitiator::class)
    class DirectAgreementFlowResponder(
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
                    this@DirectAgreementFlowResponder.progressTracker.currentStep = CHECKING_VALIDITY
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