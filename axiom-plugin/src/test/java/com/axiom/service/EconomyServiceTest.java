package com.axiom.service;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.industry.EconomyService;
import com.axiom.testsupport.InMemoryNationManager;
import com.axiom.testsupport.TestPluginFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

public class EconomyServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void printMoneyValidatesRoleAndLimit() throws Exception {
        File data = tempFolder.newFolder("plugin");
        AXIOM plugin = TestPluginFactory.createPlugin(data);
        plugin.getConfig().set("economy.maxPrintAmountPerCommand", 1000.0);

        InMemoryNationManager nationManager = new InMemoryNationManager(plugin);
        UUID leader = UUID.randomUUID();
        Nation nation = new Nation("n1", "Nation One", leader, "AXC", 1000.0);
        UUID citizen = UUID.randomUUID();
        nation.getCitizens().add(citizen);
        nation.getRoles().put(citizen, Nation.Role.CITIZEN);
        nationManager.addNation(nation);

        EconomyService economy = new EconomyService(plugin, nationManager);

        assertFalse(economy.printMoney(citizen, 100.0));
        assertEquals(1000.0, nation.getTreasury(), 0.001);

        assertTrue(economy.printMoney(leader, 200.0));
        assertEquals(1200.0, nation.getTreasury(), 0.001);

        assertFalse(economy.printMoney(leader, 2000.0));
        assertEquals(1200.0, nation.getTreasury(), 0.001);
    }

    @Test
    public void recordTransactionUpdatesGdp() throws Exception {
        File data = tempFolder.newFolder("plugin2");
        AXIOM plugin = TestPluginFactory.createPlugin(data);

        InMemoryNationManager nationManager = new InMemoryNationManager(plugin);
        UUID leader = UUID.randomUUID();
        Nation nation = new Nation("n1", "Nation One", leader, "AXC", 5000.0);
        nationManager.addNation(nation);

        EconomyService economy = new EconomyService(plugin, nationManager);

        economy.recordTransaction("n1", 1000.0);
        assertEquals(1000.0, economy.getGDP("n1"), 0.001);

        double health = economy.getEconomicHealth("n1");
        assertTrue(health > 0.0);
    }
}
