package com.axiom.service.adapter;

import com.axiom.AXIOM;
import com.axiom.model.Nation;
import com.axiom.service.EconomyService;
import com.axiom.service.EconomyServiceInterface;
import com.axiom.service.NationManager;
import com.axiom.service.WalletService;
import java.util.UUID;

/**
 * Адаптер для EconomyService
 * Позволяет использовать существующую реализацию через новый интерфейс
 */
public class EconomyServiceAdapter implements EconomyServiceInterface {
    
    private final AXIOM plugin;
    private final EconomyService legacyService;
    
    /**
     * Создать адаптер для существующего EconomyService
     * @param plugin основной плагин
     */
    public EconomyServiceAdapter(AXIOM plugin) {
        this(plugin, plugin.getEconomyService() != null
            ? plugin.getEconomyService()
            : new EconomyService(plugin, plugin.getNationManager()));
    }
    
    /**
     * Создать адаптер для существующего экземпляра
     * @param legacyService существующий сервис
     */
    public EconomyServiceAdapter(AXIOM plugin, EconomyService legacyService) {
        this.plugin = plugin;
        this.legacyService = legacyService;
    }
    
    @Override
    public double getBalance(UUID playerId) {
        WalletService walletService = plugin.getWalletService();
        return walletService != null ? walletService.getBalance(playerId) : 0.0;
    }
    
    @Override
    public void addBalance(UUID playerId, double amount) {
        WalletService walletService = plugin.getWalletService();
        if (walletService != null) {
            walletService.deposit(playerId, amount);
        }
    }
    
    @Override
    public boolean transfer(UUID fromId, UUID toId, double amount) {
        WalletService walletService = plugin.getWalletService();
        return walletService != null && walletService.transfer(fromId, toId, amount);
    }
    
    @Override
    public boolean printMoney(UUID actor, double amount) {
        return legacyService.printMoney(actor, amount);
    }
    
    @Override
    public double applyIncomeTaxes(UUID playerId, double grossAmount) {
        return legacyService.applyIncomeTaxes(playerId, grossAmount);
    }
    
    @Override
    public String transferFunds(String fromNationId, String toNationId, double amount, String reason) throws Exception {
        return legacyService.transferFunds(fromNationId, toNationId, amount, reason);
    }
    
    @Override
    public double applySalesTax(String nationId, double transactionAmount) {
        return legacyService.applySalesTax(nationId, transactionAmount);
    }
    
    @Override
    public double getGDP(String nationId) {
        return legacyService.getGDP(nationId);
    }
    
    @Override
    public double getEconomicHealth(String nationId) {
        return legacyService.getEconomicHealth(nationId);
    }
    
    @Override
    public String getNationCurrency(String nationId) {
        NationManager nationManager = plugin.getNationManager();
        if (nationManager != null) {
            Nation nation = nationManager.getNationById(nationId);
            if (nation != null && nation.getCurrencyCode() != null) {
                return nation.getCurrencyCode();
            }
        }
        return legacyService.getDefaultCurrencyCode();
    }
    
    /**
     * Получить доступ к оригинальному сервису
     * @return оригинальный EconomyService
     */
    public EconomyService getLegacyService() {
        return legacyService;
    }
}
