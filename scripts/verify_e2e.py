#!/usr/bin/env python3
"""Zero-dependency validation for the repo-template-sw 0.9.2 E2E contract."""
from __future__ import annotations
import argparse, json, sys
from pathlib import Path

FIDELITY=["host_or_fake","simulated_or_emulated","representative_virtual","representative_physical","target_environment"]
RANK={v:i for i,v in enumerate(FIDELITY)}
UI=["assertions","screenshots","full_media"]
PRINCIPLES=("final_environment_should_confirm_not_discover","execution_capability_separate_from_environment_fidelity","lowest_sufficient_test_level","critical_journeys_only","built_artifact_when_material","residual_fidelity_gaps_explicit","ui_evidence_risk_based")
TRIGGERS={"material_ui_integration_outcome","motion_or_animation","timing_or_progression","navigation_or_transition_sequence","lifecycle_visibility","release_acceptance"}

def text(v): return isinstance(v,str) and bool(v.strip())
def refs(value,known,label,errors,allow_empty=False):
    if not isinstance(value,list) or not all(text(x) for x in value): errors.append(f"{label} must be a list of ids"); return []
    if not value and not allow_empty: errors.append(f"{label} must not be empty")
    for x in value:
        if x not in known: errors.append(f"{label} references unknown id: {x}")
    return value

