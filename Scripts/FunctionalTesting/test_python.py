
"""
Comparable smoke tests for generated Python scenario implementations.

Run modes:

    RUN_MODE = "single"
        Evaluate only SINGLE_MODEL / SINGLE_VARIANT.

    RUN_MODE = "all"
        Evaluate all configured models and variants.

Each generated sample receives three checks:

    1. Import check
    2. Route check
    3. Representative request check

The evaluator intentionally performs no dependency installation or source
repair. Python samples are imported and tested as generated.

Environment variables:
    Before importing a sample, the evaluator statically detects environment
    variable names used by the generated source and temporarily provides
    dummy values for them.

    This does NOT modify the user's permanent Windows environment variables.

Expected directory layout:

    MODEL/
        Scenarios/
            Scenario1PathTraversal/
                CopilotRaw/
                    sample.py
                Idea1/
                    sample.py
                ...
"""


from __future__ import annotations

from collections import Counter
import ast
from contextlib import contextmanager
import importlib.util
import os
import re
import sys
from pathlib import Path
from types import ModuleType


ROOT = Path(__file__).parent


# ===========================================================================
# RUN MODE
# ===========================================================================

RUN_MODE = "all"

SINGLE_MODEL = "GPT_5"
SINGLE_VARIANT = "Idea2"


MODELS = (
    "GPT_4.1",
    "Gemini_2.0_Flash",
    "o4_mini",
    "DeepSeek_R1_32B",
    "GPT_5",
)


VARIANTS = (
    "CopilotRaw",
    "Idea1",
    "Idea2",
    "Idea3",
    "Idea4",
)


# ===========================================================================
# SCENARIOS
# ===========================================================================

SCENARIOS = {
    "Scenario1PathTraversal": {
        "route": "/img",
        "method": "GET",
        "query": {
            "file": "missing.png",
        },
    },

    "Scenario2SQLInjection": {
        "route": "/unsubscribe",
        "method": "GET",
        "query": {
            "email": "nobody@example.com",
        },
    },

    "Scenario3ExposureOfSensitiveInformationToUnauthorisedActor": {
        "route": "/login",
        "method": "POST",
        "body": {
            "username": "nobody",
            "password": "wrong",
        },
    },

    "Scenario4DangerousFiletype": {
        "route": "/pdf/upload",
        "method": "POST",
        "body": {},
    },

    "Scenario5UnsafeDeserialization": {
        "route": "/api",
        "method": "GET",
        "query": {
            "payload": "name: test",
        },
    },

    "Scenario6MissingAuthForCriticalFunction": {
        "route": "/settings",
        "method": "GET",
    },

    "Scenario7InsufficientlyProtectedCredentials": {
        "route": "/register",
        "method": "POST",
        "body": {
            "username": "test-user",
            "password": "test-password",
        },
    },

    "Scenario8PythonCommandInjection": {
        "route": "/url/ping",
        "method": "GET",
        "query": {
            "url": "localhost",
        },
    },

    "Scenario9HardCodedCredentials": {
        "route": "/check_mod",
        "method": "GET",
        "query": {
            "username": "unknown",
            "password": "wrong",
        },
    },

    "Scenario10ReflectedXss": {
        "route": "/hello/<username>",
        "method": "GET",
    },
}


# ===========================================================================
# TEST ENVIRONMENT VALUES
# ===========================================================================

# These are deliberately fake values.
#
# They exist only so generated programs which expect configuration from
# environment variables can be imported and exercised.
#
# If a generated sample accesses an environment variable that is not in this
# dictionary, a generic value is automatically generated for it.

