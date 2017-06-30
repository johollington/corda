package net.corda.node.services.transactions

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.TrustedAuthorityNotaryService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap
import java.security.SignatureException

/**
 * A notary commit flow that makes sure a given transaction is valid before committing it. This does mean that the calling
 * party has to reveal the whole transaction history; however, we avoid complex conflict resolution logic where a party
 * has its input states "blocked" by a transaction from another party, and needs to establish whether that transaction was
 * indeed valid.
 */
class ValidatingNotaryFlow(otherSide: Party, service: TrustedAuthorityNotaryService) : NotaryFlow.Service(otherSide, service) {
    /**
     * The received transaction is checked for contract-validity, which requires fully resolving it into a
     * [TransactionForVerification], for which the caller also has to to reveal the whole transaction
     * dependency chain.
     */
    @Suspendable
    override fun receiveAndVerifyTx(): TransactionParts {
        try {
            return subFlow(ReceiveTransactionFlow(SignedTransaction::class.java, otherSide, verifySignatures = false)).unwrap {
                it.verifySignaturesExcept(serviceHub.myInfo.notaryIdentity.owningKey)
                val wtx = it.tx
                wtx.toLedgerTransaction(serviceHub).verify()
                TransactionParts(wtx.id, wtx.inputs, wtx.timeWindow)
            }
        } catch(e: TransactionVerificationException) {
            throw NotaryException(NotaryError.TransactionInvalid(e))
        } catch (e: SignatureException) {
            throw NotaryException(NotaryError.TransactionInvalid(e))
        }
    }
}
