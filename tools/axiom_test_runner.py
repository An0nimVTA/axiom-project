#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import sys
import time
import uuid
from datetime import datetime
from pathlib import Path
import xml.etree.ElementTree as ET
import hashlib
from typing import List, Optional, Tuple


def now_stamp():
    return datetime.now().strftime("%Y%m%d-%H%M%S")


def read_properties(path: Path) -> dict:
    props = {}
    if not path.exists():
        return props
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" in line:
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def offline_uuid(name: str) -> str:
    raw = ("OfflinePlayer:" + name).encode("utf-8")
    digest = bytearray(hashlib.md5(raw).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30
    digest[8] = (digest[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(digest)))


def ensure_op(server_dir: Path, username: str) -> Tuple[bool, str]:
    if not server_dir.exists():
        return False, f"server dir not found: {server_dir}"
    props = read_properties(server_dir / "server.properties")
    online_mode = props.get("online-mode", "").lower()
    if online_mode == "true":
        # In online-mode we cannot compute offline UUID; skip OP automation.
        return True, "online-mode=true; skipping offline op automation"

    ops_path = server_dir / "ops.json"
    ops = []
    if ops_path.exists():
        try:
            ops = json.loads(ops_path.read_text(encoding="utf-8"))
        except Exception:
            ops = []

    player_uuid = offline_uuid(username)
    updated = False
    for entry in ops:
        if entry.get("name") == username or entry.get("uuid") == player_uuid:
            entry["name"] = username
            entry["uuid"] = player_uuid
            entry["level"] = max(int(entry.get("level", 4)), 4)
            entry.setdefault("bypassesPlayerLimit", False)
            updated = True
            break

    if not updated:
        ops.append(
            {
                "uuid": player_uuid,
                "name": username,
                "level": 4,
                "bypassesPlayerLimit": False,
            }
        )

    ops_path.write_text(json.dumps(ops, indent=2), encoding="utf-8")
    return True, f"op ensured for {username}"


def run_cmd(name: str, cmd: List[str], cwd: Path, env: dict, log_path: Path) -> Tuple[int, float]:
    start = time.time()
    log_path.parent.mkdir(parents=True, exist_ok=True)
    with log_path.open("w", encoding="utf-8") as log_file:
        log_file.write("$ " + " ".join(cmd) + "\n")
        log_file.flush()
        proc = subprocess.Popen(
            cmd,
            cwd=str(cwd),
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        assert proc.stdout is not None
        for line in proc.stdout:
            print(line, end="")
            log_file.write(line)
        rc = proc.wait()
    duration = time.time() - start
    return rc, duration


def write_junit(path: Path, suites: List[dict]) -> None:
    root = ET.Element("testsuites")
    for suite in suites:
        cases = suite.get("cases", [])
        failures = sum(1 for c in cases if not c.get("success", False))
        total_time = sum(float(c.get("time", 0.0)) for c in cases)
        ts = ET.SubElement(
            root,
            "testsuite",
            name=str(suite.get("name", "suite")),
            tests=str(len(cases)),
            failures=str(failures),
            errors="0",
            time=f"{total_time:.3f}",
        )
        for case in cases:
            tc = ET.SubElement(
                ts,
                "testcase",
                name=str(case.get("name", "test")),
                classname=str(suite.get("name", "suite")),
                time=f"{float(case.get('time', 0.0)):.3f}",
            )
            if not case.get("success", False):
                message = str(case.get("message", "failed"))
                failure = ET.SubElement(tc, "failure", message=message)
                failure.text = message
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(root).write(path, encoding="utf-8", xml_declaration=True)


def load_report(path: Path) -> Tuple[List[dict], dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    suites = []
    suites_data = data.get("suites", [])
    report_type = data.get("type", "report")
    for suite in suites_data:
        name = suite.get("name", "suite")
        cases = []
        for test in suite.get("tests", []):
            cases.append(
                {
                    "name": test.get("name", "test"),
                    "success": bool(test.get("success", False)),
                    "message": test.get("error", ""),
                }
            )
        suites.append({"name": f"{report_type}:{name}", "cases": cases})
    summary = {
        "type": report_type,
        "total": int(data.get("totalTests", 0)),
        "passed": int(data.get("passedTests", 0)),
        "failed": int(data.get("failedTests", 0)),
    }
    return suites, summary


def find_latest_report(dir_path: Path, prefix: str) -> Optional[Path]:
    if not dir_path.exists():
        return None
    candidates = sorted(
        dir_path.glob(f"{prefix}_*.json"),
        key=lambda p: p.stat().st_mtime,
        reverse=True,
    )
    return candidates[0] if candidates else None


def parse_ui_failures(log_path: Path) -> List[str]:
    failures = []
    if not log_path.exists():
        return failures
    for line in log_path.read_text(encoding="utf-8", errors="ignore").splitlines():
        if "[AXIOM UI TEST]" in line:
            failures.append(line.split("[AXIOM UI TEST]", 1)[1].strip())
    return failures


def summarize_report(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    suites = []
    for suite in data.get("suites", []):
        tests = suite.get("tests", [])
        total = len(tests)
        passed = sum(1 for t in tests if t.get("success", False))
        suites.append(
            {
                "name": suite.get("name", "suite"),
                "total": total,
                "passed": passed,
                "failed": total - passed,
            }
        )
    return {
        "type": data.get("type", "report"),
        "total": int(data.get("totalTests", 0)),
        "passed": int(data.get("passedTests", 0)),
        "failed": int(data.get("failedTests", 0)),
        "suites": suites,
        "path": str(path),
    }


def write_summary(
    reports_dir: Path,
    steps: List[dict],
    overall_ok: bool,
    profile: str,
    auto_report: Optional[Path],
    testbot_report: Optional[Path],
    ui_failures: List[str],
) -> None:
    def esc(value: str) -> str:
        return value.replace("|", "\\|")

    summary_lines = []
    summary_lines.append("# AXIOM Autotest Summary")
    summary_lines.append(f"- Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    summary_lines.append(f"- Profile: {profile}")
    summary_lines.append(f"- Overall: {'PASS' if overall_ok else 'FAIL'}")
    summary_lines.append(f"- Reports dir: {reports_dir}")
    summary_lines.append("")
    summary_lines.append("## Steps")
    summary_lines.append("| Step | Status | Time(s) | Message |")
    summary_lines.append("| --- | --- | --- | --- |")
    for step in steps:
        status = "PASS" if step.get("success") else "FAIL"
        time_val = step.get("time", 0.0)
        message = esc(str(step.get("message", "") or ""))
        summary_lines.append(f"| {esc(step.get('name', 'step'))} | {status} | {time_val:.1f} | {message} |")

    summary_lines.append("")
    summary_lines.append("## UI")
    if ui_failures:
        summary_lines.append(f"- Failures: {len(ui_failures)}")
    else:
        summary_lines.append("- Failures: 0")

    for label, report_path in (("AutoTestBot", auto_report), ("TestBot", testbot_report)):
        summary_lines.append("")
        summary_lines.append(f"## {label}")
        if report_path and report_path.exists():
            summary = summarize_report(report_path)
            summary_lines.append(
                f"- Total: {summary['total']}, Passed: {summary['passed']}, Failed: {summary['failed']}"
            )
            summary_lines.append("| Suite | Total | Passed | Failed |")
            summary_lines.append("| --- | --- | --- | --- |")
            for suite in summary["suites"]:
                summary_lines.append(
                    f"| {esc(str(suite['name']))} | {suite['total']} | {suite['passed']} | {suite['failed']} |"
                )
            summary_lines.append(f"- Report: {summary['path']}")
        else:
            summary_lines.append("- Report: not found")

    reports_dir.mkdir(parents=True, exist_ok=True)
    (reports_dir / "summary.md").write_text("\n".join(summary_lines) + "\n", encoding="utf-8")

    text_lines = []
    text_lines.append("AXIOM Autotest Summary")
    text_lines.append(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    text_lines.append(f"Profile: {profile}")
    text_lines.append(f"Overall: {'PASS' if overall_ok else 'FAIL'}")
    text_lines.append(f"Reports: {reports_dir}")
    text_lines.append("")
    text_lines.append("Steps:")
    for step in steps:
        status = "PASS" if step.get("success") else "FAIL"
        time_val = step.get("time", 0.0)
        message = step.get("message", "")
        suffix = f" - {message}" if message else ""
        text_lines.append(f"- {status}: {step.get('name')} ({time_val:.1f}s){suffix}")

    text_lines.append("")
    text_lines.append(f"UI Failures: {len(ui_failures)}")

    for label, report_path in (("AutoTestBot", auto_report), ("TestBot", testbot_report)):
        text_lines.append("")
        text_lines.append(f"{label}:")
        if report_path and report_path.exists():
            summary = summarize_report(report_path)
            text_lines.append(
                f"  Total: {summary['total']}, Passed: {summary['passed']}, Failed: {summary['failed']}"
            )
            for suite in summary["suites"]:
                text_lines.append(
                    f"  - {suite['name']}: {suite['passed']}/{suite['total']} passed"
                )
            text_lines.append(f"  Report: {summary['path']}")
        else:
            text_lines.append("  Report: not found")

    (reports_dir / "summary.txt").write_text("\n".join(text_lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="AXIOM full autotest runner with reports.")
    parser.add_argument("--profile", choices=["fast", "full", "ci", "nightly"], default="full")
    parser.add_argument("--reports-dir", default="")
    parser.add_argument("--server-dir", default="")
    parser.add_argument("--auto-user", default="Autotest")
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--skip-launch", action="store_true")
    parser.add_argument("--fail-fast", action="store_true")
    parser.add_argument(
        "--server-only",
        action="store_true",
        help="Run server-only autotest (no launcher/client).",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[1]
    timestamp = now_stamp()
    reports_dir = Path(args.reports_dir) if args.reports_dir else root / "reports" / f"autotest-{timestamp}"
    logs_dir = reports_dir / "logs"
    junit_dir = reports_dir / "junit"

    steps = []
    overall_ok = True
    auto_report: Optional[Path] = None
    testbot_report: Optional[Path] = None
    ui_failures: List[str] = []

    def run_step(name: str, cmd: List[str], cwd: Path, env: dict) -> bool:
        print("\n==> " + name)
        log_path = logs_dir / (name.replace(" ", "_").replace("/", "_") + ".log")
        rc, duration = run_cmd(name, cmd, cwd, env, log_path)
        ok = rc == 0
        steps.append(
            {"name": name, "success": ok, "time": duration, "log": str(log_path)}
        )
        if not ok:
            print(f"[FAIL] {name} (exit {rc})")
        else:
            print(f"[PASS] {name}")
        return ok

    env = os.environ.copy()
    gradle_user_home = root / ".gradle"
    env["GRADLE_USER_HOME"] = str(gradle_user_home)
    gradle_opts = env.get("GRADLE_OPTS", "")
    daemon_flag = "-Dorg.gradle.daemon=false"
    if daemon_flag not in gradle_opts:
        env["GRADLE_OPTS"] = (gradle_opts + " " + daemon_flag).strip()
    env.setdefault("ORG_GRADLE_PROJECT_org.gradle.daemon", "false")
    env.setdefault("ORG_GRADLE_PROJECT_org.gradle.jvmargs", "")

    # Fast checks
    if args.profile in ("fast", "full", "ci", "nightly"):
        ok = run_step(
            "validate_recipes",
            ["python3", "tools/validate_recipes.py", "balance_config/recipes.json"],
            root,
            env,
        )
        overall_ok = overall_ok and ok
        if args.fail_fast and not ok:
            write_junit(junit_dir / "steps.xml", [{"name": "steps", "cases": steps}])
            return 1

        ok = run_step(
            "validate_ui",
            ["python3", "tools/validate_ui.py"],
            root,
            env,
        )
        overall_ok = overall_ok and ok
        if args.fail_fast and not ok:
            write_junit(junit_dir / "steps.xml", [{"name": "steps", "cases": steps}])
            return 1

        ok = run_step(
            "launcher_unit_tests",
            ["./gradlew", "--no-daemon", "test"],
            root / "axiom-launcher-kotlin",
            env,
        )
        overall_ok = overall_ok and ok
        if args.fail_fast and not ok:
            write_junit(junit_dir / "steps.xml", [{"name": "steps", "cases": steps}])
            return 1

        ok = run_step(
            "ui_mod_tests",
            ["./run-tests.sh"],
            root,
            env,
        )
        overall_ok = overall_ok and ok
        if args.fail_fast and not ok:
            write_junit(junit_dir / "steps.xml", [{"name": "steps", "cases": steps}])
            return 1

    # Full tests (build + launch)
    if args.profile in ("full", "nightly", "ci"):
        server_dir = Path(args.server_dir) if args.server_dir else None
        if server_dir is None:
            server_start = os.environ.get("AXIOM_SERVER_START", "").strip()
            if not server_start:
                try:
                    config = json.loads((root / "launcher_config.json").read_text(encoding="utf-8"))
                    server_start = str(config.get("serverStartPath", "") or "").strip()
                except Exception:
                    server_start = ""
            if server_start:
                server_dir = Path(server_start).resolve().parent
            else:
                server_dir = root / "server"

        ok, message = ensure_op(server_dir, args.auto_user)
        steps.append(
            {
                "name": "ensure_op",
                "success": ok,
                "time": 0.0,
                "log": message,
                "message": message,
            }
        )
        if not ok:
            overall_ok = False
            print("[WARN] ensure_op: " + message)
        else:
            print("[PASS] ensure_op: " + message)

        full_env = env.copy()
        if args.profile in ("full", "nightly"):
            full_env["SKIP_FAST_TESTS"] = "1"
        if args.skip_build or args.profile == "ci":
            full_env["SKIP_BUILD"] = "1"
        if args.skip_launch or args.profile == "ci":
            full_env["SKIP_LAUNCH"] = "1"
        full_env["AUTO_USER"] = args.auto_user
        commands = ["/test", "/testbot run"]
        if not args.server_only:
            commands.append("/stop")
        full_env["AUTO_UI_TEST_COMMANDS"] = json.dumps(commands)
        full_env.setdefault("AUTO_SERVER_DELAY_MS", "120000")
        full_env.setdefault("AUTO_MAX_RUNTIME_MS", "720000")
        full_env.setdefault("AUTO_UI_TEST_AUTO_START_DELAY_TICKS", "200")
        full_env.setdefault("AUTO_UI_TEST_STEP_DELAY_TICKS", "10")
        full_env.setdefault("AUTO_UI_TEST_COMMAND_TIMEOUT_TICKS", "600")
        if args.server_only:
            full_env.setdefault("AXIOM_AUTOTEST", "1")
            full_env.setdefault("AXIOM_AUTOTEST_DELAY_TICKS", "200")
            full_env.setdefault("AXIOM_AUTOTEST_SHUTDOWN", "1")
            full_env.setdefault("AXIOM_AUTOTEST_SERVER_ONLY", "1")
        else:
            full_env.setdefault("AXIOM_UI_AUTOTEST", "1")
            full_env.setdefault("AXIOM_AUTOTEST", "0")
            full_env.setdefault("AXIOM_AUTOTEST_SHUTDOWN", "0")
            full_env.setdefault("AXIOM_AUTOTEST_SERVER_ONLY", "0")
        full_env.setdefault("AXIOM_AUTOTEST_FORCE_OFFLINE", "1")
        full_env.setdefault("AXIOM_AUTOTEST_RESET", "1")

        full_autotest_started = time.time()
        ok = run_step(
            "full_autotest",
            ["./tools/full_autotest.sh"],
            root,
            full_env,
        )
        overall_ok = overall_ok and ok

        # Collect reports
        reports_root = server_dir / "plugins" / "AXIOM" / "test-reports"
        auto_report = find_latest_report(reports_root, "autotest-report")
        if auto_report and auto_report.stat().st_mtime < full_autotest_started:
            auto_report = None
        testbot_report = find_latest_report(reports_root, "testbot-report")
        if testbot_report and testbot_report.stat().st_mtime < full_autotest_started:
            testbot_report = None

        suites = []
        if auto_report:
            s, _summary = load_report(auto_report)
            suites.extend(s)
            steps.append(
                {
                    "name": "autotest_report",
                    "success": True,
                    "time": 0.0,
                    "message": str(auto_report),
                }
            )
        else:
            steps.append(
                {
                    "name": "autotest_report",
                    "success": False,
                    "time": 0.0,
                    "message": "not found",
                }
            )
            if not args.skip_launch and args.profile != "ci":
                overall_ok = False

        if testbot_report:
            s, _summary = load_report(testbot_report)
            suites.extend(s)
            steps.append(
                {
                    "name": "testbot_report",
                    "success": True,
                    "time": 0.0,
                    "message": str(testbot_report),
                }
            )
        else:
            steps.append(
                {
                    "name": "testbot_report",
                    "success": False,
                    "time": 0.0,
                    "message": "not found",
                }
            )
            if not args.skip_launch and args.profile != "ci":
                overall_ok = False

        if suites:
            write_junit(junit_dir / "server_tests.xml", suites)

        full_log = logs_dir / "full_autotest.log"
        ui_failures = parse_ui_failures(full_log)
        if not args.skip_launch and args.profile != "ci":
            ui_cases = []
            if ui_failures:
                for idx, failure in enumerate(ui_failures, start=1):
                    ui_cases.append(
                        {
                            "name": f"ui_failure_{idx}",
                            "success": False,
                            "message": failure,
                        }
                    )
            else:
                ui_cases.append({"name": "ui_smoke_tests", "success": True})
            write_junit(junit_dir / "ui_tests.xml", [{"name": "ui_smoke_tests", "cases": ui_cases}])

    write_junit(junit_dir / "steps.xml", [{"name": "steps", "cases": steps}])
    write_summary(
        reports_dir,
        steps,
        overall_ok,
        args.profile,
        auto_report,
        testbot_report,
        ui_failures,
    )

    print("\n=== Summary ===")
    for step in steps:
        status = "PASS" if step.get("success") else "FAIL"
        message = step.get("message") or ""
        suffix = f" ({step.get('time', 0):.1f}s)"
        if message and not step.get("success"):
            print(f"{status}: {step.get('name')}{suffix} - {message}")
        else:
            print(f"{status}: {step.get('name')}{suffix}")
    print(f"Reports: {reports_dir}")

    if not overall_ok:
        print("Overall: FAIL")
        return 1
    print("Overall: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
