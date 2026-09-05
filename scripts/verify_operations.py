#!/usr/bin/env python3
"""Zero-dependency validation for the repo-template-sw 0.9.2 operating contract."""
from __future__ import annotations
import argparse, json, sys
from pathlib import Path

COMMANDS=("setup","doctor","dev","check","test","e2e","build","smoke","package","stop","clean")
STATUSES={"required","recommended","optional","n/a"}
REQUIRED_NON_NA={"setup","check","test","build","clean"}
PUB_FLAGS=("agent_preflight_required","target_base_freshness_required","full_diff_review_required","material_ambiguity_must_be_resolved","failure_root_cause_required","execution_capability_classification_required","blast_radius_profile_selection_required","automatable_gates_must_not_be_delegated_to_user","remote_automated_fallback_required_when_agent_local_unavailable","deterministic_ci_command_parity_required","non_automated_evidence_must_be_declared","exact_head_evidence_required")
E2E_FLAGS=("recommended_when_full_workflow_boundary_exists","critical_journeys_prioritized","lower_level_tests_remain_primary","use_stack_native_tooling","run_against_built_artifact_when_material","failure_evidence_bounded","zero_residue_required","incidental_ui_does_not_force_full_media","full_media_for_motion_timing_sequence_or_release_claims")
CLEANUP={"success","failure","timeout","cancellation","interrupt","partial-initialization"}
DELTA={"source","dependencies","toolchain","configuration","compatibility_migrations","artifact_metrics","validation"}

def truth(section,key,errors,prefix):
    if section.get(key) is not True: errors.append(f"{prefix}.{key} must be true")
def falsity(section,key,errors,prefix):
    if section.get(key) is not False: errors.append(f"{prefix}.{key} must be false")
def obj(data,key,errors):
    value=data.get(key)
    if not isinstance(value,dict): errors.append(f"{key} must be an object"); return {}
    return value

