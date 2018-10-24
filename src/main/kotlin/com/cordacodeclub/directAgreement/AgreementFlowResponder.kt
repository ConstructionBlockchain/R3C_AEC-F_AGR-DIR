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

//Define LegalAgreementFlowResponder
@InitiatedBy(LegalAgreementFlow::class)
class AgreementFlowResponder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an LegalAgreement state." using (output is LegalAgreementState)
                val legalAgreement = output as LegalAgreementState
                "The legal agreement's value can't be negative." using (legalAgreement.value.quantity > 0)
                "The intermediary should be the sender." using (legalAgreement.intermediary == otherPartySession.counterparty)
                "PartyA should be me." using (legalAgreement.partyA == ourIdentity)
                val contract = stx.tx.outputs.single().contract
                "This must be a DirectAgreementContract." using (contract == ID)
            }
        }
        subFlow(signTransactionFlow)
    }
}