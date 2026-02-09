#!/usr/bin/env python3
import argparse
import json
import os
import re
import sys


def read_text(path: str) -> str:
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def unescape_java_string(value: str) -> str:
    # Minimal unescape for validation readability
    value = value.replace("\\\\", "\\")
    value = value.replace("\\\"", "\"")
    value = value.replace("\\n", "\n")
    value = value.replace("\\t", "\t")
    return value


def split_top_level_args(arg_text: str) -> list[str]:
    args = []
    buf = []
    depth = 0
    in_str = False
    escape = False
    i = 0
    while i < len(arg_text):
        ch = arg_text[i]
        if in_str:
            buf.append(ch)
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == "\"":
                in_str = False
            i += 1
            continue

        if ch == "\"":
            in_str = True
            buf.append(ch)
            i += 1
            continue

        if ch == "(":
            depth += 1
        elif ch == ")":
            if depth > 0:
                depth -= 1

        if ch == "," and depth == 0:
            args.append("".join(buf).strip())
            buf = []
            i += 1
            continue

        buf.append(ch)
        i += 1

    if buf:
        args.append("".join(buf).strip())
    return args


def extract_constructor_blocks(text: str, ctor: str) -> list[str]:
    blocks = []
    idx = 0
    needle = f"new {ctor}("
    while True:
        start = text.find(needle, idx)
        if start == -1:
            break
        i = start + len(needle)
        depth = 1
        in_str = False
        escape = False
        buf = []
        while i < len(text):
            ch = text[i]
            buf.append(ch)
            if in_str:
                if escape:
                    escape = False
                elif ch == "\\":
                    escape = True
                elif ch == "\"":
                    in_str = False
            else:
                if ch == "\"":
                    in_str = True
                elif ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                    if depth == 0:
                        buf.pop()  # drop closing ')'
                        break
            i += 1
        blocks.append("".join(buf).strip())
        idx = i + 1
    return blocks


def parse_quoted_strings(text: str) -> list[str]:
    values = []
    i = 0
    while i < len(text):
        if text[i] != "\"":
            i += 1
            continue
        i += 1
        buf = []
        escape = False
        while i < len(text):
            ch = text[i]
            if escape:
                buf.append(ch)
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == "\"":
                break
            else:
                buf.append(ch)
            i += 1
        values.append(unescape_java_string("".join(buf)))
        # Move past closing quote if present
        if i < len(text) and text[i] == "\"":
            i += 1
    return values


def parse_string_literal(arg: str) -> str | None:
    arg = arg.strip()
    if not arg.startswith("\""):
        return None
    values = parse_quoted_strings(arg)
    if not values:
        return None
    # Ensure it is a single literal with no extra tokens
    if len(values) != 1:
        return None
    trailing = arg[arg.rfind("\"") + 1 :].strip()
    if trailing:
        return None
    return values[0]


def parse_list_of_strings(arg: str) -> list[str]:
    return parse_quoted_strings(arg)


