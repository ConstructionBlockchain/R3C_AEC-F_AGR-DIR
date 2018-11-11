# Direct Agreement Module (R3C_AEC-F_AGR-DIR)

## What are Direct Agreements?

A direct agreement allows a lender to deal directly with a contractor in the case that the special-purpose vehicle (SPV) who is funding the project defaults.

## Links
* http://wiki.designcomputation.org/home/index.php/Project_Finance#Security_Documents - More general discription there

## Example actions:

### Create LegalAgreement

In node shell:

```yaml
# From Intermediary
flow start com.cordacodeclub.directAgreement.flow.LegalAgreementFlow$LegalAgreementFlowInitiator agreementValue: "20 USD", partyA: "O=Contractor,L=London,C=GB", partyB: "O=Lender,L=New York,C=US", oracle: "O=Oracle,L=Hamburg,C=DE"
```

With API:

```bash
curl -X PUT 'http://localhost:10015/api/agreement/create-legal-agreement?quantity=20&currency=USD&partyA=O=Contractor,L=London,C=GB&partyB=O=Lender,L=New%20York,C=US&oracle=O=Oracle,L=Hamburg,C=DE'
```

### Get status

In node shell:

```yaml
run vaultQuery contractStateType: com.cordacodeclub.directAgreement.state.LegalAgreementState
```

### Mark PartyC as bust

```yaml
# From Oracle
flow start com.cordacodeclub.directAgreement.flow.BustPartyOracleFlow$SetBustPartyInitiator bustParty: "O=Intermediary,L=Paris,C=FR", isBust: true
```

With API:

```bash
curl -X PUT 'http://localhost:10018/api/agreement/set-party-bust?party=O=Intermediary,L=Paris,C=FR&isBust=true'
```

### Go to direct agreement

```yaml
# From Contractor or Lender
flow start com.cordacodeclub.directAgreement.flow.DirectAgreementFlow$DirectAgreementFlowInitiator txhash: "9309801D9A84D4A1D2EED84E4AC553B3427EBE10195772B678C0ECB1593AC263", index: 0
```

### Complete agreement

```yaml
# From any allowed Intermediary / Contractor or Contractor / Lender
flow start com.cordacodeclub.directAgreement.flow.EndAgreementFlow$EndAgreementFlowInitiator txhash: "2A7C7A70B7EBF9D4915BD83828C391586F6582E5E2448214ECA1483ACDEF56E7", index: 0
```

## Troubleshoot
If `runnodes` gives problems:

```bash
# Notary
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Notary" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Notary-corda.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7005,logHandlerClass=net.corda.node.JolokiaSlf4Adapter" "-jar" "corda.jar" && exit'

# Contractor
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Contractor" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Contractor-corda.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7006,logHandlerClass=net.corda.node.JolokiaSlf4Adapter" "-jar" "corda.jar" && exit'

# Contractor web server
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Contractor" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Contractor-corda-webserver.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5007 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7007,logHandlerClass=net.corda.webserver.JolokiaSlf4Adapter" "-jar" "corda-webserver.jar" && exit'

# Lender
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Lender" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Lender-corda.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5008 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7008,logHandlerClass=net.corda.node.JolokiaSlf4Adapter" "-jar" "corda.jar" && exit'

# Lender web server
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Lender" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Lender-corda-webserver.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5009 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7009,logHandlerClass=net.corda.webserver.JolokiaSlf4Adapter" "-jar" "corda-webserver.jar" && exit'

# Intermediary
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Intermediary" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Intermediary-corda.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5010 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7010,logHandlerClass=net.corda.node.JolokiaSlf4Adapter" "-jar" "corda.jar" && exit'

# Intermediary web server
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Intermediary" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Intermediary-corda-webserver.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5011 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7011,logHandlerClass=net.corda.webserver.JolokiaSlf4Adapter" "-jar" "corda-webserver.jar" && exit'

# Oracle
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Oracle" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Oracle-corda.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5012 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7012,logHandlerClass=net.corda.node.JolokiaSlf4Adapter" "-jar" "corda.jar" && exit'

# Oracle web server
bash -c 'cd "/Users/xavier/DAPPS/Corda/R3C_AEC-F_AGR-DIR/build/nodes/Oracle" ; "/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre/bin/java" "-Dname=Oracle-corda-webserver.jar" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5013 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7013,logHandlerClass=net.corda.webserver.JolokiaSlf4Adapter" "-jar" "corda-webserver.jar" && exit'
```