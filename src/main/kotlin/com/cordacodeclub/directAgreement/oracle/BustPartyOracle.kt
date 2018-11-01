package com.cordacodeclub.directAgreement.oracle

import com.cordacodeclub.directAgreement.contract.DirectAgreementContract
import net.corda.core.contracts.Command
import net.corda.core.crypto.TransactionSignature
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction

@CordaService
class BustPartyOracle(val services: ServiceHub) : SingletonSerializeAsToken() {
    private val myKey = services.myInfo.legalIdentities.first().owningKey

    // Eventually, this function should be real
    fun isItBust(party: Party): Boolean {
        return true
    }

    fun sign(ftx: FilteredTransaction): TransactionSignature {
        // Check the partial Merkle tree is valid.
        ftx.verify()

        /** Returns true if the component is a GoToDirect command that:
         *  - States the proper state of a Party
         *  - Has the oracle listed as a signer
         */
        fun isValid(elem: Any) = when {
            elem is Command<*> && elem.value is DirectAgreementContract.Commands.GoToDirect -> {
                val cmdData = elem.value as DirectAgreementContract.Commands.GoToDirect
                myKey in elem.signers && isItBust(cmdData.party) == cmdData.isBust
            }
            else  -> false
        }

        val isOk = ftx.checkWithFun(::isValid)

        if (isOk) {
            return services.createSignature(ftx, myKey)
        } else {
            throw IllegalArgumentException("Oracle signature requested over invalid transaction.")
        }
    }
}