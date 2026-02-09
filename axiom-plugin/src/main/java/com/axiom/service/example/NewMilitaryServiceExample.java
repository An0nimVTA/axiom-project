package com.axiom.service.example;

import com.axiom.domain.service.military.MilitaryServiceInterface;
import com.axiom.domain.repo.NationRepository;
import com.axiom.domain.repo.MilitaryRepository;
import com.axiom.domain.model.Nation;
import com.axiom.domain.model.MilitaryData;
import com.axiom.exception.MilitaryException;

import java.util.HashMap;
import java.util.Map;

/**
 * Пример новой реализации MilitaryService
 * Эта реализация показывает, как будет выглядеть сервис в новой архитектуре
 */
public class NewMilitaryServiceExample implements MilitaryServiceInterface {
    
    private final NationRepository nationRepository;
    private final MilitaryRepository militaryRepository;
    
    public NewMilitaryServiceExample(NationRepository nationRepository, MilitaryRepository militaryRepository) {
        this.nationRepository = nationRepository;
        this.militaryRepository = militaryRepository;
    }
    
    @Override
    public String recruitUnits(String nationId, String unitType, int count, double cost) {
        // 1. Проверка существования нации
        Nation nation = nationRepository.findById(nationId)
            .orElseThrow(() -> new MilitaryException("Nation not found"));
        
        // 2. Проверка бюджета
        if (nation.getTreasury() < cost * count) {
            throw new MilitaryException("Insufficient funds");
        }
        
        // 3. Получение или создание военных данных
        MilitaryData militaryData = militaryRepository.findByNationId(nationId)
            .orElse(new MilitaryData(nationId));
        
        // 4. Наем войск
        switch (unitType.toLowerCase()) {
            case "infantry": militaryData.incrementInfantry(count); break;
            case "cavalry": militaryData.incrementCavalry(count); break;
            case "artillery": militaryData.incrementArtillery(count); break;
            case "navy": militaryData.incrementNavy(count); break;
            case "airforce": militaryData.incrementAirForce(count); break;
            default: throw new MilitaryException("Unknown unit type");
        }
        
        // 5. Списание средств
        nation.setTreasury(nation.getTreasury() - cost * count);
        nationRepository.save(nation);
        
        // 6. Сохранение военных данных
        militaryRepository.save(militaryData);
        
        // 7. Пересчет боевой мощи
        updateMilitaryStrength(nationId);
        
        return "Recruited: " + count + " " + unitType;
    }
    
    private void updateMilitaryStrength(String nationId) {
        MilitaryData data = militaryRepository.findByNationId(nationId)
            .orElse(new MilitaryData(nationId));
        
        // Базовый расчет мощи
        double baseStrength = data.getInfantry() * 1.0 +
                            data.getCavalry() * 1.5 +
                            data.getArtillery() * 2.0 +
                            data.getNavy() * 2.5 +
                            data.getAirForce() * 3.0;
        
        // Применение бонусов
        double finalStrength = applyBonuses(nationId, baseStrength);
        
        data.setStrength(finalStrength);
        militaryRepository.save(data);
    }
    
    private double applyBonuses(String nationId, double baseStrength) {
        // Здесь будет логика применения бонусов от технологий и модов
        return baseStrength;
    }
    
    @Override
    public double getMilitaryStrength(String nationId) {
        return militaryRepository.findByNationId(nationId)
            .map(MilitaryData::getStrength)
            .orElse(0.0);
    }
    
    @Override
    public Map<String, Object> getMilitaryStatistics(String nationId) {
        MilitaryData data = militaryRepository.findByNationId(nationId)
            .orElse(new MilitaryData(nationId));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUnits", data.getTotalUnits());
        stats.put("strength", data.getStrength());
        stats.put("infantry", data.getInfantry());
        stats.put("cavalry", data.getCavalry());
        stats.put("artillery", data.getArtillery());
        stats.put("navy", data.getNavy());
        stats.put("airForce", data.getAirForce());
        
        return stats;
    }
    
    @Override
    public double calculateMaintenanceCost(String nationId) {
        return militaryRepository.findByNationId(nationId)
            .map(MilitaryData::calculateMaintenanceCost)
            .orElse(0.0);
    }
    
    @Override
    public int getMilitaryCapacity(String nationId) {
        Nation nation = nationRepository.findById(nationId)
            .orElseThrow(() -> new MilitaryException("Nation not found"));
        
        return nation.getCitizens().size() * 100;
    }
    
    @Override
    public boolean canRecruitMore(String nationId, int additionalUnits) {
        MilitaryData data = militaryRepository.findByNationId(nationId)
            .orElse(new MilitaryData(nationId));
        
        return (data.getTotalUnits() + additionalUnits) <= getMilitaryCapacity(nationId);
    }
    
    @Override
    public String disbandUnits(String nationId, String unitType, int count) throws Exception {
        Nation nation = nationRepository.findById(nationId)
            .orElseThrow(() -> new MilitaryException("Nation not found"));
        
        MilitaryData data = militaryRepository.findByNationId(nationId)
            .orElseThrow(() -> new MilitaryException("No military data found"));
        
        // Логика расформирования
        switch (unitType.toLowerCase()) {
            case "infantry": data.decrementInfantry(count); break;
            case "cavalry": data.decrementCavalry(count); break;
            case "artillery": data.decrementArtillery(count); break;
            case "navy": data.decrementNavy(count); break;
            case "airforce": data.decrementAirForce(count); break;
            default: throw new MilitaryException("Unknown unit type");
        }
        
        // Возврат части средств
        double refund = count * 0.5;
        nation.setTreasury(nation.getTreasury() + refund);
        
        nationRepository.save(nation);
        militaryRepository.save(data);
        updateMilitaryStrength(nationId);
        
        return "Disbanded: " + count + " " + unitType + ". Refunded: " + refund;
    }
    
    @Override
    public String upgradeUnits(String nationId, String unitType, int count, double cost) throws Exception {
        Nation nation = nationRepository.findById(nationId)
            .orElseThrow(() -> new MilitaryException("Nation not found"));
        
        if (nation.getTreasury() < cost * count) {
            throw new MilitaryException("Insufficient funds");
        }
        
        MilitaryData data = militaryRepository.findByNationId(nationId)
            .orElseThrow(() -> new MilitaryException("No military data found"));
        
        // Логика улучшения
        nation.setTreasury(nation.getTreasury() - cost * count);
        
        nationRepository.save(nation);
        militaryRepository.save(data);
        updateMilitaryStrength(nationId);
        
        return "Upgraded: " + count + " " + unitType + ". Strength increased.";
    }
}