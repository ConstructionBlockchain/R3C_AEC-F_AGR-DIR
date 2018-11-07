package com.cordacodeclub.directAgreement.plugin

import com.cordacodeclub.directAgreement.api.AgreementApi
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class AgreementPlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis = listOf(Function(::AgreementApi))
}