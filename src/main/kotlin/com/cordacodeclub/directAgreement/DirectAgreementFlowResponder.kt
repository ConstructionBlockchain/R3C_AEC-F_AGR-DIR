package com.cordacodeclub.directAgreement

import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.transactions.SignedTransaction

/**
 * Define LegalAgreementFlowResponder
 * We want to be able to call this flow only when we confirm that we are expecting it.
 */
@InitiatedBy(DirectAgreementFlow::class)
class DirectAgreementFlowResponder(val otherPartySession: FlowSession,
                                   val isOk: Boolean) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                "This flow must have been approved" using (isOk)

                // How to confirm that this builds on an input we agreed on and of which we are party

                // Do we need to check that the output "matches" the input as if the tx was verified?
            }
        }
        subFlow(signTransactionFlow)
    }
}