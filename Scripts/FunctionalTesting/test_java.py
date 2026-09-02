"""Comparable checks for every generated Java scenario sample.

Run from this directory with:
    C:/Users/solar/AppData/Local/Programs/Python/Python312/python.exe test.py
"""

from __future__ import annotations

from collections import Counter
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile


ROOT = Path(__file__).parent
MODELS = ("GPT_4.1", "Gemini_2.0_Flash", "o4_mini", "DeepSeek_R1_32B", "GPT_5")
VARIANTS = ("CopilotRaw", "Idea1", "Idea2", "Idea3", "Idea4")
SCENARIOS = {
    "Scenario1PathTraversal": ("/img", "file"),
    "Scenario2SQLInjection": ("/unsubscribe", "email"),
    "Scenario3ExposureOfSensitiveInformationToUnauthorisedActor": ("/login", "username"),
    "Scenario4DangerousFiletype": ("/pdf/upload", "pdf"),
    "Scenario5UnsafeDeserialization": ("/api", "payload"),
    "Scenario6MissingAuthForCriticalFunction": ("/settings", "session"),
    "Scenario7InsufficientlyProtectedCredentials": ("/register", "username"),
    "Scenario8CommandInjection": ("/url/ping", "url"),
    "Scenario9HardCodedCredentials": ("/check_mod", "username"),
    "Scenario10ReflectedXss": ("/hello", "username"),
}
HTTP_METHODS = {
    "Scenario1PathTraversal": "doGet",
    "Scenario2SQLInjection": "doGet",
    "Scenario3ExposureOfSensitiveInformationToUnauthorisedActor": "doPost",
    "Scenario4DangerousFiletype": "doPost",
    "Scenario5UnsafeDeserialization": "doGet",
    "Scenario6MissingAuthForCriticalFunction": "doGet",
    "Scenario7InsufficientlyProtectedCredentials": "doPost",
    "Scenario8CommandInjection": "doGet",
    "Scenario9HardCodedCredentials": "doGet",
    "Scenario10ReflectedXss": "doGet",
}


def classify_failure(output: str) -> tuple[str, str]:
    lower = output.lower()
    if "package javax.servlet" in lower or "package jakarta.servlet" in lower or "cannot find symbol" in lower and "class" in lower:
        category = "Missing Java dependency"
    elif "cannot find symbol" in lower or "incompatible types" in lower or "';' expected" in lower:
        category = "Java compile error"
    elif "error:" in lower:
        category = "Java syntax or compile error"
    else:
        category = "Other javac failure"
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    detail = next((line for line in lines if "error:" in line.lower()), lines[-1] if lines else "javac failed")
    return category, detail


def compile_source(path: Path) -> tuple[bool, str, str]:
    output_dir = Path(tempfile.mkdtemp(prefix="java_sample_"))
    classpath = str(ROOT / "lib" / "*")
    try:
        completed = subprocess.run(
            ["javac", "-cp", classpath, "-d", str(output_dir), str(path)],
            capture_output=True,
            text=True,
            env=os.environ.copy(),
            timeout=15,
        )
    except FileNotFoundError:
        return False, "Java unavailable", "javac was not found on PATH"
    except subprocess.TimeoutExpired:
        return False, "javac timeout", "javac timed out after 15 seconds"
    finally:
        shutil.rmtree(output_dir, ignore_errors=True)
    if completed.returncode == 0:
        return True, "", ""
    category, detail = classify_failure((completed.stderr or "") + (completed.stdout or ""))
    return False, category, detail


SERVLET_HARNESS = r'''import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class RuntimeCheck {
    static Object request(String method, Map<String, String> values) {
        return Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
            new Class<?>[]{HttpServletRequest.class}, (proxy, call, args) -> {
                if (call.getName().equals("getParameter")) return values.get(args[0]);
                if (call.getName().equals("getMethod")) return method;
                if (call.getName().equals("getSession")) return null;
                if (call.getName().equals("getServletContext")) return null;
                return defaultValue(call.getReturnType());
            });
    }

    static Object response() {
        StringWriter text = new StringWriter();
        return Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
            new Class<?>[]{HttpServletResponse.class}, (proxy, call, args) -> {
                if (call.getName().equals("getWriter")) return new PrintWriter(text, true);
                if (call.getName().equals("getOutputStream")) return new ServletOutputStream() {
                    public void write(int value) { text.write(value); }
                    public boolean isReady() { return true; }
                    public void setWriteListener(WriteListener listener) { }
                };
                return defaultValue(call.getReturnType());
            });
    }

    static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        return null;
    }

    public static void main(String[] args) throws Exception {
        Class<?> servletClass = Class.forName(args[0]);
        Object servlet = servletClass.getDeclaredConstructor().newInstance();
        Method handler = servletClass.getDeclaredMethod(args[1], HttpServletRequest.class, HttpServletResponse.class);
        handler.setAccessible(true);
        Map<String, String> values = new HashMap<>();
        if (args.length > 3) values.put(args[2], args[3]);
        handler.invoke(servlet, request(args[1].equals("doPost") ? "POST" : "GET", values), response());
        handler.invoke(servlet, request(args[1].equals("doPost") ? "POST" : "GET", new HashMap<>()), response());
    }
}
'''


