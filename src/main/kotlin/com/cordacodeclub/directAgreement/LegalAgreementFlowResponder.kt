package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

/**
 * Define LegalAgreementFlowResponder
 * We want to be able to pass the expected parameters partyB and value so that we confirm we are not hoodwinked into
 * accepting values that we do not agree with.
 * As seen here https://docs.corda.net/flow-state-machines.html#a-two-party-trading-flow
 */
@InitiatedBy(LegalAgreementFlow::class)
class LegalAgreementFlowResponder(val otherPartySession: FlowSession,
                                  val partyB: Party,
                                  val value: Long) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
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
            }
        }
        subFlow(signTransactionFlow)
    }
}