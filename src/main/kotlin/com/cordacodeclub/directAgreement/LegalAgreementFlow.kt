import co.paralleluniverse.fibers.Suspendable
import com.cordacodeclub.directAgreement.DirectAgreementContract.Commands.Create
import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
import com.cordacodeclub.directAgreement.LegalAgreementState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*

@InitiatingFlow
@StartableByRPC
class LegalAgreementFlow(val agreementValue: Amount<Currency>,
                         val partyA: Party,
                         val partyB: Party) : FlowLogic<Unit>() {

    /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
    override val progressTracker = ProgressTracker()

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable
    override fun call() {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val tx1Builder = TransactionBuilder(notary = notary)

        // We create the transaction components.
        val outputState = LegalAgreementState(ourIdentity, partyA, partyB,
                LegalAgreementState.Status.INTERMEDIATE, agreementValue)
        val outputAgreementContractState = StateAndContract(outputState, ID)
        val cmd = Command(Create(),
                listOf(ourIdentity.owningKey, partyA.owningKey))

        //We add the items ot the builder
        tx1Builder.withItems(outputAgreementContractState, cmd)

        //Verifying the transaction
        tx1Builder.verify(serviceHub)

        //Signing the transaction
        val signedTx1 = serviceHub.signInitialTransaction(tx1Builder)

        //Creating a session with the other party
        val otherPartySession = initiateFlow(partyA)

        //Obtaining the counterparty's signature
        val fullySignedTx1 = subFlow(CollectSignaturesFlow(signedTx1,
                listOf(otherPartySession), CollectSignaturesFlow.tracker()))

        // We finalise the transaction.
        subFlow(FinalityFlow(fullySignedTx1))
    }
}