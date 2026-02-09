package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.politics.DiplomacyRelationService;
import com.axiom.testsupport.InMemoryNationManager;
import com.axiom.testsupport.TestPluginFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

public class DiplomacyRelationServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void allianceAndNeutralTransitionsUpdateStatus() throws Exception {
        File data = tempFolder.newFolder("plugin");
        AXIOM plugin = TestPluginFactory.createPlugin(data);
        InMemoryNationManager nationManager = new InMemoryNationManager(plugin);

        Nation a = new Nation("n1", "Nation One", UUID.randomUUID(), "AXC", 10000.0);
        Nation b = new Nation("n2", "Nation Two", UUID.randomUUID(), "AXC", 10000.0);
        nationManager.addNation(a);
        nationManager.addNation(b);

        DiplomacyRelationService service = new DiplomacyRelationService(plugin, nationManager);

        String err = service.setStatus("n1", "n2", DiplomacyRelationService.RelationStatus.ALLIANCE, 0, "test");
        assertNull(err);
        assertEquals(DiplomacyRelationService.RelationStatus.ALLIANCE, service.getStatus("n1", "n2"));
        assertTrue(a.getAllies().contains("n2"));
        assertTrue(b.getAllies().contains("n1"));

        String warErr = service.setStatus("n1", "n2", DiplomacyRelationService.RelationStatus.WAR, 3600_000L, "test");
        assertEquals("Нельзя объявить войну союзнику. Расторгните альянс.", warErr);

        String neutralErr = service.setStatus("n1", "n2", DiplomacyRelationService.RelationStatus.NEUTRAL, 0, "test");
        assertNull(neutralErr);
        assertEquals(DiplomacyRelationService.RelationStatus.NEUTRAL, service.getStatus("n1", "n2"));
        assertFalse(a.getAllies().contains("n2"));
        assertFalse(b.getAllies().contains("n1"));
    }

    @Test
    public void sanctionsBlockAlliance() throws Exception {
        File data = tempFolder.newFolder("plugin2");
        AXIOM plugin = TestPluginFactory.createPlugin(data);
        InMemoryNationManager nationManager = new InMemoryNationManager(plugin);

        Nation a = new Nation("n1", "Nation One", UUID.randomUUID(), "AXC", 10000.0);
        Nation b = new Nation("n2", "Nation Two", UUID.randomUUID(), "AXC", 10000.0);
        nationManager.addNation(a);
        nationManager.addNation(b);

        DiplomacyRelationService service = new DiplomacyRelationService(plugin, nationManager);

        String sanctionErr = service.imposeSanction("n1", "n2", 3600_000L, "test");
        assertNull(sanctionErr);

        String allianceErr = service.setStatus("n1", "n2", DiplomacyRelationService.RelationStatus.ALLIANCE, 0, "test");
        assertEquals("Нельзя заключить альянс при активных санкциях.", allianceErr);
    }
}