def servlet_target(source: str, expected_method: str) -> tuple[str | None, str | None]:
    outer_match = re.search(r"\bpublic\s+class\s+(\w+)", source)
    servlet_match = re.search(r"\bclass\s+(\w+)\s+extends\s+HttpServlet", source)
    if outer_match is None or servlet_match is None:
        return None, None
    outer = outer_match.group(1)
    inner = servlet_match.group(1)
    target = outer if inner == outer else f"{outer}${inner}"
    method = expected_method if re.search(rf"\b{expected_method}\s*\(", source) else None
    if method is None:
        alternate = "doPost" if expected_method == "doGet" else "doGet"
        method = alternate if re.search(rf"\b{alternate}\s*\(", source) else None
    return target, method


def run_servlet(path: Path, scenario: str) -> tuple[bool, str, str]:
    source = path.read_text(encoding="utf-8", errors="replace")
    expected_method = HTTP_METHODS[scenario]
    target, method = servlet_target(source, expected_method)
    if target is None or method is None:
        return False, "Servlet handler unavailable", "No executable doGet/doPost servlet handler found"
    input_name = SCENARIOS[scenario][1]
    with tempfile.TemporaryDirectory(prefix="java_runtime_") as directory:
        work = Path(directory)
        harness = work / "RuntimeCheck.java"
        harness.write_text(SERVLET_HARNESS, encoding="utf-8")
        output_dir = work / "classes"
        output_dir.mkdir()
        classpath = str(ROOT / "lib" / "*")
        try:
            compiled = subprocess.run(
                ["javac", "-cp", classpath, "-d", str(output_dir), str(path), str(harness)],
                capture_output=True, text=True, env=os.environ.copy(), timeout=15,
            )
            if compiled.returncode != 0:
                return False, *classify_failure((compiled.stderr or "") + (compiled.stdout or ""))
            runtime_classpath = str(output_dir) + os.pathsep + classpath
            value = "test-value"
            completed = subprocess.run(
                ["java", "-cp", runtime_classpath, "RuntimeCheck", target, method, input_name, value],
                capture_output=True, text=True, env=os.environ.copy(), timeout=15,
            )
        except FileNotFoundError:
            return False, "Java unavailable", "javac or java was not found on PATH"
        except subprocess.TimeoutExpired:
            return False, "Java runtime timeout", "Java servlet test timed out after 15 seconds"
    if completed.returncode == 0:
        return True, "", ""
    return False, *classify_failure((completed.stderr or "") + (completed.stdout or ""))


def test_source(path: Path, scenario: str) -> tuple[int, list[tuple[str, str]]]:
    source = path.read_text(encoding="utf-8", errors="replace")
    route, input_name = SCENARIOS[scenario]
    passed = 0
    failures: list[tuple[str, str]] = []

    compiled, category, detail = compile_source(path)
    if compiled:
        passed += 1
    else:
        failures.append((category, detail))

    _, method = servlet_target(source, HTTP_METHODS[scenario])
    if method is not None:
        passed += 1
    else:
        failures.append(("Servlet handler unavailable", f"expected {HTTP_METHODS[scenario]} was not found"))

    runtime_passed, category, detail = run_servlet(path, scenario)
    if runtime_passed:
        passed += 1
    else:
        failures.append((category, detail))

    return passed, failures


def main() -> int:
    results = {model: {variant: [0, 0] for variant in VARIANTS} for model in MODELS}
    failures: list[tuple[str, str, str, str]] = []
    sample_paths = [
        path
        for model in MODELS
        for scenario in SCENARIOS
        for variant in VARIANTS
        for path in sorted((ROOT / model / "Scenarios" / scenario / variant).glob("*.java"))
    ]
    total_samples = len(sample_paths)
    completed_samples = 0

    for model in MODELS:
        for scenario in SCENARIOS:
            for variant in VARIANTS:
                folder = ROOT / model / "Scenarios" / scenario / variant
                for path in sorted(folder.glob("*.java")):
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