def main()->int:
    p=argparse.ArgumentParser(); p.add_argument("--root",default="."); p.add_argument("--template-mode",action="store_true"); a=p.parse_args(); root=Path(a.root).resolve(); errors=[]; warnings=[]
    try: data=json.loads((root/".engineering"/"commands.json").read_text(encoding="utf-8"))
    except (OSError,json.JSONDecodeError) as exc: print(f"FAIL: invalid .engineering/commands.json: {exc}"); return 1
    if data.get("schema_version")!=1: errors.append("schema_version must be 1")
    if data.get("contract_version")!="0.6.1": errors.append("contract_version must be 0.6.1")
    commands=obj(data,"commands",errors)
    for name in COMMANDS:
        entry=commands.get(name)
        if not isinstance(entry,dict): errors.append(f"missing command intent: {name}"); continue
        status=entry.get("status"); run=entry.get("run")
        if status not in STATUSES: errors.append(f"commands.{name}.status invalid")
        if name in REQUIRED_NON_NA and status=="n/a": errors.append(f"commands.{name} may not be n/a")
        if status!="n/a" and (not isinstance(run,str) or not run.strip()): errors.append(f"commands.{name}.run is required when status is not n/a")
        if not a.template_mode and isinstance(run,str) and ("<REPLACE_WITH_" in run or "<PROJECT_" in run): errors.append(f"unresolved command placeholder in commands.{name}.run")
    v=obj(data,"development_velocity",errors)
    if v.get("default_stage")!="iteration" or v.get("stages")!=["iteration","integration","release"]: errors.append("development_velocity stage model must be iteration -> integration -> release")
    it=v.get("iteration",{}); integ=v.get("integration",{}); rel=v.get("release",{})
    for k in ("exact_head_required","full_diff_review_required","durable_documentation_current_required","remote_preflight_required"): falsity(it,k,errors,"development_velocity.iteration")
    if it.get("e2e_default")!="risk_only": errors.append("iteration.e2e_default must be risk_only")
    for k in ("exact_head_required","full_diff_review_required","durable_documentation_current_required","remote_preflight_when_required_gates_unavailable_local","automated_e2e_required_when_affected","real_environment_deferred_to_release"): truth(integ,k,errors,"development_velocity.integration")
    falsity(integ,"real_environment_blocking",errors,"development_velocity.integration")
    if integ.get("e2e_default")!="affected_critical_journeys": errors.append("integration.e2e_default must be affected_critical_journeys")
    for k in ("exact_head_required","full_diff_review_required","durable_documentation_current_required","full_validation_required","required_real_environment_blocking"): truth(rel,k,errors,"development_velocity.release")
    if rel.get("e2e_default")!="release_critical_journeys": errors.append("release.e2e_default must be release_critical_journeys")
    truth(v,"parallel_development_prefers_early_convergence",errors,"development_velocity"); truth(v,"stacked_publication_exception_only",errors,"development_velocity")
    pub=obj(data,"publication_gate",errors)
    if pub.get("applies_from_stage")!="integration": errors.append("publication_gate.applies_from_stage must be integration")
    for k in PUB_FLAGS: truth(pub,k,errors,"publication_gate")
    ex=obj(data,"validation_execution",errors)
    if not {"agent_local","remote_automated","real_environment"}.issubset(set(ex.get("classes") or [])): errors.append("validation_execution.classes incomplete")
    truth(ex,"no_human_runner_for_automatable_gates",errors,"validation_execution"); truth(ex,"remote_automation_required_when_agent_local_unavailable",errors,"validation_execution")
    prof=obj(data,"validation_profiles",errors)
    if prof.get("default")!="auto" or not {"lean","scoped","strong","full"}.issubset(set(prof.get("profiles") or [])): errors.append("validation_profiles must declare auto + lean/scoped/strong/full")
    if prof.get("selector_output")!="risk_dimensions_and_required_gates": errors.append("validation_profiles.selector_output invalid")
    if not isinstance(prof.get("selector"),str) or not prof.get("selector").strip(): errors.append("validation_profiles.selector is required")
    for k in ("profiles_are_shorthand","gate_selection_preferred_over_suite_selection","unknown_executable_paths_fail_safe","selector_changes_force_full","promotion_validation_full","automatic_escalation_allowed","silent_downgrade_below_auto_forbidden","report_selected_profile_and_reason"): truth(prof,k,errors,"validation_profiles")
    remote=obj(data,"remote_preflight",errors)
    if remote.get("status") not in {"required","recommended","n/a"}: errors.append("remote_preflight.status invalid")
    if remote.get("status")!="n/a":
        if not isinstance(remote.get("trigger"),str) or not remote.get("trigger").strip(): errors.append("remote_preflight.trigger required")
        for k in ("stronger_profile_override_allowed","weaker_profile_override_requires_explicit_justification","exact_head_required","reuse_successful_equivalent_evidence","rerun_only_when_missing_stale_or_insufficient","trusted_requesters_only","same_repository_prs_only_by_default","report_result_to_pr"): truth(remote,k,errors,"remote_preflight")
        if not {"head","target_base","required_gates","profile","e2e_environment"}.issubset(set(remote.get("evidence_identity_fields") or [])): errors.append("remote_preflight.evidence_identity_fields incomplete")
        if remote.get("execution_job_write_credentials") is not False: errors.append("remote_preflight.execution_job_write_credentials must be false")
    e2e=obj(data,"end_to_end",errors)
    for k in E2E_FLAGS: truth(e2e,k,errors,"end_to_end")
    if e2e.get("ui_evidence_modes")!=["assertions","screenshots","full_media"] or e2e.get("ui_evidence_selection")!="risk_based": errors.append("end_to_end UI evidence policy invalid")
    eco=obj(data,"validation_economics",errors)
    if eco.get("optimize_for")!="sufficient-confidence-per-feedback-time" or not {"duration","flake_rate","unique_regression_signal","overlap"}.issubset(set(eco.get("dimensions") or [])): errors.append("validation_economics invalid")
    truth(eco,"periodic_review",errors,"validation_economics")
    identity=obj(data,"build_identity",errors)
    for k in ("unique_per_build","source_revision_required","dirty_state_required"): truth(identity,k,errors,"build_identity")
    if not {"product","product_version","build_id","source_revision"}.issubset(set(identity.get("artifact_name_fields") or [])): errors.append("build_identity.artifact_name_fields incomplete")
    art=obj(data,"artifact_lifecycle",errors)
    for k in ("immutable_successful_artifacts","promote_only_after_success","manifest_required","release_artifacts_immutable"): truth(art,k,errors,"artifact_lifecycle")
    if str(art.get("checksum_algorithm","")).lower()!="sha256": errors.append("artifact_lifecycle.checksum_algorithm must be sha256")
    delta=obj(data,"build_delta",errors); truth(delta,"required",errors,"build_delta"); truth(delta,"bundle_with_artifact",errors,"build_delta")
    if not DELTA.issubset(set(delta.get("dimensions") or [])): errors.append("build_delta.dimensions incomplete")
    eph=obj(data,"ephemeral_resources",errors)
    for k in ("run_identity","isolated_workspace","stale_resource_recovery","ownership_required_before_cleanup","post_cleanup_verification"): truth(eph,k,errors,"ephemeral_resources")
    if not CLEANUP.issubset(set(eph.get("cleanup_paths") or [])): errors.append("ephemeral_resources.cleanup_paths incomplete")
    print("Project operating contract check"); print(f"root: {root}")
    for x in warnings: print(f"WARN: {x}")
    for x in errors: print(f"FAIL: {x}")
    if errors: print(f"RESULT: FAIL ({len(errors)} error(s), {len(warnings)} warning(s))"); return 1
    print(f"RESULT: PASS ({len(warnings)} warning(s))"); return 0
if __name__=="__main__": sys.exit(main())
