package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.military.AdvancedWarSystem;
import com.axiom.domain.service.politics.DiplomacySystem;
import com.axiom.domain.service.state.NationManager;
import com.axiom.testsupport.InMemoryNationManager;
import com.axiom.testsupport.TestPluginFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

public class AdvancedWarSystemTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void registerWarAndCeasefireFlow() throws Exception {
        File data = tempFolder.newFolder("plugin");
        AXIOM plugin = TestPluginFactory.createPlugin(data);
        InMemoryNationManager nationManager = new InMemoryNationManager(plugin);

        Nation a = new Nation("n1", "Nation One", UUID.randomUUID(), "AXC", 20000.0);
        Nation b = new Nation("n2", "Nation Two", UUID.randomUUID(), "AXC", 20000.0);
        nationManager.addNation(a);
        nationManager.addNation(b);

        StubDiplomacySystem diplomacySystem = new StubDiplomacySystem(plugin, nationManager);

        AdvancedWarSystem warSystem = new AdvancedWarSystem(
            plugin,
            nationManager,
            diplomacySystem,
            null,
            null,
            null,
            null
        );

        warSystem.registerDiplomaticWar("n1", "n2");
        AdvancedWarSystem.War war = warSystem.getActiveWar("n1", "n2");
        assertNotNull(war);
        assertEquals(AdvancedWarSystem.WarStage.PREPARATION, war.stage);
        assertEquals(AdvancedWarSystem.WarStatus.DECLARED, war.status);
        assertFalse(war.fronts.isEmpty());

        warSystem.enterCeasefire("n1", "n2", 3600_000L);
        war = warSystem.getActiveWar("n1", "n2");
        assertNotNull(war);
        assertEquals(AdvancedWarSystem.WarStage.CEASEFIRE, war.stage);
        assertEquals(AdvancedWarSystem.WarStatus.NEAR_PEACE, war.status);
        assertTrue(war.ceasefireEndsAt > System.currentTimeMillis());

        warSystem.endWarByNations("n1", "n2");
        assertNull(warSystem.getActiveWar("n1", "n2"));
        assertTrue(diplomacySystem.peaceCalled);
    }

    private static final class StubDiplomacySystem extends DiplomacySystem {
        private boolean peaceCalled;

        private StubDiplomacySystem(AXIOM plugin, NationManager nationManager) {
            super(plugin, nationManager, null);
        }

        @Override
        public synchronized void declarePeace(String nationA, String nationB) {
            peaceCalled = true;
        }
    }
}
