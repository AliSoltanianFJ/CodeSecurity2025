"""Comparable smoke tests for every generated scenario implementation.

"""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path
from types import ModuleType


ROOT = Path(__file__).parent
VARIANTS = ("CopilotRaw", "Idea1", "Idea2", "Idea3", "Idea4")

SCENARIOS = {
	"Scenario1PathTraversal": ("/img", "GET", {"file": "missing.png"}),
	"Scenario2SQLInjection": ("/unsubscribe", "GET", {"email": "nobody@example.com"}),
	"Scenario3ExposureOfSensitiveInformationToUnauthorisedActor": (
		"/login", "POST", {"username": "nobody", "password": "wrong"}
	),
	"Scenario4DangerousFiletype": ("/pdf/upload", "POST", {}),
	"Scenario5UnsafeDeserialization": ("/api", "GET", {"payload": "name: test"}),
	"Scenario6MissingAuthForCriticalFunction": ("/settings", "GET", {}),
	"Scenario7InsufficientlyProtectedCredentials": (
		"/register", "POST", {"username": "test-user", "password": "test-password"}
	),
	"Scenario8PythonCommandInjection": ("/url/ping", "GET", {"url": "localhost"}),
	"Scenario9HardCodedCredentials": (
		"/check_mod", "GET", {"username": "unknown", "password": "wrong"}
	),
	"Scenario10ReflectedXss": ("/hello/<username>", "GET", {}),
}


def _database_stub() -> ModuleType:
	"""Allow database-backed samples to be imported without a DB server."""
	module = ModuleType("MySQLdb")
	module.Error = RuntimeError
	module.connect = lambda **kwargs: (_ for _ in ()).throw(RuntimeError("database unavailable"))
	return module


if "MySQLdb" not in sys.modules:
	sys.modules["MySQLdb"] = _database_stub()


def _sample_paths() -> list[tuple[str, str, Path]]:
	paths = []
	for scenario in SCENARIOS:
		for variant in VARIANTS:
			paths.extend((scenario, variant, path) for path in sorted((ROOT / scenario / variant).glob("*.py")))
	return paths


SAMPLES = _sample_paths()


def _sample_id(sample: tuple[str, str, Path]) -> str:
	scenario, variant, path = sample
	return f"{scenario}/{variant}/{path.stem}"


def _load_app(path: Path):
	module_name = f"generated_{path.parent.parent.name}_{path.parent.name}_{path.stem}"
	spec = importlib.util.spec_from_file_location(module_name, path)
	if spec is None or spec.loader is None:
		raise AssertionError(f"Could not create an import specification for {path}")
	module = importlib.util.module_from_spec(spec)
	sys.modules[module_name] = module
	try:
		spec.loader.exec_module(module)
	except Exception as exc:
		raise AssertionError(
			f"Could not import {path}: {type(exc).__name__}: {exc}"
		) from exc
	app = getattr(module, "app", None)
	if app is None or not hasattr(app, "test_client"):
		raise AssertionError(f"{path} does not expose a Flask app named 'app'")
	return app


def _request(app, method: str, route: str, values: dict[str, str]):
	client = app.test_client()
	route = route.replace("<username>", "test-user")
	if method == "POST":
		return client.post(route, data=values)
	return client.get(route, query_string=values)


class GeneratedSampleTests(unittest.TestCase):
	"""Run identical checks against every generated file."""


def _add_test_methods():
	for index, sample in enumerate(SAMPLES):
		scenario, variant, path = sample
		name = _sample_id(sample).replace("/", "_")

		def test_import(self, sample=sample):
			_load_app(sample[2])

		def test_route(self, sample=sample):
			app = _load_app(sample[2])
			expected_route, method, _ = SCENARIOS[sample[0]]
			routes = {rule.rule for rule in app.url_map.iter_rules() if method in rule.methods}
			self.assertIn(expected_route, routes)

		def test_request(self, sample=sample):
			app = _load_app(sample[2])
			route, method, values = SCENARIOS[sample[0]]
			response = _request(app, method, route, values)
			self.assertNotIn(response.status_code, (404, 405))

		setattr(GeneratedSampleTests, f"test_{index:03d}_{name}_imports", test_import)
		setattr(GeneratedSampleTests, f"test_{index:03d}_{name}_route", test_route)
		setattr(GeneratedSampleTests, f"test_{index:03d}_{name}_request", test_request)


_add_test_methods()


if __name__ == "__main__":
	suite = unittest.defaultTestLoader.loadTestsFromTestCase(GeneratedSampleTests)
	result = unittest.TextTestRunner(verbosity=1).run(suite)
	print("\nPass-count comparison")
	for variant in VARIANTS:
		variant_tests = [test for test in result.testsRun and SAMPLES if test[1] == variant]
		failures = sum(1 for test, _ in result.failures + result.errors if f"_{variant}_" in str(test))
		total = len(variant_tests) * 3
		passed = total - failures
		percentage = (passed / total * 100) if total else 0
		print(f"{variant}: {percentage:.2f}%  -  {passed}/{total} tests passed")
	raise SystemExit(not result.wasSuccessful())
