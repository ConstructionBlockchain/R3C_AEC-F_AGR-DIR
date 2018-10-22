package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.transactions.SignedTransaction

//Define LegalAgreementFlowResponder
@InitiatedBy(LegalAgreementFlow::class)
class AgreementFlowResponder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an LegalAgreement transaction." using (output is LegalAgreementState)
                val legalAgreement = output as LegalAgreementState
                "The legal agreement's value can't be negative." using (legalAgreement.value.quantity > 0)
            }
        }
        subFlow(signTransactionFlow)
    }
}