//import com.cordacodeclub.directAgreement.DirectAgreementContract
//import com.cordacodeclub.directAgreement.DirectAgreementContract.Companion.ID
//import com.cordacodeclub.directAgreement.LegalAgreementState
//import net.corda.core.contracts.Amount
//import net.corda.core.identity.CordaX500Name
//import org.junit.Test
//import net.corda.testing.core.TestIdentity
//import net.corda.testing.node.MockServices
//import net.corda.testing.node.ledger
//import java.lang.Compiler.command
//import java.util.*
//
//class DirectAgreementContractTest {
//    private val ledgerServices = MockServices()
//    private val testSpv = TestIdentity(CordaX500Name("TestSpv", "London", "GB"))
//    private val testContractor = TestIdentity(CordaX500Name("TestContractor", "London", "GB"))
//    private val testLender = TestIdentity(CordaX500Name("TestLender", "London", "GB"))
//
//    @Test
//    fun `transaction must include CreateSpv command`() {
//        val directAgreement = 1
//        ledgerServices.ledger {
//            transaction {
//                output(ID, LegalAgreementState(testSpv, testContractor, testLender,
//                        LegalAgreementState.Status.VIASPV, Amount(10, Currency.getInstance("GBP"))))
//                fails()
//                command(listOf(testSpv.publicKey, testContractor.publicKey), DirectAgreementContract.Commands.CreateViaSpv())
//                verifies()
//            }
//        }
//    }
//}