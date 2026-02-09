#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path

ITEM_ID_RE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


def fail(msg):
    print(f"ERROR: {msg}")


def warn(msg):
    print(f"WARN: {msg}")


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("balance_config/recipes.json")

    if not path.exists():
        fail(f"file not found: {path}")
        return 1

    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"invalid JSON: {exc}")
        return 1

    if not isinstance(data, dict):
        fail("top-level JSON must be an object")
        return 1

    recipes = data.get("recipes")
    if not isinstance(recipes, dict):
        fail("'recipes' must be an object")
        return 1

    errors = 0
    warnings = 0

    for recipe_id, spec in recipes.items():
        if not isinstance(recipe_id, str) or not recipe_id:
            fail("recipe id must be a non-empty string")
            errors += 1
            continue

        if not isinstance(spec, dict):
            fail(f"{recipe_id}: recipe spec must be an object")
            errors += 1
            continue

        for key in ("original", "modified"):
            if key not in spec:
                fail(f"{recipe_id}: missing '{key}'")
                errors += 1
                continue
            value = spec[key]
            if not isinstance(value, list) or not value:
                fail(f"{recipe_id}: '{key}' must be a non-empty list")
                errors += 1
                continue

            for idx, ing in enumerate(value):
                if not isinstance(ing, dict):
                    fail(f"{recipe_id}: '{key}'[{idx}] must be an object")
                    errors += 1
                    continue
                item = ing.get("item")
                count = ing.get("count")
                if not isinstance(item, str) or not item:
                    fail(f"{recipe_id}: '{key}'[{idx}].item must be a non-empty string")
                    errors += 1
                elif not ITEM_ID_RE.match(item):
                    warn(f"{recipe_id}: '{key}'[{idx}].item has unexpected format: {item}")
                    warnings += 1

                if not isinstance(count, int) or count <= 0:
                    fail(f"{recipe_id}: '{key}'[{idx}].count must be a positive int")
                    errors += 1

        reason = spec.get("reason")
        if reason is not None and not isinstance(reason, str):
            warn(f"{recipe_id}: 'reason' should be a string")
            warnings += 1

    if errors:
        print(f"Validation failed: {errors} error(s), {warnings} warning(s)")
        return 1

    print(f"Validation OK: {len(recipes)} recipe(s), {warnings} warning(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
