#!/usr/bin/env python3
from __future__ import annotations
import unittest
from detect_ci_scope import classify_paths, gates_for

class DevelopmentVelocityTest(unittest.TestCase):
    def test_scoped_iteration_avoids_lint_and_apk_builds(self) -> None:
        scope = classify_paths(["app/src/main/kotlin/io/github/daniele21/redactguard/ui/MainScreen.kt"])
        gates = gates_for(scope, "iteration")
        self.assertIn("debug-compile", gates)
        self.assertIn("unit-tests", gates)
        self.assertNotIn("lint", gates)
        self.assertNotIn("debug-apk", gates)
        self.assertNotIn("androidtest-apk", gates)
        self.assertNotIn("release-r8", gates)

    def test_scoped_integration_adds_lint_and_debug_apk(self) -> None:
        scope = classify_paths(["app/src/main/kotlin/io/github/daniele21/redactguard/ui/MainScreen.kt"])
        gates = gates_for(scope, "integration")
        self.assertIn("lint", gates)
        self.assertIn("debug-apk", gates)
        self.assertNotIn("release-r8", gates)

    def test_harness_boundary_integration_adds_stronger_android_gates(self) -> None:
        scope = classify_paths(["app/src/main/kotlin/io/github/daniele21/redactguard/infrastructure/SharedRuntimeClient.kt"])
        gates = gates_for(scope, "integration")
        self.assertIn("direct-contract", gates)
        self.assertIn("androidtest-apk", gates)
        self.assertIn("release-r8", gates)

if __name__ == "__main__":
    unittest.main()
