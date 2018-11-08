package com.cordacodeclub.directAgreement.schema

import net.corda.core.contracts.Amount
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.finance.DOLLARS
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for LegalAgreement.
 */
object LegalAgreementSchema

/**
 * An LegalAgreement schema.
 */
object LegalAgreementSchemaV1 : MappedSchema(
        schemaFamily = LegalAgreementSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentLegalAgreement::class.java)) {
    @Entity
    @Table(name = "legalAgreement_states")
    class PersistentLegalAgreement(
            @Column(name = "intermediary")
            var intermediary: String,

            @Column(name = "partyA")
            var partyA: String,

            @Column(name = "partyB")
            var partyB: String,

            @Column(name = "oracle")
            var oracle: String,

            @Column(name = "status")
            var status: String,

            @Column(name = "value")
            var value: Amount<Currency>
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("", "", "", "", "", 0.DOLLARS)
    }
}
