package com.axiom.service;

import com.axiom.domain.service.state.TerritoryService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class TerritoryServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void claimAndUnclaimUpdatesIndexes() {
        TerritoryService service = new TerritoryService(Logger.getLogger("test"), null);

        service.claim("n1", "world", 1, 2);
        assertEquals("n1", service.getNationAt("world", 1, 2));
        assertEquals(1, service.getNationClaims("n1").size());
        assertEquals(1, service.getTotalClaimedChunks());

        service.unclaim("n1", "world", 1, 2);
        assertNull(service.getNationAt("world", 1, 2));
        assertTrue(service.getNationClaims("n1").isEmpty());
        assertEquals(0, service.getTotalClaimedChunks());
    }

    @Test
    public void claimReassignsOwnerAndCleansPreviousNation() {
        TerritoryService service = new TerritoryService(Logger.getLogger("test"), null);

        service.claim("n1", "world", 5, 6);
        service.claim("n2", "world", 5, 6);

        assertEquals("n2", service.getNationAt("world", 5, 6));
        assertTrue(service.getNationClaims("n1").isEmpty());
        assertEquals(1, service.getNationClaims("n2").size());

        Set<TerritoryService.TerritorySquare> squares = Set.copyOf(service.getAllSquares());
        assertEquals(1, squares.size());
        assertEquals("n2", squares.iterator().next().getNationId());
    }

    @Test
    public void unclaimDoesNotRemoveOtherOwner() {
        TerritoryService service = new TerritoryService(Logger.getLogger("test"), null);

        service.claim("n1", "world", 3, 4);
        service.claim("n2", "world", 3, 4);
        service.unclaim("n1", "world", 3, 4);

        assertEquals("n2", service.getNationAt("world", 3, 4));
        assertTrue(service.getNationClaims("n1").isEmpty());
        assertEquals(1, service.getNationClaims("n2").size());
    }

    @Test
    public void saveAndLoadPersistsTerritories() throws Exception {
        java.io.File storage = tempFolder.newFile("territories.json");
        TerritoryService service = new TerritoryService(Logger.getLogger("test"), null, storage);
        service.claim("n1", "world", 7, 8);
        service.save();

        TerritoryService reloaded = new TerritoryService(Logger.getLogger("test"), null, storage);
        assertEquals("n1", reloaded.getNationAt("world", 7, 8));
        assertEquals(1, reloaded.getTotalClaimedChunks());
    }

    @Test
    public void deltaReportsChanges() {
        TerritoryService service = new TerritoryService(Logger.getLogger("test"), null);
        long v0 = service.getVersion();

        service.claim("n1", "world", 2, 3);
        TerritoryService.DeltaResult delta = service.getDeltaSince(v0);

        assertFalse(delta.requiresSnapshot());
        assertEquals(1, delta.getChanges().size());
        assertEquals("claim", delta.getChanges().get(0).getOp());
    }
}
