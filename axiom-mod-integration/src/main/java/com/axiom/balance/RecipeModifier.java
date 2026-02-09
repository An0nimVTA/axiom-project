package com.axiom.balance;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.*;

@Mod.EventBusSubscriber
public class RecipeModifier {
    
    private static final Map<String, RecipeChange> RECIPE_CHANGES = new HashMap<>();
    
    static {
        // === ПРИМЕРЫ ИЗМЕНЕНИЙ ===
        
        // TACZ AK-47 (tacz:ak47) - пример усложнения
        // Стандартный крафт обычно проще. Мы заменяем его на список дорогих компонентов.
        RECIPE_CHANGES.put("tacz:ak47", new RecipeChange(
            Arrays.asList(
                "minecraft:iron_block", "pointblank:gun_parts", "minecraft:iron_block",
                "ballistix:barrel", "caps:tactical_grip", "ballistix:barrel",
                "minecraft:iron_ingot", "pointblank:gun_parts", "minecraft:iron_ingot"
            ),
            "Требует детали из Point Blank, Ballistix, CAPS"
        ));
        
        // Пример для Immersive Engineering
        RECIPE_CHANGES.put("immersiveengineering:crafting/crusher", new RecipeChange(
            Arrays.asList(
                "minecraft:iron_block", "industrialupgrade:electric_motor", "minecraft:iron_block",
                "minecraft:piston", "appliedenergistics2:engineering_processor", "minecraft:piston",
                "minecraft:copper_block", "industrialupgrade:electric_motor", "minecraft:copper_block"
            ),
            "Требует процессор из AE2 и мотор из Industrial"
        ));
    }
    
    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        System.out.println("[AXIOM Balance] Перехват и модификация рецептов...");
        modifyRecipes(event.getServer().getRecipeManager());
    }

    private static void modifyRecipes(RecipeManager recipeManager) {
        try {
            Map<ResourceLocation, Recipe<?>> recipesMap = resolveRecipesMap(recipeManager);
            if (recipesMap.isEmpty()) {
                System.err.println("[AXIOM Balance] Не удалось получить карту рецептов из RecipeManager");
                return;
            }

            int modifiedCount = 0;

            for (Map.Entry<String, RecipeChange> change : RECIPE_CHANGES.entrySet()) {
                ResourceLocation recipeId = new ResourceLocation(change.getKey());
                Recipe<?> oldRecipe = recipesMap.get(recipeId);
                
                if (oldRecipe instanceof ShapedRecipe shapedRecipe) {
                    System.out.println("[AXIOM Balance] Патчим рецепт: " + recipeId);
                    
                    // 2. Создаем новый список ингредиентов (NonNullList)
                    NonNullList<Ingredient> newIngredients = NonNullList.create();
                    for (String itemId : change.getValue().ingredients) {
                        newIngredients.add(createIngredient(itemId));
                    }
                    
                    // Если размер нового списка не совпадает с размером рецепта (width * height),
                    // то рецепт может сломаться или отображаться криво. 
                    // Для надежности мы просто заменяем список ингредиентов.
                    // В идеале нужно менять и width/height, но это сложнее. 
                    // Предполагаем, что мы заменяем рецепт 3x3 на 3x3.
                    
                    // 3. Внедряем новые ингредиенты через Reflection
                    try {
                        // Поле 'recipeItems' или 'ingredients' в ShapedRecipe
                        // Нужно найти правильное имя поля. В dev среде это 'recipeItems'.
                        // В продакшене (Forge) это может быть f_44146_ (SRG)
                        Field ingredientsField = null;
                        
                        // Пытаемся найти поле по типу
                        for (Field f : ShapedRecipe.class.getDeclaredFields()) {
                            if (f.getType() == NonNullList.class) {
                                ingredientsField = f;
                                break;
                            }
                        }
                        
                        if (ingredientsField != null) {
                            ingredientsField.setAccessible(true);
                            ingredientsField.set(shapedRecipe, newIngredients);
                            System.out.println("   -> Успешно заменены ингредиенты");
                            modifiedCount++;
                        } else {
                            System.err.println("   -> Ошибка: Не найдено поле ингредиентов в ShapedRecipe");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("   -> Ошибка при замене полей: " + e.getMessage());
                    }
                    
                } else {
                    if (oldRecipe == null) {
                        System.out.println("[AXIOM Balance] Рецепт не найден (возможно, мод не загружен): " + recipeId);
                    } else {
                        System.out.println("[AXIOM Balance] Рецепт не является ShapedRecipe (пропуск): " + recipeId + " (" + oldRecipe.getClass().getName() + ")");
                    }
                }
            }
            
            System.out.println("[AXIOM Balance] Итог: Изменено " + modifiedCount + " рецептов.");
            
        } catch (Exception e) {
            System.err.println("[AXIOM Balance] Критическая ошибка RecipeModifier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<ResourceLocation, Recipe<?>> resolveRecipesMap(RecipeManager recipeManager) {
        try {
            try {
                var getRecipesMethod = RecipeManager.class.getMethod("getRecipes");
                Object raw = getRecipesMethod.invoke(recipeManager);
                Map<ResourceLocation, Recipe<?>> flattened = flattenRecipeMap(raw);
                if (!flattened.isEmpty()) {
                    return flattened;
                }
            } catch (Exception ignored) {
            }

            for (Field field : RecipeManager.class.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object raw = field.get(recipeManager);
                Map<ResourceLocation, Recipe<?>> flattened = flattenRecipeMap(raw);
                if (!flattened.isEmpty()) {
                    return flattened;
                }
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyMap();
    }

    private static Map<ResourceLocation, Recipe<?>> flattenRecipeMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        Map<ResourceLocation, Recipe<?>> flattened = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof ResourceLocation && value instanceof Recipe) {
                flattened.put((ResourceLocation) key, (Recipe<?>) value);
                continue;
            }
            if (value instanceof Map<?, ?> nested) {
                for (Map.Entry<?, ?> nestedEntry : nested.entrySet()) {
                    Object nestedKey = nestedEntry.getKey();
                    Object nestedValue = nestedEntry.getValue();
                    if (nestedKey instanceof ResourceLocation && nestedValue instanceof Recipe) {
                        flattened.put((ResourceLocation) nestedKey, (Recipe<?>) nestedValue);
                    }
                }
            }
        }
        return flattened;
    }
    
    private static Ingredient createIngredient(String itemId) {
        ResourceLocation id = new ResourceLocation(itemId);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item != null) {
            return Ingredient.of(new ItemStack(item));
        } else {
            System.err.println("[AXIOM Balance] ОШИБКА: Предмет не найден: " + itemId);
            return Ingredient.EMPTY; // Чтобы не крашить, возвращаем пустой
        }
    }
    
    static class RecipeChange {
        List<String> ingredients;
        String reason;
        
        RecipeChange(List<String> ingredients, String reason) {
            this.ingredients = ingredients;
            this.reason = reason;
        }
    }
}
