# Reproduce

```sh
./gradlew deployNodes
./build/nodes/runnodes
```
Then in `PartyC`'s shell, run:

```yaml
flow start com.cordacodeclub.directAgreement.flow.LegalAgreementFlow$LegalAgreementFlowInitiator agreementValue: "20 USD", partyA: "O=PartyA,L=London,C=GB", partyB: "O=PartyB,L=New York,C=US", oracle: "O=PartyC,L=Paris,C=FR"
```
Take the tx hash, eg `587E5840B7A6C546A353612B227ADC76D03EF47B590FCE6B00A72825F7FFDC3B`, then using it, in the same `PartyC`'s shell:

```yaml
flow start com.cordacodeclub.directAgreement.flow.DirectAgreementFlow$DirectAgreementFlowInitiator txhash: "768526FDEDA4A15D814A8F60851C87254554EAC035D974D799E0030D1EF867E8", index: 0
```
You should get something like:

```
✅   Querying the BustPartyOracl
✅   Querying the BustPartyOracle.
    ✅   Sending and receiving partly infornation request to BustPartyOracle.
✅   Generating transaction based on new IOU.
✅   Verifying contract constraints.
✅   Signing transaction with our private key.
✅   Gathering the oracle's signature.
    ✅   Sending and receiving partly signed transaction to BustPartyOracle.
✅   Gathering the counterparty's signature.
    ✅   Collecting signatures from counterparties.
    ✅   Verifying collected signatures.
✅   Obtaining notary signature and recording transaction.
    ✅   Requesting signature by notary service
        ✅   Requesting signature by Notary service
        ✅   Validating response from Notary service
    ✅   Broadcasting transaction to participants
✅   Done
☠   Unexpected party
```
Notice how all is green but at the end it says `Unexpected party`, which is the expected exception, as found in `DirectAgreementFlow.kt:122`.
The problem is that all is green despite the error.