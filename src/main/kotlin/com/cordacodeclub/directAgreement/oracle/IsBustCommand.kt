package com.cordacodeclub.directAgreement.oracle

import net.corda.core.identity.Party

/**
 * Identifies a command that is accepted by the oracle
 */
open class IsBustCommand(val bustParty: Party, val isBust: Boolean)