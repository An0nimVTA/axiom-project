package com.axiom.ui;

import java.util.ArrayList;
import java.util.List;

public class CommandCatalog {
    private static final List<CommandInfo> COMMANDS = new ArrayList<>();

    static {
        // ========== NATION COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/nation create <название>",
            "Создать нацию",
            "Основать собственную нацию",
            "Создаёт новую нацию с указанным названием. Вы становитесь лидером. Требует начальный капитал.",
            CommandCategory.NATION,
            List.of("/n create", "/nation new"),
            List.of("/nation create Россия", "/n create \"Великая Империя\""),
            "axiom.nation.create",
            false
        ));

        COMMANDS.add(new CommandInfo(
            "/nation join <название>",
            "Присоединиться к нации",
            "Вступить в существующую нацию",
            "Отправляет запрос на вступление в нацию. Требуется одобрение лидера или офицера.",
            CommandCategory.NATION,
            List.of("/n join"),
            List.of("/nation join Россия"),
            "axiom.nation.join",
            false
        ));

        COMMANDS.add(new CommandInfo(
            "/nation leave",
            "Покинуть нацию",
            "Выйти из текущей нации",
            "Покидаете свою нацию. Лидер не может покинуть нацию, пока не передаст власть.",
            CommandCategory.NATION,
            List.of("/n leave", "/nation quit"),
            List.of("/nation leave"),
            "axiom.nation.leave",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/nation info [название]",
            "Информация о нации",
            "Показать данные нации",
            "Отображает полную информацию: лидер, граждане, территория, казна, технологии, репутация.",
            CommandCategory.NATION,
            List.of("/n info", "/nation show"),
            List.of("/nation info", "/nation info Россия"),
            "axiom.nation.info",
            false
        ));

        COMMANDS.add(new CommandInfo(
            "/nation invite <игрок>",
            "Пригласить игрока",
            "Отправить приглашение в нацию",
            "Приглашает игрока в вашу нацию. Требуется роль лидера или офицера.",
            CommandCategory.NATION,
            List.of("/n invite"),
            List.of("/nation invite Steve"),
            "axiom.nation.invite",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/nation kick <игрок>",
            "Исключить игрока",
            "Выгнать гражданина из нации",
            "Исключает игрока из нации. Требуется роль лидера или офицера.",
            CommandCategory.NATION,
            List.of("/n kick"),
            List.of("/nation kick Alex"),
            "axiom.nation.kick",
            true
        ));

        // ========== ECONOMY COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/wallet",
            "Кошелёк",
            "Проверить баланс",
            "Показывает ваш текущий баланс и историю последних транзакций.",
            CommandCategory.ECONOMY,
            List.of("/money", "/bal", "/balance"),
            List.of("/wallet", "/wallet Steve"),
            "axiom.wallet.check",
            false
        ));

        COMMANDS.add(new CommandInfo(
            "/wallet pay <игрок> <сумма>",
            "Перевести деньги",
            "Отправить средства игроку",
            "Переводит указанную сумму другому игроку. Комиссия 1%.",
            CommandCategory.ECONOMY,
            List.of("/pay"),
            List.of("/wallet pay Steve 1000", "/pay Alex 500"),
            "axiom.wallet.pay",
            false
        ));

        COMMANDS.add(new CommandInfo(
            "/bank",
            "Банк",
            "Открыть банковское меню",
            "Открывает меню банка: депозиты, снятие, кредиты, инвестиции. Требует технологию 'banking'.",
            CommandCategory.ECONOMY,
            List.of("/banking"),
            List.of("/bank", "/bank deposit 5000"),
            "axiom.bank.use",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/trade",
            "Торговля",
            "Открыть торговую площадку",
            "Открывает торговую площадку для покупки/продажи ресурсов между нациями.",
            CommandCategory.ECONOMY,
            List.of("/market", "/shop"),
            List.of("/trade", "/trade sell diamond 64 1000"),
            "axiom.trade.use",
            false
        ));

        // ========== DIPLOMACY COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/treaty create <нация> <тип>",
            "Создать договор",
            "Предложить договор нации",
            "Создаёт дипломатический договор: peace (мир), trade (торговля), alliance (союз), nap (пакт о ненападении).",
            CommandCategory.DIPLOMACY,
            List.of("/treaty new"),
            List.of("/treaty create Россия alliance", "/treaty create США trade"),
            "axiom.treaty.create",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/ally <нация>",
            "Союз",
            "Предложить союз",
            "Отправляет предложение о военном союзе. Требует технологию 'advanced_diplomacy'.",
            CommandCategory.DIPLOMACY,
            List.of("/alliance"),
            List.of("/ally Россия"),
            "axiom.ally.create",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/war declare <нация>",
            "Объявить войну",
            "Начать военный конфликт",
            "Объявляет войну указанной нации. Требует технологию 'basic_military' и казну на военные расходы.",
            CommandCategory.MILITARY,
            List.of("/war start"),
            List.of("/war declare Россия"),
            "axiom.war.declare",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/war peace <нация>",
            "Заключить мир",
            "Предложить мирный договор",
            "Отправляет предложение о мире. Можно указать условия: репарации, территории.",
            CommandCategory.MILITARY,
            List.of("/peace"),
            List.of("/war peace Россия", "/war peace США reparations 10000"),
            "axiom.war.peace",
            true
        ));

        // ========== TECHNOLOGY COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/tech",
            "Технологии",
            "Открыть дерево технологий",
            "Открывает интерактивное меню дерева технологий с 5 уровнями и 5 ветвями развития.",
            CommandCategory.TECHNOLOGY,
            List.of("/technology"),
            List.of("/tech", "/tech research firearms_tech"),
            "axiom.tech.view",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/tech research <id>",
            "Исследовать технологию",
            "Начать изучение технологии",
            "Начинает исследование указанной технологии. Требует выполнение предварительных условий, образование и средства.",
            CommandCategory.TECHNOLOGY,
            List.of("/research"),
            List.of("/tech research firearms_tech", "/tech research banking"),
            "axiom.tech.research",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/tech list",
            "Список технологий",
            "Показать доступные технологии",
            "Выводит список всех доступных для исследования технологий с требованиями и стоимостью.",
            CommandCategory.TECHNOLOGY,
            List.of("/tech available"),
            List.of("/tech list", "/tech list tier 3"),
            "axiom.tech.list",
            true
        ));

        // ========== TERRITORY COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/claim",
            "Захватить территорию",
            "Присоединить чанк к нации",
            "Захватывает текущий чанк для вашей нации. Требует технологию 'territory_claim' и средства.",
            CommandCategory.NATION,
            List.of("/territory claim"),
            List.of("/claim"),
            "axiom.territory.claim",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/unclaim",
            "Освободить территорию",
            "Отказаться от чанка",
            "Освобождает текущий чанк от владения нации. Возвращает 50% стоимости захвата.",
            CommandCategory.NATION,
            List.of("/territory unclaim"),
            List.of("/unclaim"),
            "axiom.territory.unclaim",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/capture",
            "Система захвата",
            "Открыть меню осад",
            "Открывает меню системы захвата: ядра стран, ядра городов, укрепления, осады.",
            CommandCategory.MILITARY,
            List.of("/siege"),
            List.of("/capture", "/capture begin_siege"),
            "axiom.capture.use",
            true
        ));

        // ========== CITY COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/city create <название>",
            "Создать город",
            "Основать новый город",
            "Создаёт город на текущей территории нации. Требует технологию 'basic_construction'.",
            CommandCategory.CITY,
            List.of("/city new"),
            List.of("/city create Москва"),
            "axiom.city.create",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/city build <тип>",
            "Построить здание",
            "Строительство в городе",
            "Строит здание в городе: factory (завод), school (школа), hospital (больница), barracks (казарма).",
            CommandCategory.CITY,
            List.of("/build"),
            List.of("/city build factory", "/city build school"),
            "axiom.city.build",
            true
        ));

        // ========== RELIGION COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/religion create <название>",
            "Создать религию",
            "Основать новую религию",
            "Создаёт новую религиозную систему. Требует технологию 'religion' и средства.",
            CommandCategory.RELIGION,
            List.of("/faith create"),
            List.of("/religion create Православие"),
            "axiom.religion.create",
            true
        ));

        COMMANDS.add(new CommandInfo(
            "/religion join <название>",
            "Принять веру",
            "Присоединиться к религии",
            "Принимаете указанную религию. Даёт бонусы и открывает ритуалы.",
            CommandCategory.RELIGION,
            List.of("/faith join"),
            List.of("/religion join Христианство"),
            "axiom.religion.join",
            false
        ));

        // ========== PROFILE COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/profile",
            "Профиль",
            "Личная информация",
            "Показывает ваш профиль: статистика, достижения, репутация, история.",
            CommandCategory.PROFILE,
            List.of("/me", "/stats"),
            List.of("/profile", "/profile Steve"),
            "axiom.profile.view",
            false
        ));

        COMMANDS.add(new CommandInfo(
            "/history",
            "История",
            "Хронология событий",
            "Показывает историю нации или личную хронологию событий.",
            CommandCategory.PROFILE,
            List.of("/timeline"),
            List.of("/history", "/history nation"),
            "axiom.history.view",
            false
        ));

        // ========== ADMIN COMMANDS ==========
        COMMANDS.add(new CommandInfo(
            "/axiom reload",
            "Перезагрузить плагин",
            "Перезагрузка конфигурации",
            "Перезагружает все конфигурационные файлы плагина без перезапуска сервера.",
            CommandCategory.ADMIN,
            List.of("/ax reload"),
            List.of("/axiom reload"),
            "axiom.admin.reload",
            false
        ));

        COMMANDS.add(new CommandInfo(
            "/axiom debug",
            "Отладка",
            "Отладочная информация",
            "Показывает отладочную информацию: обнаруженные моды, статистику, ошибки.",
            CommandCategory.ADMIN,
            List.of("/ax debug"),
            List.of("/axiom debug", "/axiom debug mods"),
            "axiom.admin.debug",
            false
        ));

        COMMANDS.add(new CommandInfo(
            "/testbot",
            "Тестовый бот",
            "Автоматическое тестирование",
            "Запускает автоматические тесты всех систем плагина.",
            CommandCategory.ADMIN,
            List.of("/tb", "/autotest"),
            List.of("/testbot", "/tb run"),
            "axiom.admin.testbot",
            false
        ));
    }

    public static List<CommandInfo> getAllCommands() {
        return new ArrayList<>(COMMANDS);
    }

    public static List<CommandInfo> getCommandsByCategory(CommandCategory category) {
        return COMMANDS.stream()
            .filter(cmd -> cmd.getCategory() == category)
            .toList();
    }
}
