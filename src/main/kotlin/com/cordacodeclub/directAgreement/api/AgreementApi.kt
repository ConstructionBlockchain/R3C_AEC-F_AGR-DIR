package com.cordacodeclub.directAgreement.api

import com.cordacodeclub.directAgreement.flow.LegalAgreementFlow.LegalAgreementFlowInitiator
import com.cordacodeclub.directAgreement.schema.LegalAgreementSchemaV1
import com.cordacodeclub.directAgreement.state.LegalAgreementState
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED
val SERVICE_NAMES = listOf("oracle", "Network Map Service")

@Path("agreement")
class AgreementApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<AgreementApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, oracle and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all LegalAgreement states that exist in the node's vault.
     */
    @GET
    @Path("legalAgreements")
    @Produces(MediaType.APPLICATION_JSON)
    fun getLegalAgreements() = rpcOps.vaultQueryBy<LegalAgreementState>().states

    /**
     * Initiates a flow to agree a LegalAgreement between PartyA and Intermediary.
     *
     * Once the flow finishes it will have written the LegalAgreement to ledger. Both the PartyA and the Intermediary will be able to
     * see it when calling /api/directAgreement/legalAgreements on their respective nodes.
     *
     * This end-point takes PartyA's name, PartyB's name and Oracle's name as part of the path. If the serving node can't find the other parties
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    //Do we need an endpoint for every LegalAgreement state? (INTERMEDIATE/DIRECT/COMPLETED)
//    @PUT
//    @Path("create-legalAgreement")
//    fun createLegalAgreement(@QueryParam("value") value: Amount<Currency>,
//                             @QueryParam("partyA") partyA: CordaX500Name?,
//                             @QueryParam("partyB") partyB: CordaX500Name?,
//                             @QueryParam("oracle") oracle: CordaX500Name?): Response {
//        if (value.quantity <= 0 ) {
//            return Response.status(BAD_REQUEST).entity("Query parameter 'value' must be non-negative.\n").build()
//        }
//        if (partyA == null || partyB == null ||  oracle== null) {
//            return Response.status(BAD_REQUEST).entity("Query parameter '$partyA'/'$partyB'/'$oracle' missing or has wrong format.\n").build()
//        }
//
//        val partyA = rpcOps.wellKnownPartyFromX500Name(partyA) ?:
//        return Response.status(BAD_REQUEST).entity("Party named $partyA cannot be found.\n").build()
//
//        val partyB = rpcOps.wellKnownPartyFromX500Name(partyB) ?:
//        return Response.status(BAD_REQUEST).entity("Party named $partyB cannot be found.\n").build()
//
//        val oracle = rpcOps.wellKnownPartyFromX500Name(oracle) ?:
//        return Response.status(BAD_REQUEST).entity("Party named $oracle cannot be found.\n").build()
//
//        return try {
//            val signedTx = rpcOps.startTrackedFlow().returnValue.getOrThrow()
//            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()
//
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            Response.status(BAD_REQUEST).entity(ex.message!!).build()
//        }
//    }

    /**
     * Displays all LegalAgreement states that are created by Party.
     */
    @GET
    @Path("my-legalAgreements")
    @Produces(MediaType.APPLICATION_JSON)
    fun myLegalAgreements(): Response {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
        val results = builder {
            var partyType = LegalAgreementSchemaV1.PersistentLegalAgreement::intermediary
                    .equal(rpcOps.nodeInfo().legalIdentities.first().name.toString())
            val customCriteria = QueryCriteria.VaultCustomQueryCriteria(partyType)
            val criteria = generalCriteria.and(customCriteria)
            val results = rpcOps.vaultQueryBy<LegalAgreementState>(criteria).states
            return Response.ok(results).build()
        }
    }
}