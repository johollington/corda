package net.corda.core.flows

import net.corda.core.getOrThrow
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.VerifiedAnonymousParty
import net.corda.core.identity.Party
import net.corda.testing.ALICE
import net.corda.testing.BOB
import net.corda.testing.DUMMY_NOTARY
import net.corda.testing.node.MockNetwork
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TransactionKeyFlowTests {
    @Test
    fun `issue key`() {
        // We run this in parallel threads to help catch any race conditions that may exist.
        val mockNet = MockNetwork(false, true)

        // Set up values we'll need
        val notaryNode = mockNet.createNotaryNode(null, DUMMY_NOTARY.name)
        val aliceNode = mockNet.createPartyNode(notaryNode.network.myAddress, ALICE.name)
        val bobNode = mockNet.createPartyNode(notaryNode.network.myAddress, BOB.name)
        val alice: Party = aliceNode.services.myInfo.legalIdentity
        val bob: Party = bobNode.services.myInfo.legalIdentity
        aliceNode.services.identityService.registerIdentity(bobNode.info.legalIdentityAndCert)
        aliceNode.services.identityService.registerIdentity(notaryNode.info.legalIdentityAndCert)
        bobNode.services.identityService.registerIdentity(aliceNode.info.legalIdentityAndCert)
        bobNode.services.identityService.registerIdentity(notaryNode.info.legalIdentityAndCert)

        // Run the flows
        val requesterFlow = aliceNode.services.startFlow(TransactionKeyFlow(bob))

        // Get the results
        val actual: Map<Party, VerifiedAnonymousParty> = requesterFlow.resultFuture.getOrThrow().toMap()
        assertEquals(2, actual.size)
        // Verify that the generated anonymous identities do not match the well known identities
        val aliceAnonymousIdentity = actual[alice] ?: throw IllegalStateException()
        val bobAnonymousIdentity = actual[bob] ?: throw IllegalStateException()
        assertNotEquals<AbstractParty>(alice, aliceAnonymousIdentity.party)
        assertNotEquals<AbstractParty>(bob, bobAnonymousIdentity.party)

        // Verify that the anonymous identities look sane
        assertEquals(alice.name, aliceAnonymousIdentity.name)
        assertEquals(bob.name, bobAnonymousIdentity.name)

        // Verify that the nodes have the right anonymous identities
        assertTrue { aliceAnonymousIdentity.party.owningKey in aliceNode.services.keyManagementService.keys }
        assertTrue { bobAnonymousIdentity.party.owningKey in bobNode.services.keyManagementService.keys }
        assertFalse { aliceAnonymousIdentity.party.owningKey in bobNode.services.keyManagementService.keys }
        assertFalse { bobAnonymousIdentity.party.owningKey in aliceNode.services.keyManagementService.keys }

        mockNet.stopNodes()
    }
}
