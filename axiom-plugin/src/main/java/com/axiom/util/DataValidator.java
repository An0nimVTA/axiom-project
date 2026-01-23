package com.axiom.util;

import com.axiom.exception.ValidationException;
import org.bukkit.ChatColor;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Утилита для валидации данных
 */
public class DataValidator {
    
    // Регулярные выражения для валидации
    private static final Pattern NATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");
    private static final Pattern CITY_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    
    /**
     * Валидация ID нации
     * @param nationId ID нации
     * @throws ValidationException если ID невалиден
     */
    public static void validateNationId(String nationId) throws ValidationException {
        if (nationId == null || nationId.isEmpty()) {
            throw new ValidationException("Nation ID cannot be null or empty");
        }
        
        if (!NATION_ID_PATTERN.matcher(nationId).matches()) {
            throw new ValidationException("Invalid nation ID format. Use 3-32 alphanumeric characters, underscores or hyphens");
        }
    }
    
    /**
     * Валидация ID города
     * @param cityId ID города
     * @throws ValidationException если ID невалиден
     */
    public static void validateCityId(String cityId) throws ValidationException {
        if (cityId == null || cityId.isEmpty()) {
            throw new ValidationException("City ID cannot be null or empty");
        }
        
        if (!CITY_ID_PATTERN.matcher(cityId).matches()) {
            throw new ValidationException("Invalid city ID format. Use 3-32 alphanumeric characters, underscores or hyphens");
        }
    }
    
    /**
     * Валидация имени игрока
     * @param playerName имя игрока
     * @throws ValidationException если имя невалидно
     */
    public static void validatePlayerName(String playerName) throws ValidationException {
        if (playerName == null || playerName.isEmpty()) {
            throw new ValidationException("Player name cannot be null or empty");
        }
        
        if (!PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
            throw new ValidationException("Invalid player name format. Use 3-16 alphanumeric characters or underscores");
        }
    }
    
    /**
     * Валидация UUID игрока
     * @param playerId UUID игрока
     * @throws ValidationException если UUID невалиден
     */
    public static void validatePlayerId(UUID playerId) throws ValidationException {
        if (playerId == null) {
            throw new ValidationException("Player ID cannot be null");
        }
    }
    
    /**
     * Валидация типа войск
     * @param unitType тип войск
     * @throws ValidationException если тип невалиден
     */
    public static void validateUnitType(String unitType) throws ValidationException {
        if (unitType == null || unitType.isEmpty()) {
            throw new ValidationException("Unit type cannot be null or empty");
        }
        
        String normalized = unitType.toLowerCase();
        if (!normalized.matches("infantry|cavalry|artillery|navy|airforce")) {
            throw new ValidationException("Invalid unit type. Use: infantry, cavalry, artillery, navy, or airforce");
        }
    }
    
    /**
     * Валидация положительного числа
     * @param value число
     * @param fieldName название поля
     * @throws ValidationException если число невалидно
     */
    public static void validatePositiveNumber(double value, String fieldName) throws ValidationException {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ValidationException(fieldName + " must be a valid number");
        }
        
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }
    
    /**
     * Валидация неотрицательного числа
     * @param value число
     * @param fieldName название поля
     * @throws ValidationException если число невалидно
     */
    public static void validateNonNegativeNumber(double value, String fieldName) throws ValidationException {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ValidationException(fieldName + " must be a valid number");
        }
        
        if (value < 0) {
            throw new ValidationException(fieldName + " cannot be negative");
        }
    }
    
    /**
     * Валидация положительного целого числа
     * @param value число
     * @param fieldName название поля
     * @throws ValidationException если число невалидно
     */
    public static void validatePositiveInteger(int value, String fieldName) throws ValidationException {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be positive integer");
        }
    }
    
    /**
     * Валидация неотрицательного целого числа
     * @param value число
     * @param fieldName название поля
     * @throws ValidationException если число невалидно
     */
    public static void validateNonNegativeInteger(int value, String fieldName) throws ValidationException {
        if (value < 0) {
            throw new ValidationException(fieldName + " cannot be negative");
        }
    }
    
    /**
     * Валидация строки на максимальную длину
     * @param value строка
     * @param maxLength максимальная длина
     * @param fieldName название поля
     * @throws ValidationException если строка слишком длинная
     */
    public static void validateMaxLength(String value, int maxLength, String fieldName) throws ValidationException {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(fieldName + " cannot exceed " + maxLength + " characters");
        }
    }
    
    /**
     * Форматирование сообщения об ошибке для игрока
     * @param message сообщение
     * @return форматированное сообщение
     */
    public static String formatErrorMessage(String message) {
        return ChatColor.RED + "Ошибка: " + ChatColor.WHITE + message;
    }
    
    /**
     * Форматирование успешного сообщения для игрока
     * @param message сообщение
     * @return форматированное сообщение
     */
    public static String formatSuccessMessage(String message) {
        return ChatColor.GREEN + "Успех: " + ChatColor.WHITE + message;
    }
    
    /**
     * Форматирование информационного сообщения для игрока
     * @param message сообщение
     * @return форматированное сообщение
     */
    public static String formatInfoMessage(String message) {
        return ChatColor.YELLOW + "Информация: " + ChatColor.WHITE + message;
    }
}