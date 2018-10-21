package com.cordacodeclub.directAgreement

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

data class LegalAgreementState(
        val spv: Party,
        val contractor: Party,
        val lender: Party,
        val status: Status,
        val value: Amount<Currency>) : ContractState {

    enum class Status { VIASPV, DIRECT }

    init {
        requireThat {
            "The value should be positive" using (value.quantity > 0)
            "The SPV and contractor cannot be the same entity" using (spv != contractor)
            "The contractor and lender cannot be the same entity" using (contractor != lender)
            "The lender and SPV cannot be the same entity" using (lender != spv)
        }
    }

    override val participants: List<AbstractParty> = listOf(contractor, lender)
}