def validate_command_catalog(path: str) -> tuple[list[str], set[str]]:
    errors = []
    permissions = set()
    text = read_text(path)
    blocks = extract_constructor_blocks(text, "CommandInfo")
    if not blocks:
        errors.append("CommandCatalog: не найдены записи CommandInfo")
        return errors, permissions

    seen_commands = {}
    for idx, block in enumerate(blocks, start=1):
        args = split_top_level_args(block)
        if len(args) != 9:
            errors.append(f"CommandCatalog[{idx}]: ожидалось 9 аргументов, найдено {len(args)}")
            continue

        command = parse_string_literal(args[0])
        display = parse_string_literal(args[1])
        short_desc = parse_string_literal(args[2])
        full_desc = parse_string_literal(args[3])
        category = args[4].strip()
        aliases = parse_list_of_strings(args[5])
        examples = parse_list_of_strings(args[6])
        permission = parse_string_literal(args[7])
        requires_nation = args[8].strip()

        if not command:
            errors.append(f"CommandCatalog[{idx}]: не удалось прочитать команду")
        else:
            if not command.startswith("/"):
                errors.append(f"CommandCatalog[{idx}]: команда должна начинаться с '/': {command}")
            if command in seen_commands:
                errors.append(
                    f"CommandCatalog[{idx}]: дубликат команды '{command}' (раньше в #{seen_commands[command]})"
                )
            else:
                seen_commands[command] = idx

        if display is None or not display.strip():
            errors.append(f"CommandCatalog[{idx}]: пустое displayName")
        if short_desc is None or not short_desc.strip():
            errors.append(f"CommandCatalog[{idx}]: пустое shortDesc")
        if full_desc is None or not full_desc.strip():
            errors.append(f"CommandCatalog[{idx}]: пустое fullDesc")

        if not category.startswith("CommandCategory."):
            errors.append(f"CommandCatalog[{idx}]: не задана категория: {category}")

        if permission is None or not permission.strip():
            errors.append(f"CommandCatalog[{idx}]: пустой permission")
        else:
            permissions.add(permission.strip())

        if requires_nation not in ("true", "false"):
            errors.append(f"CommandCatalog[{idx}]: requiresNation должен быть true/false")

        alias_set = set()
        for alias in aliases:
            if not alias.startswith("/"):
                errors.append(f"CommandCatalog[{idx}]: alias должен начинаться с '/': {alias}")
            if alias == command:
                errors.append(f"CommandCatalog[{idx}]: alias совпадает с командой: {alias}")
            if alias in alias_set:
                errors.append(f"CommandCatalog[{idx}]: дубликат alias: {alias}")
            alias_set.add(alias)
            if alias in seen_commands:
                errors.append(
                    f"CommandCatalog[{idx}]: alias '{alias}' дублирует существующую команду (#{seen_commands[alias]})"
                )
            else:
                seen_commands.setdefault(alias, idx)

        for example in examples:
            if not example.startswith("/"):
                errors.append(f"CommandCatalog[{idx}]: пример должен начинаться с '/': {example}")

    return errors, permissions


def validate_religions_json(path: str) -> list[str]:
    errors = []
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as exc:
        return [f"religions.json: ошибка чтения: {exc}"]

    if not isinstance(data, list) or not data:
        return ["religions.json: должен быть непустой список"]

    seen_ids = set()
    for idx, entry in enumerate(data, start=1):
        if not isinstance(entry, dict):
            errors.append(f"religions.json[{idx}]: запись не объект")
            continue
        card_id = entry.get("id")
        if not isinstance(card_id, str) or not card_id.strip():
            errors.append(f"religions.json[{idx}]: пустой id")
        elif not re.fullmatch(r"[a-z0-9_-]+", card_id):
            errors.append(f"religions.json[{idx}]: id с недопустимыми символами: {card_id}")
        elif card_id in seen_ids:
            errors.append(f"religions.json[{idx}]: дубликат id: {card_id}")
        else:
            seen_ids.add(card_id)

        for key in ("name", "tagline", "symbol"):
            val = entry.get(key)
            if not isinstance(val, str) or not val.strip():
                errors.append(f"religions.json[{idx}]: пустое поле {key}")

        color = entry.get("color")
        if not isinstance(color, str) or not re.fullmatch(r"#[0-9A-Fa-f]{6}", color):
            errors.append(f"religions.json[{idx}]: некорректный color: {color}")

        details = entry.get("details")
        if not isinstance(details, list) or not details:
            errors.append(f"religions.json[{idx}]: details должен быть непустым списком")
        else:
            for detail in details:
                if not isinstance(detail, str) or not detail.strip():
                    errors.append(f"religions.json[{idx}]: пустая строка в details")
                    break

    return errors


def parse_plugin_permissions(path: str) -> set[str]:
    permissions = set()
    try:
        lines = read_text(path).splitlines()
    except Exception:
        return permissions

    in_permissions = False
    for line in lines:
        if not in_permissions:
            if re.match(r"^permissions\s*:\s*$", line.strip()):
                in_permissions = True
            continue

        if not line.startswith("  "):
            break

        match = re.match(r"^\s{2}([A-Za-z0-9_.-]+)\s*:\s*$", line)
        if match:
            permissions.add(match.group(1))

    return permissions


