# Direct Agreement Module (R3C_AEC-F_AGR-DIR)

## What are Direct Agreements?

A direct agreement allows a lender to deal directly with a contractor in the case that the special-purpose vehicle (SPV) who is funding the project defaults.

## Links
* http://wiki.designcomputation.org/home/index.php/Project_Finance#Security_Documents - More general discription there

## Example actions:

### Create LegalAgreement

```yaml
# From PartyC
flow start com.cordacodeclub.directAgreement.flow.LegalAgreementFlow$LegalAgreementFlowInitiator agreementValue: "20 USD", partyA: "O=PartyA,L=London,C=GB", partyB: "O=PartyB,L=New York,C=US", oracle: "O=PartyC,L=Paris,C=FR"
```

### Get status

```yaml
run vaultQuery contractStateType: com.cordacodeclub.directAgreement.state.LegalAgreementState
```

### Mark PartyC as bust

```yaml
# From PartyC
flow start com.cordacodeclub.directAgreement.flow.BustPartyOracleFlow$SetBustPartyInitiator bustParty: "O=PartyC,L=Paris,C=FR", isBust: true
```

### Go to direct agreement

```yaml
# From PartyB
flow start com.cordacodeclub.directAgreement.flow.DirectAgreementFlow$DirectAgreementFlowInitiator txhash: "9309801D9A84D4A1D2EED84E4AC553B3427EBE10195772B678C0ECB1593AC263", index: 0
```

### Complete agreement

```yaml
# From allowed part
flow start com.cordacodeclub.directAgreement.flow.EndAgreementFlow$EndAgreementFlowInitiator txhash: "2A7C7A70B7EBF9D4915BD83828C391586F6582E5E2448214ECA1483ACDEF56E7", index: 0
```