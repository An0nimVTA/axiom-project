package com.axiom.economy.service;

import com.axiom.core.EventPublisher;
import com.axiom.economy.model.EconomyData;
import com.axiom.economy.repository.EconomyRepository;

import java.util.Optional;

/**
 * Сервис для работы с экономикой
 */
public class EconomyService {
    private final EconomyRepository economyRepository;
    private final EventPublisher eventPublisher;
    
    public EconomyService(EconomyRepository economyRepository, EventPublisher eventPublisher) {
        this.economyRepository = economyRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Получение экономических данных нации
     */
    public Optional<EconomyData> getEconomyData(String nationId) {
        return economyRepository.findById(nationId);
    }
    
    /**
     * Получение или создание экономических данных для нации
     */
    public EconomyData getOrCreateEconomyData(String nationId) {
        return economyRepository.getOrCreateEconomyData(nationId);
    }
    
    /**
     * Добавление средств в казну нации
     */
    public boolean addToTreasury(String nationId, double amount) {
        EconomyData economyData = getOrCreateEconomyData(nationId);
        economyData.addToTreasury(amount);
        
        economyRepository.save(economyData);
        eventPublisher.publish(new TreasuryUpdatedEvent(nationId, economyData.getTreasury()));
        
        return true;
    }
    
    /**
     * Вычитание средств из казны нации
     */
    public boolean removeFromTreasury(String nationId, double amount) {
        Optional<EconomyData> economyDataOpt = economyRepository.findById(nationId);
        if (economyDataOpt.isPresent()) {
            EconomyData economyData = economyDataOpt.get();
            if (economyData.removeFromTreasury(amount)) {
                economyRepository.save(economyData);
                eventPublisher.publish(new TreasuryUpdatedEvent(nationId, economyData.getTreasury()));
                return true;
            }
        }
        return false;
    }
    
    /**
     * Перевод средств между нациями
     */
    public boolean transferFunds(String fromNationId, String toNationId, double amount) {
        EconomyData fromEconomy = getOrCreateEconomyData(fromNationId);
        EconomyData toEconomy = getOrCreateEconomyData(toNationId);
        
        if (fromEconomy.removeFromTreasury(amount)) {
            toEconomy.addToTreasury(amount);
            
            economyRepository.save(fromEconomy);
            economyRepository.save(toEconomy);
            
            eventPublisher.publish(new FundTransferEvent(fromNationId, toNationId, amount));
            return true;
        }
        
        return false;
    }
    
    /**
     * Обновление ВВП нации
     */
    public void updateGdp(String nationId, double gdpGrowth) {
        EconomyData economyData = getOrCreateEconomyData(nationId);
        economyData.updateGdp(gdpGrowth);
        
        economyRepository.save(economyData);
        eventPublisher.publish(new GdpUpdatedEvent(nationId, economyData.getGdp()));
    }
    
    /**
     * Установка уровня инфляции
     */
    public void setInflationRate(String nationId, double inflationRate) {
        EconomyData economyData = getOrCreateEconomyData(nationId);
        economyData.setInflationRate(inflationRate);
        
        economyRepository.save(economyData);
        eventPublisher.publish(new InflationRateUpdatedEvent(nationId, inflationRate));
    }
    
    /**
     * Установка уровня безработицы
     */
    public void setUnemploymentRate(String nationId, double unemploymentRate) {
        EconomyData economyData = getOrCreateEconomyData(nationId);
        economyData.setUnemploymentRate(unemploymentRate);
        
        economyRepository.save(economyData);
        eventPublisher.publish(new UnemploymentRateUpdatedEvent(nationId, unemploymentRate));
    }
    
    public EconomyRepository getEconomyRepository() {
        return economyRepository;
    }
}

// События для экономического сервиса
class TreasuryUpdatedEvent {
    public final String nationId;
    public final double newAmount;
    
    public TreasuryUpdatedEvent(String nationId, double newAmount) {
        this.nationId = nationId;
        this.newAmount = newAmount;
    }
}

class FundTransferEvent {
    public final String fromNationId;
    public final String toNationId;
    public final double amount;
    
    public FundTransferEvent(String fromNationId, String toNationId, double amount) {
        this.fromNationId = fromNationId;
        this.toNationId = toNationId;
        this.amount = amount;
    }
}

class GdpUpdatedEvent {
    public final String nationId;
    public final double newGdp;
    
    public GdpUpdatedEvent(String nationId, double newGdp) {
        this.nationId = nationId;
        this.newGdp = newGdp;
    }
}

class InflationRateUpdatedEvent {
    public final String nationId;
    public final double newRate;
    
    public InflationRateUpdatedEvent(String nationId, double newRate) {
        this.nationId = nationId;
        this.newRate = newRate;
    }
}

class UnemploymentRateUpdatedEvent {
    public final String nationId;
    public final double newRate;
    
    public UnemploymentRateUpdatedEvent(String nationId, double newRate) {
        this.nationId = nationId;
        this.newRate = newRate;
    }
}