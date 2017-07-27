package net.corda.contracts

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder

// The dummy contract doesn't do anything useful. It exists for testing purposes.

val ANOTHER_DUMMY_PROGRAM_ID = AnotherDummyContract()

class AnotherDummyContract : Contract, net.corda.core.node.DummyContractBackdoor {
    data class State(val magicNumber: Int = 0) : ContractState {
        override val contract = ANOTHER_DUMMY_PROGRAM_ID
        override val participants: List<AbstractParty>
            get() = emptyList()
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        // Always accepts.
    }

    // The "empty contract"
    override val legalContractReference: SecureHash = SecureHash.sha256("https://anotherdummy.org")

    override fun generateInitial(owner: PartyAndReference, magicNumber: Int, notary: Party): TransactionBuilder {
        val state = State(magicNumber)
        return TransactionBuilder(notary).withItems(state, Command(Commands.Create(), owner.party.owningKey))
    }

    override fun inspectState(state: ContractState): Int = (state as State).magicNumber

}