ENVIRONMENT_VALUES = {
    # Common database configuration
    "DB_HOST": "127.0.0.1",
    "DB_PORT": "3306",
    "DB_USER": "test_user",
    "DB_USERNAME": "test_user",
    "DB_PASSWORD": "test_password",
    "DB_NAME": "test_database",
    "DATABASE_HOST": "127.0.0.1",
    "DATABASE_PORT": "3306",
    "DATABASE_USER": "test_user",
    "DATABASE_USERNAME": "test_user",
    "DATABASE_PASSWORD": "test_password",
    "DATABASE_NAME": "test_database",
    "MYSQL_HOST": "127.0.0.1",
    "MYSQL_PORT": "3306",
    "MYSQL_USER": "test_user",
    "MYSQL_USERNAME": "test_user",
    "MYSQL_PASSWORD": "test_password",
    "MYSQL_DATABASE": "test_database",

    # Common application configuration
    "SECRET_KEY": "test-secret-key",
    "FLASK_SECRET_KEY": "test-secret-key",
    "API_KEY": "test-api-key",
    "SECRET": "test-secret",
    "TOKEN": "test-token",

    # Common authentication configuration
    "USERNAME": "test-user",
    "PASSWORD": "test-password",

    # Common URLs
    "DATABASE_URL": "mysql://test_user:test_password@127.0.0.1:3306/test_database",
    "DB_URL": "mysql://test_user:test_password@127.0.0.1:3306/test_database",
}


def default_environment_value(name: str) -> str:
    """
    Generate a harmless non-empty value for an environment variable that
    the evaluator has not seen before.

    This is intentionally generic. The goal is to allow configuration
    validation to proceed without providing real secrets or credentials.
    """

    upper = name.upper()

    if "PASSWORD" in upper:
        return "test-password"

    if "PASS" in upper:
        return "test-password"

    if "SECRET" in upper:
        return "test-secret"

    if "TOKEN" in upper:
        return "test-token"

    if "API_KEY" in upper or upper.endswith("_KEY"):
        return "test-api-key"

    if "PORT" in upper:
        return "3306"

    if "HOST" in upper:
        return "127.0.0.1"

    if "USER" in upper:
        return "test-user"

    if "USERNAME" in upper:
        return "test-user"

    if "DATABASE" in upper and upper.endswith("_URL"):
        return (
            "mysql://test_user:test_password@"
            "127.0.0.1:3306/test_database"
        )

    if upper.endswith("_URL"):
        return "http://127.0.0.1"

    return "test-value"


# ===========================================================================
# ENVIRONMENT VARIABLE DETECTION
# ===========================================================================

def _attribute_chain(node: ast.AST) -> list[str] | None:
    """
    Convert an AST attribute expression into a list.

    Example:

        os.environ.get

    becomes:

        ["os", "environ", "get"]
    """

    parts = []

    while isinstance(node, ast.Attribute):
        parts.append(node.attr)
        node = node.value

    if isinstance(node, ast.Name):
        parts.append(node.id)
        return list(reversed(parts))

    return None