def main()->int:
    p=argparse.ArgumentParser(); p.add_argument("--root",default="."); p.add_argument("--template-mode",action="store_true"); a=p.parse_args(); root=Path(a.root).resolve(); errors=[]; warnings=[]
    try: data=json.loads((root/".engineering"/"e2e.json").read_text(encoding="utf-8")); commands=json.loads((root/".engineering"/"commands.json").read_text(encoding="utf-8"))
    except (OSError,json.JSONDecodeError) as exc: print(f"FAIL: invalid engineering JSON: {exc}"); return 1
    if data.get("schema_version")!=1: errors.append("schema_version must be 1")
    if data.get("contract_version")!="0.2.1": errors.append("contract_version must be 0.2.1")
    app=data.get("applicability",{}); status=app.get("status")
    if status not in {"required","recommended","n/a"}: errors.append("applicability.status invalid")
    if not text(app.get("reason")): errors.append("applicability.reason is required")
    command=(commands.get("commands") or {}).get("e2e") or {}; cstatus=command.get("status")
    if status=="n/a" and cstatus!="n/a": errors.append("E2E n/a requires commands.e2e n/a")
    if status in {"required","recommended"} and cstatus=="n/a": errors.append("E2E-applicable repo may not set commands.e2e n/a")
    if status=="required" and cstatus!="required": errors.append("required E2E requires commands.e2e required")
    principles=data.get("principles",{})
    for k in PRINCIPLES:
        if principles.get(k) is not True: errors.append(f"principles.{k} must be true")
    policy=data.get("stage_policy",{}); integ=policy.get("integration",{}); rel=policy.get("release",{})
    expected_integ={"automated_e2e_before_shared_integration":True,"real_environment_blocking":False,"real_environment_deferred_to_release":True,"material_ui_journey_minimum_evidence_mode":"full_media","incidental_ui_may_use_assertions":True}
    expected_rel={"full_validation_required":True,"release_critical_e2e_required":True,"required_real_environment_blocking":True}
    for k,v in expected_integ.items():
        if integ.get(k)!=v: errors.append(f"stage_policy.integration.{k} must be {v!r}")
    for k,v in expected_rel.items():
        if rel.get(k)!=v: errors.append(f"stage_policy.release.{k} must be {v!r}")
    ui=data.get("ui_evidence",{})
    if ui.get("modes")!=UI: errors.append("ui_evidence.modes invalid")
    if ui.get("default_mode") not in UI: errors.append("ui_evidence.default_mode invalid")
    if ui.get("assertions_allowed_when_ui_incidental") is not True: errors.append("ui_evidence.assertions_allowed_when_ui_incidental must be true")
    missing=TRIGGERS-set(ui.get("full_media_triggers") or [])
    if missing: errors.append("ui_evidence.full_media_triggers missing: "+", ".join(sorted(missing)))
    if data.get("fidelity_order")!=FIDELITY: errors.append("fidelity_order must match canonical order")
    targets_raw=data.get("target_environments"); envs_raw=data.get("execution_environments"); journeys_raw=data.get("critical_journeys")
    if not isinstance(targets_raw,list): errors.append("target_environments must be a list"); targets_raw=[]
    if not isinstance(envs_raw,list): errors.append("execution_environments must be a list"); envs_raw=[]
    if not isinstance(journeys_raw,list): errors.append("critical_journeys must be a list"); journeys_raw=[]
    def keyed(items,label):
        out={}
        for i,item in enumerate(items):
            if not isinstance(item,dict) or not text(item.get("id")): errors.append(f"{label}[{i}].id required"); continue
            if item["id"] in out: errors.append(f"duplicate {label} id: {item['id']}")
            out[item["id"]]=item
        return out
    targets=keyed(targets_raw,"target_environments"); envs=keyed(envs_raw,"execution_environments"); journeys=keyed(journeys_raw,"critical_journeys")
    if status in {"required","recommended"} and (not targets or not envs or not journeys): errors.append("E2E-applicable repo must declare target/execution environments and critical journeys")
    for ident,t in targets.items():
        if not text(t.get("platform")) or not text(t.get("description")): errors.append(f"target_environments.{ident} platform/description required")
        dims=t.get("material_dimensions");
        if not isinstance(dims,list) or not dims or not all(text(x) for x in dims): errors.append(f"target_environments.{ident}.material_dimensions invalid")
    automated=set()
    for ident,e in envs.items():
        if e.get("fidelity_class") not in RANK: errors.append(f"execution_environments.{ident}.fidelity_class invalid")
        if e.get("automation") not in {"automated","real_environment"}: errors.append(f"execution_environments.{ident}.automation invalid")
        if e.get("automation")=="automated": automated.add(ident)
        if not text(e.get("platform")) or not text(e.get("artifact_surface")): errors.append(f"execution_environments.{ident} platform/artifact_surface required")
        refs(e.get("target_environment_refs"),set(targets),f"execution_environments.{ident}.target_environment_refs",errors)
        gaps=e.get("known_gaps")
        if not isinstance(gaps,list) or not all(text(x) for x in gaps): errors.append(f"execution_environments.{ident}.known_gaps invalid")
    for ident,j in journeys.items():
        if not text(j.get("claim")): errors.append(f"critical_journeys.{ident}.claim required")
        if not isinstance(j.get("ui_surface"),bool): errors.append(f"critical_journeys.{ident}.ui_surface must be boolean")
        mode=j.get("minimum_ui_evidence_mode")
        if j.get("ui_surface") is True and mode not in UI: errors.append(f"critical_journeys.{ident}.minimum_ui_evidence_mode invalid")
        if j.get("ui_surface") is False and mode not in {None,"assertions"}: errors.append(f"critical_journeys.{ident} non-UI mode must be assertions/absent")
        refs(j.get("target_environment_refs"),set(targets),f"critical_journeys.{ident}.target_environment_refs",errors)
        arefs=refs(j.get("automated_environment_refs"),set(envs),f"critical_journeys.{ident}.automated_environment_refs",errors,allow_empty=True)
        minimum=j.get("minimum_automated_fidelity")
        if minimum not in RANK: errors.append(f"critical_journeys.{ident}.minimum_automated_fidelity invalid")
        ranks=[RANK[envs[x]["fidelity_class"]] for x in arefs if x in envs and envs[x].get("automation")=="automated" and envs[x].get("fidelity_class") in RANK]
        if ranks and minimum in RANK and max(ranks)<RANK[minimum]: errors.append(f"critical_journeys.{ident} automated fidelity below minimum")
        if arefs and not any(x in automated for x in arefs): errors.append(f"critical_journeys.{ident} has no automated execution environment")
        if not arefs and not text(j.get("automation_gap_reason")): errors.append(f"critical_journeys.{ident} needs automated refs or automation_gap_reason")
        confirmation=j.get("real_environment_confirmation")
        if confirmation not in {"required","conditional","not_required"}: errors.append(f"critical_journeys.{ident}.real_environment_confirmation invalid")
        residual=j.get("residual_gaps")
        if not isinstance(residual,list) or not all(text(x) for x in residual): errors.append(f"critical_journeys.{ident}.residual_gaps invalid")
        if confirmation=="not_required" and residual: warnings.append(f"critical_journeys.{ident} has residual gaps but real_environment_confirmation not_required")
    if not a.template_mode and "<REPLACE_WITH_" in json.dumps(data): errors.append("unresolved adopter placeholder in .engineering/e2e.json")
    print("E2E environment fidelity contract check"); print(f"root: {root}"); print(f"applicability: {status}"); print(f"commands.e2e.status: {cstatus}")
    for x in warnings: print(f"WARN: {x}")
    for x in errors: print(f"FAIL: {x}")
    if errors: print(f"RESULT: FAIL ({len(errors)} error(s), {len(warnings)} warning(s))"); return 1
    print(f"RESULT: PASS ({len(warnings)} warning(s))"); return 0
if __name__=="__main__": sys.exit(main())
