package com.cordacodeclub.directAgreement.flow.mock

import com.cordacodeclub.directAgreement.flow.LegalAgreementFlow
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy

@InitiatedBy(LegalAgreementFlow.LegalAgreementFlowInitiator::class)
class LegalAgreementFlowResponderMock(session: FlowSession): LegalAgreementFlow.LegalAgreementFlowResponder(session) {
}