def extract_permissions_from_code(root_dir: str) -> set[str]:
    permissions = set()
    if not root_dir or not os.path.isdir(root_dir):
        return permissions

    patterns = [
        re.compile(r"\bhasPermission\(\"([^\"]+)\"\)"),
        re.compile(r"\bsetPermission\(\"([^\"]+)\"\)"),
        re.compile(r"\bpermission\(\"([^\"]+)\"\)"),
    ]

    for base, _, files in os.walk(root_dir):
        for name in files:
            if not name.endswith((".java", ".kt")):
                continue
            path = os.path.join(base, name)
            try:
                text = read_text(path)
            except Exception:
                continue
            for pattern in patterns:
                for match in pattern.findall(text):
                    if match:
                        permissions.add(match.strip())

    return permissions


def validate_ui_contract(client_path: str, server_path: str) -> list[str]:
    errors = []
    client_text = read_text(client_path)
    server_text = read_text(server_path)

    client_requests = set(re.findall(r"sendToServer\([^,]+,\s*\"(get_[^\"]+)\"", client_text))
    update_types = set(re.findall(r"requestUpdate\(\"([^\"]+)\"\)", client_text))
    client_requests |= {"get_" + t for t in update_types}

    server_requests = set(re.findall(r"case\s+\"(get_[^\"]+)\"", server_text))
    extra_server_responses = {"ui_autotest_start", "open_main_menu"}

    missing_on_server = client_requests - server_requests
    if missing_on_server:
        errors.append("UI->Server: нет обработчиков для " + ", ".join(sorted(missing_on_server)))

    server_only = server_requests - client_requests
    if server_only:
        errors.append("Server: есть обработчики без клиента: " + ", ".join(sorted(server_only)))

    server_responses = set(re.findall(r"sendData\([^,]+,\s*\"([^\"]+)\"", server_text))
    server_responses |= extra_server_responses
    client_responses = set(re.findall(r"case\s+\"([^\"]+)\"", client_text))

    missing_on_client = server_responses - client_responses
    if missing_on_client:
        errors.append("Server->UI: клиент не обрабатывает " + ", ".join(sorted(missing_on_client)))

    client_only = client_responses - server_responses
    if client_only:
        errors.append("UI: есть обработчики без сервера: " + ", ".join(sorted(client_only)))

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate AXIOM UI catalog/resources/contract.")
    parser.add_argument("--catalog", default="axiom-mod-integration/src/main/java/com/axiom/ui/CommandCatalog.java")
    parser.add_argument("--religions", default="axiom-mod-integration/src/main/resources/assets/axiomui/religions.json")
    parser.add_argument("--client", default="axiom-mod-integration/src/main/java/com/axiom/ui/AxiomUiClientEvents.java")
    parser.add_argument("--server", default="axiom-plugin/src/main/java/com/axiom/infra/network/ModCommunicationHandler.java")
    parser.add_argument("--plugin-yml", default="axiom-plugin/src/main/resources/plugin.yml")
    parser.add_argument("--plugin-src", default="axiom-plugin/src/main/java")
    args = parser.parse_args()

    errors = []
    catalog_errors, catalog_permissions = validate_command_catalog(args.catalog)
    errors.extend(catalog_errors)
    errors.extend(validate_religions_json(args.religions))
    server_path = args.server
    if not os.path.exists(server_path):
        legacy_path = "axiom-plugin/src/main/java/com/axiom/network/ModCommunicationHandler.java"
        alt_path = "axiom-plugin/src/main/java/com/axiom/infra/network/ModCommunicationHandler.java"
        if os.path.exists(alt_path):
            server_path = alt_path
        elif os.path.exists(legacy_path):
            server_path = legacy_path
    errors.extend(validate_ui_contract(args.client, server_path))

    plugin_permissions = parse_plugin_permissions(args.plugin_yml)
    code_permissions = extract_permissions_from_code(args.plugin_src)
    required_permissions = catalog_permissions | code_permissions

    missing_permissions = sorted(required_permissions - plugin_permissions)
    if missing_permissions:
        errors.append("plugin.yml: отсутствуют permissions: " + ", ".join(missing_permissions))

    if errors:
        print("UI validation failed:")
        for err in errors:
            print(" - " + err)
        return 1

    print("UI validation passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
