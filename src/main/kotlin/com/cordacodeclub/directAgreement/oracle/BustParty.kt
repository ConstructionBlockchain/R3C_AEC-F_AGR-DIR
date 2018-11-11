package com.cordacodeclub.directAgreement.oracle

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class BustParty(val party: String, val isBust: Boolean)