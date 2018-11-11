package com.cordacodeclub.directAgreement.plugin

import com.cordacodeclub.directAgreement.api.AgreementApi
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class AgreementPlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis = listOf(Function(::AgreementApi))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    override val staticServeDirs = mapOf(
            // This will serve the agreementWeb directory in resources to /web/agreement
            "agreement" to javaClass.classLoader.getResource("agreementWeb").toExternalForm()
    )
}