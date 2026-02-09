package com.axiom.domain.service.industry;

import java.util.Map;

/**
 * Интерфейс для EconomyService
 * Определяет контракт для работы с экономикой
 */
public interface EconomyServiceInterface {
    
    /**
     * Получить баланс игрока
     * @param playerId ID игрока
     * @return баланс
     */
    double getBalance(java.util.UUID playerId);
    
    /**
     * Добавить средства на счет игрока
     * @param playerId ID игрока
     * @param amount сумма
     */
    void addBalance(java.util.UUID playerId, double amount);
    
    /**
     * Перевести средства между игроками
     * @param fromId ID отправителя
     * @param toId ID получателя
     * @param amount сумма
     * @return true если перевод успешен
     */
    boolean transfer(java.util.UUID fromId, java.util.UUID toId, double amount);
    
    /**
     * Напечатать деньги для нации
     * @param actor ID игрока (должен быть лидером или министром)
     * @param amount сумма
     * @return true если печать успешна
     */
    boolean printMoney(java.util.UUID actor, double amount);
    
    /**
     * Применить налоги к доходу игрока
     * @param playerId ID игрока
     * @param grossAmount валовый доход
     * @return чистый доход после налогов
     */
    double applyIncomeTaxes(java.util.UUID playerId, double grossAmount);
    
    /**
     * Перевести средства между нациями
     * @param fromNationId ID нации-отправителя
     * @param toNationId ID нации-получателя
     * @param amount сумма
     * @param reason причина перевода
     * @return сообщение о результате
     */
    String transferFunds(String fromNationId, String toNationId, double amount, String reason) throws Exception;
    
    /**
     * Применить налог с продаж
     * @param nationId ID нации
     * @param transactionAmount сумма транзакции
     * @return сумма налога
     */
    double applySalesTax(String nationId, double transactionAmount);
    
    /**
     * Получить ВВП нации
     * @param nationId ID нации
     * @return ВВП
     */
    double getGDP(String nationId);
    
    /**
     * Получить экономическое здоровье нации
     * @param nationId ID нации
     * @return экономическое здоровье (0-100)
     */
    double getEconomicHealth(String nationId);
    
    /**
     * Получить валюту нации
     * @param nationId ID нации
     * @return код валюты
     */
    String getNationCurrency(String nationId);
}