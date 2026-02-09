package com.axiom.domain.service.infrastructure;

import com.axiom.domain.service.state.TerritoryService;
import org.junit.Test;

import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class MapBoundaryServiceTest {

    @Test
    public void resolveNationChunkKeysUsesTerritoryService() {
        TerritoryService territoryService = new TerritoryService(Logger.getLogger("test"), null);
        territoryService.claim("n1", "world", 10, 11);

        Set<String> keys = MapBoundaryService.resolveNationChunkKeys(null, territoryService, "n1");
        assertEquals(1, keys.size());
        assertTrue(keys.contains("world:10:11"));
    }
}