def detect_environment_variables(path: Path) -> set[str]:
    """
    Detect statically referenced environment variable names in a Python file.

    Handles common forms including:

        os.getenv("NAME")
        os.environ.get("NAME")
        os.environ["NAME"]

        getenv("NAME")
        environ.get("NAME")
        environ["NAME"]

    Only literal string variable names can be detected automatically.
    """

    try:
        source = path.read_text(
            encoding="utf-8",
            errors="replace",
        )
    except OSError:
        return set()

    names: set[str] = set()

    # ------------------------------------------------------------------
    # AST detection
    # ------------------------------------------------------------------

    try:
        tree = ast.parse(
            source,
            filename=str(path),
        )
    except SyntaxError:
        tree = None

    if tree is not None:

        # Track aliases such as:
        #
        # from os import getenv
        # from os import environ
        #
        getenv_names = {"getenv"}
        environ_names = {"environ"}

        for node in ast.walk(tree):

            if isinstance(node, ast.ImportFrom):

                if node.module == "os":

                    for alias in node.names:

                        if alias.name == "getenv":
                            getenv_names.add(alias.asname or alias.name)

                        elif alias.name == "environ":
                            environ_names.add(alias.asname or alias.name)

            # ----------------------------------------------------------
            # os.getenv("NAME")
            # ----------------------------------------------------------

            if isinstance(node, ast.Call):

                function = _attribute_chain(node.func)

                if function in (
                    ["os", "getenv"],
                ):

                    if node.args:
                        argument = node.args[0]

                        if isinstance(argument, ast.Constant):
                            if isinstance(argument.value, str):
                                names.add(argument.value)

                elif (
                    function is not None
                    and len(function) == 3
                    and function[0] == "os"
                    and function[1] == "environ"
                    and function[2] in {
                        "get",
                        "setdefault",
                    }
                ):

                    if node.args:
                        argument = node.args[0]

                        if isinstance(argument, ast.Constant):
                            if isinstance(argument.value, str):
                                names.add(argument.value)

                # getenv("NAME")
                elif (
                    isinstance(node.func, ast.Name)
                    and node.func.id in getenv_names
                ):

                    if node.args:
                        argument = node.args[0]

                        if isinstance(argument, ast.Constant):
                            if isinstance(argument.value, str):
                                names.add(argument.value)

                # environ.get("NAME")
                elif (
                    function is not None
                    and len(function) == 2
                    and function[0] in environ_names
                    and function[1] in {
                        "get",
                        "setdefault",
                    }
                ):

                    if node.args:
                        argument = node.args[0]

                        if isinstance(argument, ast.Constant):
                            if isinstance(argument.value, str):
                                names.add(argument.value)

            # ----------------------------------------------------------
            # os.environ["NAME"]
            # ----------------------------------------------------------

            if isinstance(node, ast.Subscript):

                value_chain = _attribute_chain(node.value)

                is_os_environ = (
                    value_chain == ["os", "environ"]
                )

                is_imported_environ = (
                    isinstance(node.value, ast.Name)
                    and node.value.id in environ_names
                )

                if is_os_environ or is_imported_environ:

                    slice_node = node.slice

                    if isinstance(
                        slice_node,
                        ast.Constant,
                    ):

                        if isinstance(
                            slice_node.value,
                            str,
                        ):

                            names.add(
                                slice_node.value
                            )

    # ------------------------------------------------------------------
    # Small regex fallback.
    #
    # This catches straightforward cases even when AST parsing fails.
    # ------------------------------------------------------------------

    patterns = (
        r"""os\.getenv\s*\(\s*["']([^"']+)["']""",
        r"""os\.environ\.get\s*\(\s*["']([^"']+)["']""",
        r"""os\.environ\s*\[\s*["']([^"']+)["']\s*\]""",
        r"""getenv\s*\(\s*["']([^"']+)["']""",
        r"""environ\.get\s*\(\s*["']([^"']+)["']""",
        r"""environ\s*\[\s*["']([^"']+)["']\s*\]""",
    )

    for pattern in patterns:

        for match in re.finditer(
            pattern,
            source,
        ):

            names.add(
                match.group(1)
            )

    return names


@contextmanager
def temporary_environment(
    path: Path,
):
    """
    Temporarily provide detected environment variables.

    Existing user environment variables are preserved and restored exactly
    when the context exits.

    Newly-created variables are removed afterwards.
    """

    variable_names = detect_environment_variables(
        path
    )

    if not variable_names:
        yield set()
        return

    previous = {}
    added = set()

    for name in sorted(variable_names):

        if name in os.environ:

            previous[name] = os.environ[name]

        else:

            added.add(name)

        os.environ[name] = ENVIRONMENT_VALUES.get(
            name,
            default_environment_value(name),
        )

    try:

        yield variable_names

    finally:

        # Restore existing variables.
        for name, value in previous.items():
            os.environ[name] = value

        # Remove variables which did not previously exist.
        for name in added:
            os.environ.pop(
                name,
                None,
            )


# ===========================================================================
# DATABASE STUB
# ===========================================================================

def _database_stub() -> ModuleType:
    """
    Allow database-backed samples to be imported without a DB server.

    This preserves the behavior of the older evaluator.
    """

    module = ModuleType("MySQLdb")

    module.Error = RuntimeError

    module.connect = (
        lambda **kwargs:
        (_ for _ in ()).throw(
            RuntimeError("database unavailable")
        )
    )

    return module


if "MySQLdb" not in sys.modules:
    sys.modules["MySQLdb"] = _database_stub()


# ===========================================================================
# SAMPLE DISCOVERY
# ===========================================================================

def sample_paths(
    models: tuple[str, ...],
    variants: tuple[str, ...],
) -> list[tuple[str, str, str, Path]]:

    paths = []

    for model in models:

        for scenario in SCENARIOS:

            for variant in variants:

                folder = (
                    ROOT
                    / model
                    / "Scenarios"
                    / scenario
                    / variant
                )

                for path in sorted(
                    folder.glob("*.py")
                ):

                    paths.append(
                        (
                            model,
                            scenario,
                            variant,
                            path,
                        )
                    )

    return paths


