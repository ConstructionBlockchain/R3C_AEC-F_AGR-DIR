package com.cordacodeclub.directAgreement.flow.mock

import com.cordacodeclub.directAgreement.flow.BustPartyOracleFlow
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy

@InitiatedBy(BustPartyOracleFlow.SignBustParty::class)
class BustPartySignHandlerMock(session: FlowSession): BustPartyOracleFlow.SignHandler(session) {
}