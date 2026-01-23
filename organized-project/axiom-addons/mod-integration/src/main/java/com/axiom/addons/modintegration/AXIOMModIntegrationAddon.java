package com.axiom.addons.modintegration;

import com.axiom.addons.modintegration.service.economic.EconomicIndicatorsService;
import com.axiom.addons.modintegration.service.diplomatic.DiplomaticIntegrationService;
import com.axiom.addons.modintegration.service.propaganda.PropagandaService;
import com.axiom.addons.modintegration.service.migration.MigrationService;
import com.axiom.addons.modintegration.service.disaster.DisasterService;
import com.axiom.addons.modintegration.service.cybersecurity.CyberSecurityService;
import com.axiom.addons.modintegration.service.espionage.SpyService;
import com.axiom.addons.modintegration.service.funding.CrowdFundingService;
import com.axiom.api.ModIntegrationAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for AXIOM Mod Integration Addon
 * Extends AXIOM functionality with deep mod integration
 */
public class AXIOMModIntegrationAddon extends JavaPlugin {
    
    private static AXIOMModIntegrationAddon instance;
    
    // Service instances
    private EconomicIndicatorsService economicIndicatorsService;
    private DiplomaticIntegrationService diplomaticIntegrationService;
    private PropagandaService propagandaService;
    private MigrationService migrationService;
    private DisasterService disasterService;
    private CyberSecurityService cyberSecurityService;
    private SpyService spyService;
    private CrowdFundingService crowdFundingService;
    private ModIntegrationAPI modIntegrationAPI;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default configuration
        saveDefaultConfig();
        
        // Initialize services
        initializeServices();
        
        // Register events and commands
        registerComponents();
        
        getLogger().info("AXIOM Mod Integration Addon enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Clean up services
        cleanupServices();
        
        getLogger().info("AXIOM Mod Integration Addon disabled.");
    }
    
    private void initializeServices() {
        try {
            // Initialize the mod integration API (this would connect to the main AXIOM plugin)
            modIntegrationAPI = new ModIntegrationAPI(this);
            
            // Initialize economic service with mod integration
            economicIndicatorsService = new EconomicIndicatorsService(this);
            
            // Initialize diplomatic service with mod detection
            diplomaticIntegrationService = new DiplomaticIntegrationService(this);
            
            // Initialize propaganda service using mod items
            propagandaService = new PropagandaService(this);
            
            // Initialize migration service with mod entities
            migrationService = new MigrationService(this);
            
            // Initialize disaster service with mod events
            disasterService = new DisasterService(this);
            
            // Initialize cyber security with computer mods
            cyberSecurityService = new CyberSecurityService(this);
            
            // Initialize spy service with mod-based stealth
            spyService = new SpyService(this);
            
            // Initialize crowd funding with mod resources
            crowdFundingService = new CrowdFundingService(this);
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize services: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void registerComponents() {
        // Register event listeners, commands, and other components
        // This would include registering with the main AXIOM plugin
    }
    
    private void cleanupServices() {
        // Perform cleanup of services when disabling
    }
    
    // Getters for services
    public EconomicIndicatorsService getEconomicIndicatorsService() {
        return economicIndicatorsService;
    }
    
    public DiplomaticIntegrationService getDiplomaticIntegrationService() {
        return diplomaticIntegrationService;
    }
    
    public PropagandaService getPropagandaService() {
        return propagandaService;
    }
    
    public MigrationService getMigrationService() {
        return migrationService;
    }
    
    public DisasterService getDisasterService() {
        return disasterService;
    }
    
    public CyberSecurityService getCyberSecurityService() {
        return cyberSecurityService;
    }
    
    public SpyService getSpyService() {
        return spyService;
    }
    
    public CrowdFundingService getCrowdFundingService() {
        return crowdFundingService;
    }
    
    public ModIntegrationAPI getModIntegrationAPI() {
        return modIntegrationAPI;
    }
    
    public static AXIOMModIntegrationAddon getInstance() {
        return instance;
    }
}