package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.technology.TechnologyTreeService;
import com.axiom.testsupport.InMemoryNationManager;
import com.axiom.testsupport.TestPluginFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

public class TechnologyTreeServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void researchRequiresPrerequisitesAndFunds() throws Exception {
        File data = tempFolder.newFolder("plugin");
        AXIOM plugin = TestPluginFactory.createPlugin(data);
        InMemoryNationManager nationManager = new InMemoryNationManager(plugin);

        UUID leader = UUID.randomUUID();
        Nation nation = new Nation("n1", "Nation One", leader, "AXC", 50000.0);
        nationManager.addNation(nation);
        TestPluginFactory.setField(plugin, "nationManager", nationManager);

        TechnologyTreeService techService = new TechnologyTreeService(plugin);

        TechnologyTreeService.ResearchStatus status = techService.getResearchStatus("n1", "banking");
        assertFalse(status.prerequisitesMet);
        assertTrue(status.missingPrerequisites.contains("basic_currency"));

        String prereqMsg = techService.researchTechnology("n1", "banking");
        assertTrue(prereqMsg.contains("Не выполнены предварительные условия"));

        String basicMsg = techService.researchTechnology("n1", "basic_currency");
        assertTrue(basicMsg.contains("Технология изучена"));
        assertTrue(techService.isTechnologyUnlocked("n1", "basic_currency"));

        String bankingMsg = techService.researchTechnology("n1", "banking");
        assertTrue(bankingMsg.contains("Технология изучена"));
        assertTrue(techService.isTechnologyUnlocked("n1", "banking"));

        assertEquals(TechnologyTreeService.ProgressStage.EARLY, techService.getNationStage("n1"));
    }
}
