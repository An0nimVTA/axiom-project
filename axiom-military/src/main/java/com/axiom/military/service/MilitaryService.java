package com.axiom.military.service;

import com.axiom.core.EventPublisher;
import com.axiom.military.model.MilitaryData;
import com.axiom.military.repository.MilitaryRepository;

import java.util.Optional;

/**
 * Сервис для работы с военными силами
 */
public class MilitaryService {
    private final MilitaryRepository militaryRepository;
    private final EventPublisher eventPublisher;
    
    public MilitaryService(MilitaryRepository militaryRepository, EventPublisher eventPublisher) {
        this.militaryRepository = militaryRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Получение военных данных нации
     */
    public Optional<MilitaryData> getMilitaryData(String nationId) {
        return militaryRepository.findById(nationId);
    }
    
    /**
     * Получение или создание военных данных для нации
     */
    public MilitaryData getOrCreateMilitaryData(String nationId) {
        return militaryRepository.getOrCreateMilitaryData(nationId);
    }
    
    /**
     * Найм войск
     */
    public boolean recruitUnits(String nationId, String unitType, int count, double costPerUnit) {
        MilitaryData militaryData = getOrCreateMilitaryData(nationId);
        double totalCost = count * costPerUnit;
        
        // В реальном приложении нужно проверить казну нации
        // Здесь упрощенная версия
        
        switch (unitType.toLowerCase()) {
            case "infantry":
                militaryData.incrementInfantry(count);
                break;
            case "cavalry":
                militaryData.incrementCavalry(count);
                break;
            case "artillery":
                militaryData.incrementArtillery(count);
                break;
            case "navy":
                militaryData.incrementNavy(count);
                break;
            case "airforce":
                militaryData.incrementAirForce(count);
                break;
            default:
                return false; // Неверный тип войск
        }
        
        militaryRepository.save(militaryData);
        eventPublisher.publish(new UnitsRecruitedEvent(nationId, unitType, count));
        
        return true;
    }
    
    /**
     * Увольнение войск
     */
    public boolean dismissUnits(String nationId, String unitType, int count) {
        Optional<MilitaryData> militaryDataOpt = militaryRepository.findById(nationId);
        if (!militaryDataOpt.isPresent()) {
            return false;
        }
        
        MilitaryData militaryData = militaryDataOpt.get();
        
        switch (unitType.toLowerCase()) {
            case "infantry":
                militaryData.decrementInfantry(count);
                break;
            case "cavalry":
                militaryData.decrementCavalry(count);
                break;
            case "artillery":
                militaryData.decrementArtillery(count);
                break;
            case "navy":
                militaryData.decrementNavy(count);
                break;
            case "airforce":
                militaryData.decrementAirForce(count);
                break;
            default:
                return false; // Неверный тип войск
        }
        
        militaryRepository.save(militaryData);
        eventPublisher.publish(new UnitsDismissedEvent(nationId, unitType, count));
        
        return true;
    }
    
    /**
     * Получение боевой мощи нации
     */
    public double getMilitaryStrength(String nationId) {
        Optional<MilitaryData> militaryData = militaryRepository.findById(nationId);
        return militaryData.map(MilitaryData::getStrength).orElse(0.0);
    }
    
    /**
     * Получение общего количества войск
     */
    public int getTotalUnits(String nationId) {
        Optional<MilitaryData> militaryData = militaryRepository.findById(nationId);
        return militaryData.map(MilitaryData::getTotalUnits).orElse(0);
    }
    
    /**
     * Расчет стоимости содержания войск
     */
    public double calculateMaintenanceCost(String nationId) {
        Optional<MilitaryData> militaryData = militaryRepository.findById(nationId);
        return militaryData.map(MilitaryData::calculateMaintenanceCost).orElse(0.0);
    }
    
    /**
     * Улучшение войск (увеличение боевой мощи)
     */
    public boolean upgradeUnits(String nationId, String unitType, int count, double cost) {
        // В реальном приложении нужно проверить казну и выполнить улучшение
        // Здесь упрощенная версия
        
        Optional<MilitaryData> militaryDataOpt = militaryRepository.findById(nationId);
        if (!militaryDataOpt.isPresent()) {
            return false;
        }
        
        MilitaryData militaryData = militaryDataOpt.get();
        militaryData.setStrength(militaryData.getStrength() * 1.1); // Увеличение мощи на 10%
        
        militaryRepository.save(militaryData);
        eventPublisher.publish(new UnitsUpgradedEvent(nationId, unitType, count));
        
        return true;
    }
    
    public MilitaryRepository getMilitaryRepository() {
        return militaryRepository;
    }
}

// События для военного сервиса
class UnitsRecruitedEvent {
    public final String nationId;
    public final String unitType;
    public final int count;
    
    public UnitsRecruitedEvent(String nationId, String unitType, int count) {
        this.nationId = nationId;
        this.unitType = unitType;
        this.count = count;
    }
}

class UnitsDismissedEvent {
    public final String nationId;
    public final String unitType;
    public final int count;
    
    public UnitsDismissedEvent(String nationId, String unitType, int count) {
        this.nationId = nationId;
        this.unitType = unitType;
        this.count = count;
    }
}

class UnitsUpgradedEvent {
    public final String nationId;
    public final String unitType;
    public final int count;
    
    public UnitsUpgradedEvent(String nationId, String unitType, int count) {
        this.nationId = nationId;
        this.unitType = unitType;
        this.count = count;
    }
}