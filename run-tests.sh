#!/bin/bash
# Автоматические тесты AXIOM UI Mod

set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         AXIOM UI Mod - Автоматические тесты                  ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASSED=0
FAILED=0
WARNINGS=0

cd "$(dirname "$0")/axiom-mod-integration"

# Тест 1: Проверка существования файлов
echo -e "${YELLOW}[1/8]${NC} Проверка файлов..."
FILES=(
    "src/main/java/com/axiom/ui/CommandSearchWidget.java"
    "src/main/java/com/axiom/ui/CommandHistory.java"
    "src/main/java/com/axiom/ui/NotificationManager.java"
    "src/main/java/com/axiom/ui/CommandMenuScreen.java"
    "src/main/java/com/axiom/ui/CommandCardWidget.java"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "  ${GREEN}✓${NC} $file"
        ((++PASSED))
    else
        echo -e "  ${RED}✗${NC} $file - НЕ НАЙДЕН"
        ((++FAILED))
    fi
done

# Тест 2: Проверка синтаксиса Java
echo -e "\n${YELLOW}[2/8]${NC} Проверка синтаксиса Java..."
if GRADLE_USER_HOME=$PWD/.gradle ./gradlew --no-daemon compileJava > /tmp/compile.log 2>&1; then
    echo -e "  ${GREEN}✓${NC} Синтаксис корректен"
    ((++PASSED))
    
    # Проверка предупреждений
    WARN_COUNT=$(grep -c "warning:" /tmp/compile.log || true)
    if [ "$WARN_COUNT" -gt 0 ]; then
        echo -e "  ${YELLOW}⚠${NC} Найдено предупреждений: $WARN_COUNT"
                ((++WARNINGS))
    fi
else
    echo -e "  ${RED}✗${NC} Ошибки компиляции"
    tail -20 /tmp/compile.log
    ((FAILED++))
fi

# Тест 3: Сборка JAR
echo -e "\n${YELLOW}[3/8]${NC} Сборка JAR..."
if GRADLE_USER_HOME=$PWD/.gradle ./gradlew --no-daemon build -x test > /tmp/build.log 2>&1; then
    echo -e "  ${GREEN}✓${NC} Сборка успешна"
    ((++PASSED))
else
    echo -e "  ${RED}✗${NC} Ошибка сборки"
    tail -20 /tmp/build.log
    ((FAILED++))
fi

# Тест 4: Проверка JAR файла
echo -e "\n${YELLOW}[4/8]${NC} Проверка JAR..."
JAR_FILE="build/libs/axiomui-0.1.0.jar"
if [ -f "$JAR_FILE" ]; then
    SIZE=$(du -h "$JAR_FILE" | cut -f1)
    echo -e "  ${GREEN}✓${NC} JAR создан: $SIZE"
    ((++PASSED))
else
    echo -e "  ${RED}✗${NC} JAR не найден"
    ((FAILED++))
fi

# Тест 5: Проверка классов в JAR
echo -e "\n${YELLOW}[5/8]${NC} Проверка классов в JAR..."
REQUIRED_CLASSES=(
    "com/axiom/ui/CommandSearchWidget.class"
    "com/axiom/ui/CommandHistory.class"
    "com/axiom/ui/NotificationManager.class"
)

for class in "${REQUIRED_CLASSES[@]}"; do
    if jar tf "$JAR_FILE" | grep -q "$class"; then
        echo -e "  ${GREEN}✓${NC} $class"
        ((++PASSED))
    else
        echo -e "  ${RED}✗${NC} $class - НЕ НАЙДЕН В JAR"
        ((++FAILED))
    fi
done

# Тест 6: Проверка размера JAR
echo -e "\n${YELLOW}[6/8]${NC} Проверка размера JAR..."
SIZE_BYTES=$(stat -f%z "$JAR_FILE" 2>/dev/null || stat -c%s "$JAR_FILE" 2>/dev/null)
if [ "$SIZE_BYTES" -gt 50000 ] && [ "$SIZE_BYTES" -lt 100000 ]; then
    echo -e "  ${GREEN}✓${NC} Размер адекватный: $SIZE_BYTES байт"
    ((++PASSED))
else
    echo -e "  ${YELLOW}⚠${NC} Размер необычный: $SIZE_BYTES байт"
            ((++WARNINGS))
fi

# Тест 7: Проверка количества классов
echo -e "\n${YELLOW}[7/8]${NC} Проверка количества классов..."
CLASS_COUNT=$(jar tf "$JAR_FILE" | grep -c "\.class$" || true)
if [ "$CLASS_COUNT" -gt 20 ]; then
    echo -e "  ${GREEN}✓${NC} Классов в JAR: $CLASS_COUNT"
    ((++PASSED))
else
    echo -e "  ${RED}✗${NC} Мало классов: $CLASS_COUNT"
    ((FAILED++))
fi

# Тест 8: Проверка структуры пакетов
echo -e "\n${YELLOW}[8/8]${NC} Проверка структуры пакетов..."
PACKAGES=(
    "com/axiom/ui/"
    "com/axiom/ui/tech/"
)

for pkg in "${PACKAGES[@]}"; do
    if jar tf "$JAR_FILE" | grep -q "$pkg"; then
        echo -e "  ${GREEN}✓${NC} $pkg"
        ((++PASSED))
    else
        echo -e "  ${RED}✗${NC} $pkg - НЕ НАЙДЕН"
        ((++FAILED))
    fi
done

# Итоги
echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                        РЕЗУЛЬТАТЫ                             ║"
echo "╠═══════════════════════════════════════════════════════════════╣"
echo -e "║  ${GREEN}Пройдено:${NC}      $PASSED                                          ║"
echo -e "║  ${RED}Провалено:${NC}     $FAILED                                          ║"
echo -e "║  ${YELLOW}Предупреждений:${NC} $WARNINGS                                          ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}✅ ВСЕ ТЕСТЫ ПРОЙДЕНЫ!${NC}"
    exit 0
else
    echo -e "${RED}❌ ЕСТЬ ОШИБКИ!${NC}"
    exit 1
fi
