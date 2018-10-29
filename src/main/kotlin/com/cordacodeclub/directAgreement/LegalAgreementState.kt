package com.cordacodeclub.directAgreement

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

data class LegalAgreementState(
        val intermediary: Party,
        val partyA: Party,
        val partyB: Party,
        val oracle: Party,
        val status: Status,
        val value: Amount<Currency>) : ContractState {

    @CordaSerializable
    enum class Status { INTERMEDIATE, DIRECT, COMPLETED }

    init {
        requireThat {
            "The value should be positive" using (value.quantity > 0)
            "The intermediary and partyA cannot be the same entity" using (intermediary != partyA)
            "The partyA and partyB cannot be the same entity" using (partyA != partyB)
            "The partyB and intermediary cannot be the same entity" using (partyB != intermediary)
        }
    }

    override val participants: List<AbstractParty> = listOf(partyA, partyB)
}