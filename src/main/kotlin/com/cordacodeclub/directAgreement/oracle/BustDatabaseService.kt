package com.cordacodeclub.directAgreement.oracle

import com.cordacodeclub.directAgreement.db.DatabaseService
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import java.sql.SQLException

/**
 * A database service subclass for handling a table of bust parties.
 *
 * @param services The node's service hub.
 */
@CordaService
class BustDatabaseService(services: ServiceHub) : DatabaseService(services) {
    init {
        setUpStorage()
    }

    companion object {
        val tableName = "IS_BUST"
    }

    /**
     * Updates or adds a party and associated isBust to the table of bust parties.
     * Did not find a way to use "begin transaction" with H2.
     */
    fun setIsBust(party: String, isBust: Boolean) {
        if (updateIsBust(party, isBust) == 0 && addBustParty(party, isBust) != 1) {
            throw SQLException("Failed to set is Bust for $party")
        }
        log.info("Party $party added to $tableName table.")
    }

    /**
     * Adds a party and associated isBust to the table of bust parties.
     */
    fun addBustParty(party: String, isBust: Boolean): Int {
        val query = "insert into ${tableName} values(?, ?)"

        val params = mapOf(1 to party, 2 to isBust)

        val rowCount = executeUpdate(query, params)
        log.info("Party $party added to $tableName table.")
        return rowCount
    }

    /**
     * Updates the isBust of a crypto party in the table of bust parties.
     */
    fun updateIsBust(party: String, isBust: Boolean): Int {
        val query = "update $tableName set isBust = ? where party = ?"

        val params = mapOf(1 to isBust, 2 to party)

        val rowCount = executeUpdate(query, params)
        log.info("Party $party updated in $tableName table.")
        return rowCount
    }

    /**
     * Retrieves the bust status of a party in the table of bust parties.
     */
    fun queryIsBust(party: String): Boolean {
        val query = "select isBust from $tableName where party = ?"

        val params = mapOf(1 to party)

        val results = executeQuery(query, params, { it -> it.getBoolean("isBust") })

        val isBust = when (results.isEmpty()) {
            true -> false
            false -> results.single()
        }
        log.info("Party $party read from $tableName table.")
        return isBust
    }

    /**
     * Initialises the table of bust parties.
     */
    private fun setUpStorage() {
        val query = """
            create table if not exists $tableName(
                party varchar(64) not null,
                isBust boolean
            );
            alter table $tableName add primary key (party)"""

        executeUpdate(query, emptyMap())
        log.info("Created $tableName table.")
    }
}