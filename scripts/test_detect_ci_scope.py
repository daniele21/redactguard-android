#!/usr/bin/env python3

from __future__ import annotations

import unittest

from detect_ci_scope import apply_requested_profile, classify_paths


class DetectCiScopeTest(unittest.TestCase):
    def test_docs_are_lean(self) -> None:
        scope = classify_paths(["README.md", "docs/architecture.md"])
        self.assertEqual(scope.profile, "lean")
        self.assertFalse(scope.android)
        self.assertFalse(scope.android_test)
        self.assertFalse(scope.release)

    def test_governance_is_lean(self) -> None:
        scope = classify_paths([".engineering/baseline.json", "AGENTS.md", "scripts/verify_operations.py"])
        self.assertEqual(scope.profile, "lean")
        self.assertFalse(scope.android)

    def test_normal_ui_kotlin_change_is_scoped(self) -> None:
        scope = classify_paths(["app/src/main/kotlin/io/github/daniele21/redactguard/ui/MainScreen.kt"])
        self.assertEqual(scope.profile, "scoped")
        self.assertTrue(scope.android)
        self.assertFalse(scope.android_test)
        self.assertFalse(scope.release)

    def test_resource_change_is_scoped(self) -> None:
        scope = classify_paths(["app/src/main/res/values/strings.xml"])
        self.assertEqual(scope.profile, "scoped")

    def test_domain_change_is_strong(self) -> None:
        scope = classify_paths(["app/src/main/kotlin/io/github/daniele21/redactguard/domain/ReviewDecision.kt"])
        self.assertEqual(scope.profile, "strong")
        self.assertTrue(scope.android_test)
        self.assertTrue(scope.release)

    def test_infrastructure_change_is_strong(self) -> None:
        scope = classify_paths(["app/src/main/kotlin/io/github/daniele21/redactguard/infrastructure/SharedRuntimeClient.kt"])
        self.assertEqual(scope.profile, "strong")
        self.assertTrue(scope.android_test)
        self.assertTrue(scope.release)

    def test_harness_named_integration_change_is_strong(self) -> None:
        scope = classify_paths(["app/src/main/kotlin/io/github/daniele21/redactguard/SharedRuntimeClient.kt"])
        self.assertEqual(scope.profile, "strong")

    def test_privacy_or_pii_named_boundary_is_strong(self) -> None:
        scope = classify_paths(["app/src/main/kotlin/io/github/daniele21/redactguard/PrivacyPiiPolicy.kt"])
        self.assertEqual(scope.profile, "strong")

    def test_manifest_is_strong(self) -> None:
        scope = classify_paths(["app/src/main/AndroidManifest.xml"])
        self.assertEqual(scope.profile, "strong")
        self.assertTrue(scope.release)

    def test_proguard_is_strong(self) -> None:
        scope = classify_paths(["app/proguard-rules.pro"])
        self.assertEqual(scope.profile, "strong")

    def test_app_build_file_is_strong(self) -> None:
        scope = classify_paths(["app/build.gradle.kts"])
        self.assertEqual(scope.profile, "strong")

    def test_global_dependency_inventory_is_full(self) -> None:
        scope = classify_paths(["gradle/libs.versions.toml"])
        self.assertEqual(scope.profile, "full")

    def test_selector_change_is_full(self) -> None:
        scope = classify_paths(["scripts/detect_ci_scope.py"])
        self.assertEqual(scope.profile, "full")

    def test_validate_workflow_change_is_full(self) -> None:
        scope = classify_paths([".github/workflows/validate.yml"])
        self.assertEqual(scope.profile, "full")

    def test_remote_dispatcher_change_is_full(self) -> None:
        scope = classify_paths([".github/workflows/remote-preflight.yml"])
        self.assertEqual(scope.profile, "full")

    def test_unknown_executable_path_fails_safe_full(self) -> None:
        scope = classify_paths(["new-module/src/main/kotlin/Thing.kt"])
        self.assertEqual(scope.profile, "full")

    def test_explicit_strong_escalates_scoped(self) -> None:
        auto = classify_paths(["app/src/main/res/values/strings.xml"])
        scope = apply_requested_profile(auto, "strong")
        self.assertEqual(scope.profile, "strong")
        self.assertTrue(scope.release)
        self.assertTrue(scope.android_test)

    def test_explicit_strong_does_not_downgrade_full(self) -> None:
        auto = classify_paths(["settings.gradle.kts"])
        scope = apply_requested_profile(auto, "strong")
        self.assertEqual(scope.profile, "full")

    def test_explicit_full_forces_full(self) -> None:
        auto = classify_paths(["app/src/main/res/values/strings.xml"])
        scope = apply_requested_profile(auto, "full")
        self.assertEqual(scope.profile, "full")
        self.assertTrue(scope.release)
        self.assertTrue(scope.android_test)

    def test_empty_diff_fails_safe_full(self) -> None:
        scope = classify_paths([])
        self.assertEqual(scope.profile, "full")


if __name__ == "__main__":
    unittest.main()
