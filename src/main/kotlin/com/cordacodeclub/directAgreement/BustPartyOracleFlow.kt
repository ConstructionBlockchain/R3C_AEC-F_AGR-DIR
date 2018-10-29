package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

object BustPartyOracleFlow {

    @InitiatingFlow
    @StartableByRPC
    class QueryBustPartyInitiator(val oracle: Party, val party: Party) : FlowLogic<Boolean>() {
        @Suspendable
        override fun call() = initiateFlow(oracle).sendAndReceive<Boolean>(party).unwrap { it }
    }

    @InitiatedBy(QueryBustPartyInitiator::class)
    open class QueryBustPartyHandler(val session: FlowSession) : FlowLogic<Unit>() {
        companion object {
            object RECEIVING : ProgressTracker.Step("Receiving query request.")
            object FETCHING : ProgressTracker.Step("Fetching bust status.")
            object SENDING : ProgressTracker.Step("Sending query response.")
        }

        override val progressTracker = ProgressTracker(RECEIVING, FETCHING, SENDING)

        open fun bustPartyOracle() = serviceHub.cordaService(BustPartyOracle::class.java)

        @Suspendable
        override fun call() {
            progressTracker.currentStep = RECEIVING
            val party = session.receive<Party>().unwrap { it }

            progressTracker.currentStep = FETCHING
            val response = try {
                bustPartyOracle().isItBust(party)
            } catch (e: Exception) {
                // Re-throw the exception as a FlowException so its propagated to the querying node.
                throw FlowException(e)
            }

            progressTracker.currentStep = SENDING
            session.send(response)
        }
    }

    @InitiatingFlow
    @StartableByRPC
    class SignBustParty(val oracle: Party, val ftx: FilteredTransaction) : FlowLogic<TransactionSignature>() {
        @Suspendable override fun call(): TransactionSignature {
            val session = initiateFlow(oracle)
            return session.sendAndReceive<TransactionSignature>(ftx).unwrap { it }
        }
    }

    @InitiatedBy(SignBustParty::class)
    open class SignHandler(val session: FlowSession) : FlowLogic<Unit>() {
        companion object {
            object RECEIVING : ProgressTracker.Step("Receiving sign request.")
            object SIGNING : ProgressTracker.Step("Signing filtered transaction.")
            object SENDING : ProgressTracker.Step("Sending sign response.")
        }

        override val progressTracker = ProgressTracker(RECEIVING, SIGNING, SENDING)

        open fun bustPartyOracle() = serviceHub.cordaService(BustPartyOracle::class.java)

        @Suspendable
        override fun call() {
            progressTracker.currentStep = RECEIVING
            val request = session.receive<FilteredTransaction>().unwrap { it }

            progressTracker.currentStep = SIGNING
            val response = try {
                bustPartyOracle().sign(request)
            } catch (e: Exception) {
                throw FlowException(e)
            }

            progressTracker.currentStep = SENDING
            session.send(response)
        }
    }

}