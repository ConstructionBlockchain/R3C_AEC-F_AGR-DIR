package com.cordacodeclub.directAgreement.state

import com.cordacodeclub.directAgreement.schema.LegalAgreementSchemaV1
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.util.*

data class LegalAgreementState(
        val intermediary: Party,
        val partyA: Party,
        val partyB: Party,
        val oracle: Party,
        val status: Status,
        val value: Amount<Currency>) : ContractState, QueryableState {

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

    override val participants: List<AbstractParty> = listOf(partyA, partyB, intermediary)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is LegalAgreementSchemaV1 -> LegalAgreementSchemaV1.PersistentLegalAgreement(
                    this.intermediary.name.toString(),
                    this.partyA.name.toString(),
                    this.partyB.name.toString(),
                    this.oracle.name.toString(),
                    this.status.toString(),
                    this.value.quantity,
                    this.value.token.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(LegalAgreementSchemaV1)
}