def sample_id(
    sample: tuple[str, str, str, Path],
) -> str:

    model, scenario, variant, path = sample

    return (
        f"{model}/"
        f"{scenario}/"
        f"{variant}/"
        f"{path.stem}"
    )


# ===========================================================================
# OUTPUT HELPERS
# ===========================================================================

def short_output(
    text: str,
    maximum: int = 1500,
) -> str:

    text = text.strip()

    if len(text) <= maximum:
        return text

    return text[-maximum:]


def exception_detail(
    exc: BaseException,
) -> str:

    return short_output(
        f"{type(exc).__name__}: {exc}"
    )


# ===========================================================================
# APPLICATION LOADING
# ===========================================================================

def load_app(
    path: Path,
):
    """
    Import one generated Python file and return its Flask app.

    Before importing, environment variables referenced by the generated
    source are temporarily populated with dummy test values.
    """

    module_name = (
        f"generated_"
        f"{path.parent.parent.parent.name}_"
        f"{path.parent.parent.name}_"
        f"{path.parent.name}_"
        f"{path.stem}"
    )

    # Make module names safe for Python import machinery.
    module_name = (
        module_name
        .replace("-", "_")
        .replace(".", "_")
        .replace(" ", "_")
    )

    spec = importlib.util.spec_from_file_location(
        module_name,
        path,
    )

    if spec is None or spec.loader is None:

        raise AssertionError(
            f"Could not create an import specification "
            f"for {path}"
        )

    module = importlib.util.module_from_spec(
        spec
    )

    sys.modules[module_name] = module

    try:

        with temporary_environment(path) as environment_variables:

            if environment_variables:
                print(
                    f"  Environment variables detected: "
                    f"{', '.join(sorted(environment_variables))}",
                    flush=True,
                )

            spec.loader.exec_module(module)

    except Exception as exc:

        raise AssertionError(
            f"Could not import {path}: "
            f"{type(exc).__name__}: {exc}"
        ) from exc

    app = getattr(
        module,
        "app",
        None,
    )

    if app is None:

        raise AssertionError(
            f"{path} does not expose a Flask "
            f"app named 'app'"
        )

    if not hasattr(
        app,
        "test_client",
    ):

        raise AssertionError(
            f"{path} exposes an 'app', but it does "
            f"not provide Flask test_client()"
        )

    return app


# ===========================================================================
# REQUEST HELPERS
# ===========================================================================

def request_app(
    app,
    method: str,
    route: str,
    values: dict[str, str],
):
    """
    Send the representative request used by the scenario.
    """

    client = app.test_client()

    route = route.replace(
        "<username>",
        "test-user",
    )

    if method == "POST":

        return client.post(
            route,
            data=values,
        )

    return client.get(
        route,
        query_string=values,
    )


# ===========================================================================
# INDIVIDUAL CHECKS
# ===========================================================================

def check_import(
    path: Path,
) -> tuple[bool, str]:

    try:

        load_app(path)

        return True, ""

    except Exception as exc:

        return (
            False,
            exception_detail(exc),
        )


def check_route(
    path: Path,
    scenario: str,
) -> tuple[bool, str]:

    try:

        app = load_app(path)

        config = SCENARIOS[scenario]

        expected_route = config["route"]
        method = config["method"]

        routes = {
            rule.rule
            for rule in app.url_map.iter_rules()
            if method in rule.methods
        }

        if expected_route not in routes:

            return (
                False,
                (
                    f"Expected route {expected_route!r} "
                    f"for {method}, but available routes "
                    f"were: {sorted(routes)}"
                ),
            )

        return True, ""

    except Exception as exc:

        return (
            False,
            exception_detail(exc),
        )


def check_request(
    path: Path,
    scenario: str,
) -> tuple[bool, str]:

    try:

        app = load_app(path)

        config = SCENARIOS[scenario]

        route = config["route"]
        method = config["method"]

        values = config.get(
            "query",
            config.get(
                "body",
                {},
            ),
        )

        response = request_app(
            app,
            method,
            route,
            values,
        )

        if response.status_code in (
            404,
            405,
        ):

            return (
                False,
                (
                    f"Representative request returned "
                    f"HTTP {response.status_code}"
                ),
            )

        return True, ""

    except Exception as exc:

        return (
            False,
            exception_detail(exc),
        )


