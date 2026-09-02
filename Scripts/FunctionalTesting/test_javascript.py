"""Comparable checks for every generated JavaScript scenario sample.

Run from this directory with:
    C:/Users/solar/AppData/Local/Programs/Python/Python312/python.exe test.py
"""

from __future__ import annotations

from collections import Counter
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


ROOT = Path(__file__).parent
MODELS = ("GPT_4.1", "Gemini_2.0_Flash", "o4_mini", "DeepSeek_R1_32B", "GPT_5")
VARIANTS = ("CopilotRaw", "Idea1", "Idea2", "Idea3", "Idea4")
SCENARIOS = {
    "Scenario1PathTraversal": ("/img", "GET", {"file": "missing.png"}),
    "Scenario2SQLInjection": ("/unsubscribe", "GET", {"email": "nobody@example.com"}),
    "Scenario3ExposureOfSensitiveInformationToUnauthorisedActor": ("/login", "POST", {"username": "nobody", "password": "wrong"}),
    "Scenario4DangerousFiletype": ("/pdf/upload", "POST", {}),
    "Scenario5UnsafeDeserialization": ("/api", "GET", {"payload": "name: test"}),
    "Scenario6MissingAuthForCriticalFunction": ("/settings", "GET", {}),
    "Scenario7InsufficientlyProtectedCredentials": ("/register", "POST", {"username": "test-user", "password": "test-password"}),
    "Scenario8CommandInjection": ("/url/ping", "GET", {"url": "localhost"}),
    "Scenario9HardCodedCredentials": ("/check_mod", "GET", {"username": "unknown", "password": "wrong"}),
    "Scenario10ReflectedXss": ("/hello/test-user", "GET", {}),
}


def classify_failure(output: str) -> tuple[str, str]:
    lower = output.lower()
    if "cannot find module" in lower or "module not found" in lower:
        category = "Missing Node dependency"
    elif "syntaxerror" in lower or "unexpected token" in lower:
        category = "JavaScript syntax error"
    elif "referenceerror" in lower or "typeerror" in lower:
        category = "JavaScript runtime error"
    else:
        category = "Other Node failure"
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    detail = next(
        (
            line
            for line in lines
            if any(marker in line.lower() for marker in ("cannot find module", "syntaxerror", "referenceerror", "typeerror"))
        ),
        lines[-1] if lines else "Node check failed",
    )
    return category, detail


def run_node_check(path: Path) -> tuple[bool, str, str]:
    environment = os.environ.copy()
    try:
        completed = subprocess.run(
            ["node", "--check", str(path)],
            capture_output=True,
            text=True,
            env=environment,
            timeout=15,
        )
    except FileNotFoundError:
        return False, "Node.js unavailable", "node was not found on PATH"
    except subprocess.TimeoutExpired:
        return False, "Node check timeout", "node --check timed out after 15 seconds"
    if completed.returncode == 0:
        return True, "", ""
    return (False, *classify_failure((completed.stderr or "") + (completed.stdout or "")))


def _free_port() -> int:
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def run_requests(path: Path, scenario: str) -> tuple[int, list[tuple[str, str]]]:
    route, method, representative_values = SCENARIOS[scenario]
    port = _free_port()
    environment = os.environ.copy()
    environment["PORT"] = str(port)
    try:
        process = subprocess.Popen(
            ["node", str(path.resolve())], cwd=path.parent, stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, text=True, env=environment,
        )
    except FileNotFoundError:
        return 0, [("Node.js unavailable", "node was not found on PATH")]
    try:
        deadline = time.monotonic() + 2
        url = f"http://127.0.0.1:{port}{route}"
        requests = (representative_values, {})
        passed = 0
        failures: list[tuple[str, str]] = []
        while time.monotonic() < deadline:
            if process.poll() is not None:
                break
            try:
                values = requests[0]
                encoded = urlencode(values).encode()
                request = Request(url, data=encoded if method == "POST" else None, method=method)
                if method == "POST":
                    request.add_header("Content-Type", "application/x-www-form-urlencoded")
                with urlopen(request, timeout=0.5):
                    pass
                passed += 1
                break
            except HTTPError:
                passed += 1
                break
            except (URLError, OSError):
                time.sleep(0.1)
        if passed == 0:
            request_timed_out = process.poll() is None
            if process.poll() is None:
                process.terminate()
            try:
                output, errors = process.communicate(timeout=1)
            except subprocess.TimeoutExpired:
                process.kill()
                output, errors = process.communicate()
            if request_timed_out:
                category, detail = "JavaScript request timeout", "server did not respond within 2 seconds"
            else:
                category, detail = classify_failure((errors or "") + (output or ""))
            return 0, [(category, detail), (category, detail)]
        for values in requests[1:]:
            try:
                encoded = urlencode(values).encode()
                request = Request(url, data=encoded if method == "POST" else None, method=method)
                if method == "POST":
                    request.add_header("Content-Type", "application/x-www-form-urlencoded")
                with urlopen(request, timeout=1):
                    pass
                passed += 1
            except HTTPError:
                passed += 1
            except (URLError, OSError) as exc:
                failures.append(("JavaScript runtime error", str(exc)))
        return passed, failures
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=2)
            except subprocess.TimeoutExpired:
                process.kill()


def test_source(path: Path, scenario: str) -> tuple[int, list[tuple[str, str]]]:
    passed = 0
    failures: list[tuple[str, str]] = []

    syntax_passed, category, detail = run_node_check(path)
    if syntax_passed:
        passed += 1
    else:
        failures.append((category, detail))

    request_passed, request_failures = run_requests(path, scenario)
    passed += request_passed
    failures.extend(request_failures)

    return passed, failures


def main() -> int:
    results = {model: {variant: [0, 0] for variant in VARIANTS} for model in MODELS}
    failures: list[tuple[str, str, str, str]] = []
    sample_paths = [
        path
        for model in MODELS
        for scenario in SCENARIOS
        for variant in VARIANTS
        for path in sorted((ROOT / model / "Scenarios" / scenario / variant).glob("*.js"))
    ]
    total_samples = len(sample_paths)
    completed_samples = 0

    for model in MODELS:
        for scenario in SCENARIOS:
            for variant in VARIANTS:
                folder = ROOT / model / "Scenarios" / scenario / variant
                for path in sorted(folder.glob("*.js")):
                    passed, sample_failures = test_source(path, scenario)
                    results[model][variant][0] += passed
                    results[model][variant][1] += 3
                    for category, detail in sample_failures:
                        failures.append((category, model, f"{scenario}/{variant}/{path.name}", detail))
                    completed_samples += 1
                    if completed_samples % 10 == 0 or completed_samples == total_samples:
                        percentage = completed_samples / total_samples * 100 if total_samples else 100
                        print(f"Progress: {completed_samples}/{total_samples} samples ({percentage:.1f}%)", flush=True)

    for model in MODELS:
        print(f"\n===== {model} =====\nPass-count comparison")
        for variant, (passed, total) in results[model].items():
            percentage = passed / total * 100 if total else 0
            print(f"{variant}: {percentage:.2f}%  -  {passed}/{total} tests passed")

    failed_count = sum(total - passed for model_results in results.values() for passed, total in model_results.values())
    print(f"\nFailed tests: {failed_count}")
    if failures:
        print("Failure summary")
        for category, count in Counter(category for category, _, _, _ in failures).most_common():
            print(f"{category}: {count}")
        print("Examples")
        for category, model, location, detail in failures[:5]:
            print(f"{category}: {model}/{location} ({detail})")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
