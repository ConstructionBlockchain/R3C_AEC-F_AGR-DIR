package com.cordacodeclub.directAgreement.oracle

import net.corda.core.contracts.Command
import net.corda.core.crypto.TransactionSignature
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import java.security.PublicKey
import java.util.function.Predicate

@CordaService
class BustPartyOracle(val services: ServiceHub) : SingletonSerializeAsToken() {
    private val myKey = services.myInfo.legalIdentities.first().owningKey

    companion object {
        fun filterPredicate(oracle: PublicKey): Predicate<Any> = Predicate {
            it is Command<*> && it.value is IsBustCommand && oracle in it.signers
        }

        // Asking the oracle to sign the transaction
        // For privacy reasons, we only want to expose to the oracle any commands of type `IsBustCommand`
        // that require its signature.
        fun SignedTransaction.filterForBustPartyOracle(oracle: PublicKey): FilteredTransaction =
                this.buildFilteredTransaction(filterPredicate(oracle))
    }

    fun isItBust(party: Party): Boolean {
        val databaseService = services.cordaService(BustDatabaseService::class.java)
        return databaseService.queryIsBust(party.toString())
    }

    /** Returns true if the component is a IsBustCommand command that:
     *  - Has the oracle listed as a signer
     *  - States the proper state of a Party
     */
    fun FilteredTransaction.agreesWithBustPartyOracle(): Boolean = this.checkWithFun {
        it is Command<*>
                && myKey in it.signers
                && it.value is IsBustCommand
                && isItBust((it.value as IsBustCommand).bustParty) == (it.value as IsBustCommand).isBust
    }

    fun sign(ftx: FilteredTransaction): TransactionSignature {
        // Check the partial Merkle tree is valid.
        ftx.verify()

        if (ftx.agreesWithBustPartyOracle()) {
            return services.createSignature(ftx, myKey)
        } else {
            throw IllegalArgumentException("Oracle signature requested over invalid transaction.")
        }
    }
}