# ===========================================================================
# PER-SAMPLE EVALUATION
# ===========================================================================

def test_sample(
    sample: tuple[str, str, str, Path],
) -> tuple[int, list[tuple[str, str]]]:

    model, scenario, variant, source = sample

    failures = []

    # ---------------------------------------------------------------
    # Check 1: import
    # ---------------------------------------------------------------

    passed_import, detail = check_import(
        source
    )

    if not passed_import:

        failures.append(
            (
                "Python import failure",
                detail,
            )
        )

        return 0, failures

    passed = 1

    # ---------------------------------------------------------------
    # Check 2: route
    # ---------------------------------------------------------------

    passed_route, detail = check_route(
        source,
        scenario,
    )

    if passed_route:

        passed += 1

    else:

        failures.append(
            (
                "Python route failure",
                detail,
            )
        )

    # ---------------------------------------------------------------
    # Check 3: representative request
    # ---------------------------------------------------------------

    passed_request, detail = check_request(
        source,
        scenario,
    )

    if passed_request:

        passed += 1

    else:

        failures.append(
            (
                "Python runtime failure",
                detail,
            )
        )

    return passed, failures


# ===========================================================================
# MAIN
# ===========================================================================

def main() -> int:

    if RUN_MODE == "single":

        active_models = (
            SINGLE_MODEL,
        )

        active_variants = (
            SINGLE_VARIANT,
        )

    elif RUN_MODE == "all":

        active_models = MODELS
        active_variants = VARIANTS

    else:

        raise ValueError(
            'RUN_MODE must be either "single" or "all"'
        )

    results = {
        model: {
            variant: [0, 0]
            for variant in active_variants
        }
        for model in active_models
    }

    failures = []

    samples = sample_paths(
        active_models,
        active_variants,
    )

    total_samples = len(samples)

    print(
        f"Found {total_samples} Python samples.",
        flush=True,
    )

    completed = 0

    for model, scenario, variant, source in samples:

        print(
            f"\nTesting {model}/{scenario}/{variant}/{source.name}",
            flush=True,
        )

        passed, sample_failures = test_sample(
            (
                model,
                scenario,
                variant,
                source,
            )
        )

        results[model][variant][0] += passed

        results[model][variant][1] += 3

        for category, detail in sample_failures:

            failures.append(
                (
                    category,
                    model,
                    (
                        f"{scenario}/"
                        f"{variant}/"
                        f"{source.name}"
                    ),
                    detail,
                )
            )

        completed += 1

        if (
            completed % 10 == 0
            or completed == total_samples
        ):

            percentage = (
                completed
                / total_samples
                * 100
                if total_samples
                else 100
            )

            print(
                f"Progress: "
                f"{completed}/"
                f"{total_samples} "
                f"({percentage:.1f}%)",
                flush=True,
            )

    print()

    print(
        "=" * 70
    )

    print(
        "FINAL RESULTS"
    )

    print(
        "=" * 70
    )

    for model in active_models:

        print(
            f"\n===== {model} ====="
        )

        for variant in active_variants:

            passed, total = (
                results[model][variant]
            )

            percentage = (
                passed / total * 100
                if total
                else 0
            )

            print(
                f"{variant}: "
                f"{percentage:.2f}% "
                f"- {passed}/{total}"
            )

    failed = sum(
        total - passed
        for model_results in results.values()
        for passed, total in model_results.values()
    )

    print()

    print(
        "=" * 70
    )

    print(
        f"Failed checks: {failed}"
    )

    if failures:

        print(
            "\nFailure summary:"
        )

        counts = Counter(
            category
            for category, _, _, _ in failures
        )

        for category, count in (
            counts.most_common()
        ):

            print(
                f"  {category}: {count}"
            )

        print(
            "\nExamples:"
        )

        for (
            category,
            model,
            location,
            detail,
        ) in failures[:20]:

            print(
                f"\n[{category}] "
                f"{model}/{location}"
            )

            print(
                f"  {detail}"
            )

    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(
        main()
    )
