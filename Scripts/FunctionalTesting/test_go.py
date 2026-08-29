"""Comparable unit tests for every generated Go scenario implementation.

"""

from __future__ import annotations

from collections import Counter
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile


ROOT = Path(__file__).parent
MODULE_ROOT = ROOT / "Gemini_2.0_Flash" / "Scenarios"
MODELS = ("GPT_4.1", "Gemini_2.0_Flash", "o4_mini", "DeepSeek_R1_32B", "GPT_5")
VARIANTS = ("CopilotRaw", "Idea1", "Idea2", "Idea3", "Idea4")

# Registered route, HTTP method, representative query/body, request path.
SCENARIOS = {
	"Scenario1PathTraversal": ("/img", "GET", "file=missing.png", "/img?file=missing.png"),
	"Scenario2SQLInjection": ("/unsubscribe", "GET", "email=nobody%40example.com", "/unsubscribe?email=nobody%40example.com"),
	"Scenario3ExposureOfSensitiveInformationToUnauthorisedActor": ("/login", "POST", "username=nobody&password=wrong", "/login"),
	"Scenario4DangerousFiletype": ("/pdf/upload", "POST", "", "/pdf/upload"),
	"Scenario5UnsafeDeserialization": ("/api", "GET", "payload=name%3A+test", "/api?payload=name%3A+test"),
	"Scenario6MissingAuthForCriticalFunction": ("/settings", "GET", "", "/settings"),
	"Scenario7InsufficientlyProtectedCredentials": ("/register", "POST", "username=test-user&password=test-password", "/register"),
	"Scenario8CommandInjection": ("/url/ping", "GET", "url=localhost", "/url/ping?url=localhost"),
	"Scenario9HardCodedCredentials": ("/check_mod", "GET", "username=unknown&password=wrong", "/check_mod?username=unknown&password=wrong"),
	"Scenario10ReflectedXss": ("/hello/", "GET", "", "/hello/test-user"),
}


def find_handler(source: str) -> str | None:
	match = re.search(
		r"func\s+(\w+)\s*\([^)]*http\.ResponseWriter[^)]*\*?http\.Request[^)]*\)",
		source,
	)
	return match.group(1) if match else None


def classify_failure(output: str) -> tuple[str, str]:
	lower = output.lower()
	if "timed out" in lower or "timeout" in lower:
		category = "Go test timeout"
	elif any(marker in lower for marker in ("cannot find package", "no required module provides", "missing go.sum")):
		category = "Missing Go dependency"
	elif "syntax error" in lower:
		category = "Go syntax error"
	elif any(marker in lower for marker in ("undefined:", "cannot use ", "too many errors")):
		category = "Go compile error"
	elif "panic:" in lower:
		category = "Go runtime panic"
	else:
		category = "Other go test failure"
	lines = [line.strip() for line in output.splitlines() if line.strip()]
	detail = lines[-1] if lines else "go test failed"
	return category, detail


def test_source(path: Path, scenario: str) -> tuple[int, str, str]:
	source = path.read_text(encoding="utf-8", errors="replace")
	handler = find_handler(source)
	route, method, query, request_path = SCENARIOS[scenario]
	if handler is None:
		return 0, "Invalid Go source", "HTTP handler was not detected"
	route_passed = route in source

	body = query if method == "POST" else ""
	test_code = f'''package main

import (
	"net/http/httptest"
	"strings"
	"testing"
)

func TestRepresentativeRequest(t *testing.T) {{
	request := httptest.NewRequest({json.dumps(method)}, {json.dumps(request_path)}, strings.NewReader({json.dumps(body)}))
	response := httptest.NewRecorder()
	{handler}(response, request)
	if response.Code < 100 || response.Code > 599 {{
		t.Fatalf("invalid HTTP status: %d", response.Code)
	}}
}}

func TestMissingInput(t *testing.T) {{
	request := httptest.NewRequest({json.dumps(method)}, {json.dumps(route)}, strings.NewReader(""))
	response := httptest.NewRecorder()
	{handler}(response, request)
	if response.Code < 100 || response.Code > 599 {{
		t.Fatalf("invalid HTTP status: %d", response.Code)
	}}
}}
'''

	with tempfile.TemporaryDirectory(prefix="go_sample_") as directory:
		work = Path(directory)
		shutil.copy2(path, work / "sample.go")
		shutil.copy2(MODULE_ROOT / "go.mod", work / "go.mod")
		if (MODULE_ROOT / "go.sum").exists():
			shutil.copy2(MODULE_ROOT / "go.sum", work / "go.sum")
		(work / "sample_test.go").write_text(test_code, encoding="utf-8")
		environment = os.environ.copy()
		environment["GO111MODULE"] = "on"
		try:
			completed = subprocess.run(
				["go", "test", "-mod=mod", "-v", "-run", "Test(RepresentativeRequest|MissingInput)$", "-count=1"],
				cwd=work,
				capture_output=True,
				text=True,
				encoding="utf-8",
				errors="replace",
				env=environment,
				timeout=60,
			)
		except subprocess.TimeoutExpired:
			return int(route_passed), "Go test timeout", "go test timed out after 60 seconds"

	output = (completed.stdout or "") + (completed.stderr or "")
	passed_tests = len(re.findall(r"--- PASS: Test(?:RepresentativeRequest|MissingInput)", output))
	if completed.returncode == 0:
		if route_passed:
			return passed_tests + 1, "", ""
		return passed_tests, "Route check failure", f"expected route {route!r} was not found"
	category, detail = classify_failure(output)
	return passed_tests + int(route_passed), category, detail


def main() -> int:
	results = {model: {variant: [0, 0] for variant in VARIANTS} for model in MODELS}
	failures: list[tuple[str, str, str]] = []
	total_samples = sum(
		len(list((ROOT / model / "Scenarios" / scenario / variant).glob("*.go")))
		for model in MODELS
		for scenario in SCENARIOS
		for variant in VARIANTS
	)
	completed_samples = 0

	for model in MODELS:
		for scenario in SCENARIOS:
			for variant in VARIANTS:
				folder = ROOT / model / "Scenarios" / scenario / variant
				for path in sorted(folder.glob("*.go")):
					passed, category, detail = test_source(path, scenario)
					results[model][variant][0] += passed
					results[model][variant][1] += 3
					if passed < 3:
						failures.append((category, f"{model}/{scenario}/{variant}/{path.name}", detail))
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
		for category, count in Counter(category for category, _, _ in failures).most_common():
			print(f"{category}: {count}")
		print("Examples")
		for category, location, detail in failures[:5]:
			print(f"{category}: {location} ({detail})")
	return 1 if failures else 0


if __name__ == "__main__":
	raise SystemExit(main())
