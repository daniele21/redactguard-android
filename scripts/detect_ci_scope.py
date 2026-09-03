#!/usr/bin/env python3
"""Select RedactGuard risk dimensions, required gates and validation-profile shorthand."""
from __future__ import annotations
import argparse, os, subprocess, sys
from dataclasses import dataclass, replace
from pathlib import PurePosixPath
from typing import Iterable, Sequence

ZERO_SHA="0"*40
PROFILE_RANK={"lean":0,"scoped":1,"strong":2,"full":3}
STAGES=("iteration","integration","release")
DOC_ONLY_PATHS={
    ".engineering/baseline.json", ".engineering/documentation-policy.json", ".github/CODEOWNERS",
    ".github/dependabot.yml", ".gitattributes", ".gitignore", "AGENTS.md", "CODE_OF_CONDUCT.md",
    "CONTRIBUTING.md", "EXECUTION-CAPABILITY-CONTRACT.md", "LICENSE", "NOTICE", "README.md", "SECURITY.md",
    "design/brand-kit.json", "design/ux-contract.json", "scripts/verify_operations.py", "scripts/verify_repository.py",
    "scripts/verify_agent_context.py", "scripts/verify_docs.py", "scripts/verify_product_experience.py", "scripts/verify_repository_policy.py",
}
DOC_ONLY_PREFIXES=("docs/","skills/")
DOC_ONLY_SUFFIXES=(".md",".mdx",".rst")
FORCE_FULL_PATHS={
    ".engineering/commands.json", ".github/workflows/validate.yml", ".github/workflows/remote-preflight.yml",
    "scripts/detect_ci_scope.py", "scripts/test_detect_ci_scope.py", "build.gradle.kts", "settings.gradle.kts",
    "gradle.properties", "gradle/libs.versions.toml", "gradle/wrapper/gradle-wrapper.properties",
}
STRONG_PATHS={
    "app/build.gradle.kts", "app/proguard-rules.pro", "app/version.properties", "app/src/main/AndroidManifest.xml",
    "scripts/build-redactguard-release.sh", "scripts/package-redactguard-artifact.sh", "scripts/e2e-redactguard-device.sh",
    "scripts/smoke-redactguard-device.sh", "scripts/physical-two-apk-preflight.sh",
}
STRONG_PREFIXES=("app/src/main/kotlin/io/github/daniele21/redactguard/domain/","app/src/main/kotlin/io/github/daniele21/redactguard/infrastructure/")
STRONG_NAME_TOKENS=("sharedruntime","shared_runtime","harness","binder","persistence","repository","redaction","pii","privacy","security")
KNOWN_EXECUTABLE_PREFIXES=("app/src/main/kotlin/","app/src/main/res/","app/src/test/","app/src/androidTest/","scripts/",".engineering/","design/")

@dataclass(frozen=True)
class ValidationScope:
    profile:str
    android:bool
    android_test:bool
    release:bool
    reason:str
    risk_dimensions:tuple[str,...]=()
    required_gates:tuple[str,...]=()

def normalize_path(path:str)->str: return str(PurePosixPath(path.strip().replace("\\","/")))
def is_docs_only(path:str)->bool: return path in DOC_ONLY_PATHS or path.startswith(DOC_ONLY_PREFIXES) or path.endswith(DOC_ONLY_SUFFIXES)
def is_known_executable(path:str)->bool: return path.startswith(KNOWN_EXECUTABLE_PREFIXES) or path in STRONG_PATHS or path in FORCE_FULL_PATHS
def is_strong(path:str)->bool:
    if path in STRONG_PATHS or path.startswith(STRONG_PREFIXES): return True
    lowered=path.lower().replace("-","_")
    return path.startswith("app/src/main/kotlin/") and any(token in lowered for token in STRONG_NAME_TOKENS)

def classify_paths(paths:Iterable[str], *, force_all:bool=False)->ValidationScope:
    normalized=tuple(sorted({normalize_path(path) for path in paths if path.strip()}))
    if force_all: return ValidationScope("full",True,True,True,"explicit full validation",("release_or_global",))
    if not normalized: return ValidationScope("full",True,True,True,"no reliable diff available",("unknown_scope",))
    if any(path in FORCE_FULL_PATHS for path in normalized): return ValidationScope("full",True,True,True,"validation selector, global build or dependency inventory changed",("validation_routing",))
    implementation=tuple(path for path in normalized if not is_docs_only(path))
    if not implementation: return ValidationScope("lean",False,False,False,"documentation, governance or repository metadata only",("governance",))
    unknown=[path for path in implementation if not is_known_executable(path)]
    if unknown: return ValidationScope("full",True,True,True,"unknown executable scope: "+", ".join(unknown[:3]),("unknown_scope",))
    risks=[]
    if any(path.startswith("app/src/main/res/") for path in implementation): risks.append("ui_resources")
    if any(path.startswith("app/src/androidTest/") for path in implementation): risks.append("android_test")
    strong_paths=[path for path in implementation if is_strong(path)]
    if strong_paths:
        lowered=" ".join(strong_paths).lower()
        if any(token in lowered for token in ("privacy","pii","redaction","security")): risks.append("privacy_or_security")
        if any(token in lowered for token in ("persistence","repository")): risks.append("persistence")
        if any(token in lowered for token in ("harness","binder","sharedruntime","shared_runtime")): risks.append("harness_boundary")
        if any(path in STRONG_PATHS for path in strong_paths): risks.append("release_sensitive")
        return ValidationScope("strong",True,True,True,"domain/infrastructure, privacy/persistence or release-sensitive Android change",tuple(dict.fromkeys(risks or ["cross_boundary"])))
    if not risks: risks.append("contained_app")
    android_test=any(path.startswith("app/src/androidTest/") for path in implementation)
    reason="contained RedactGuard UI/application or AndroidTest change" if android_test else "contained RedactGuard UI/application implementation or test change"
    return ValidationScope("scoped",True,android_test,False,reason,tuple(risks))

def gates_for(scope:ValidationScope, stage:str)->tuple[str,...]:
    gates=["repository-guards"]
    if scope.profile=="lean": return tuple(gates)
    gates += ["spotless", "debug-compile", "unit-tests"]
    if scope.profile in {"strong","full"}: gates.append("direct-contract")
    if stage in {"integration","release"}: gates += ["lint", "debug-apk"]
    if stage in {"integration","release"} and scope.android_test: gates.append("androidtest-apk")
    if stage in {"integration","release"} and scope.release: gates.append("release-r8")
    if scope.profile=="full": gates += ["full-repository-validation"]
    if stage=="release": gates += ["release-critical"]
    return tuple(dict.fromkeys(gates))

def apply_requested_profile(scope:ValidationScope, requested:str)->ValidationScope:
    requested=requested.lower()
    if requested=="auto": return scope
    if requested not in {"strong","full"}: raise ValueError("requested profile must be auto, strong or full")
    if PROFILE_RANK[requested] <= PROFILE_RANK[scope.profile]: return scope
    if requested=="full": return replace(scope, profile="full", android=True, android_test=True, release=True, reason=f"explicit full override; auto={scope.profile}: {scope.reason}", risk_dimensions=tuple(dict.fromkeys(scope.risk_dimensions+("explicit_full",))))
    return replace(scope, profile="strong", android=True, android_test=True, release=True, reason=f"explicit strong override; auto={scope.profile}: {scope.reason}", risk_dimensions=tuple(dict.fromkeys(scope.risk_dimensions+("explicit_strong",))))
def ensure_commit_available(sha:str)->None:
    if not sha or sha==ZERO_SHA: raise ValueError("missing or unusable comparison SHA")
    present=subprocess.run(["git","cat-file","-e",f"{sha}^{{commit}}"],check=False,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    if present.returncode!=0: subprocess.run(["git","fetch","--no-tags","--depth=1","origin",sha],check=True)
def git_changed_files(base_sha:str, head_sha:str)->Sequence[str]:
    ensure_commit_available(base_sha); ensure_commit_available(head_sha)
    result=subprocess.run(["git","diff","--name-only","--diff-filter=ACMRD",base_sha,head_sha],check=True,capture_output=True,text=True)
    return tuple(line for line in result.stdout.splitlines() if line.strip())
def write_outputs(path:str, scope:ValidationScope, stage:str)->None:
    with open(path,"a",encoding="utf-8") as output:
        for key,value in (
            ("stage",stage),("profile",scope.profile),("android",str(scope.android).lower()),("android_test",str(scope.android_test).lower()),
            ("release",str(scope.release).lower()),("risk_dimensions",",".join(scope.risk_dimensions)),("required_gates",",".join(scope.required_gates)),("reason",scope.reason),
        ): output.write(f"{key}={value}\n")
def append_step_summary(paths:Sequence[str], scope:ValidationScope, stage:str)->None:
    summary_path=os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path: return
    with open(summary_path,"a",encoding="utf-8") as summary:
        summary.write("## Validation scope\n\n")
        summary.write(f"- Stage: **{stage.upper()}**\n- Profile: **{scope.profile.upper()}**\n")
        summary.write(f"- Risks: `{', '.join(scope.risk_dimensions) or 'none'}`\n- Required gates: `{', '.join(scope.required_gates) or 'none'}`\n")
        summary.write(f"- Reason: {scope.reason}\n- Changed paths considered: {len(paths)}\n")
def parse_args()->argparse.Namespace:
    parser=argparse.ArgumentParser(); parser.add_argument("--event",required=True); parser.add_argument("--base-sha",default=""); parser.add_argument("--head-sha",default=""); parser.add_argument("--github-output",required=True); parser.add_argument("--profile",default="auto",choices=("auto","strong","full")); parser.add_argument("--stage",default="integration",choices=STAGES); parser.add_argument("--force-full",action="store_true"); return parser.parse_args()
def main()->int:
    args=parse_args(); paths:Sequence[str]=()
    try:
        if args.force_full: scope=classify_paths((),force_all=True)
        else: paths=git_changed_files(args.base_sha,args.head_sha); scope=apply_requested_profile(classify_paths(paths),args.profile)
        scope=replace(scope,required_gates=gates_for(scope,args.stage))
    except (ValueError,subprocess.CalledProcessError) as exc:
        print(f"warning: unable to determine safe validation scope: {exc}",file=sys.stderr); scope=classify_paths((),force_all=True); scope=replace(scope,required_gates=gates_for(scope,args.stage))
    write_outputs(args.github_output,scope,args.stage); append_step_summary(paths,scope,args.stage)
    print(f"validation scope: stage={args.stage} profile={scope.profile} risks={','.join(scope.risk_dimensions)} gates={','.join(scope.required_gates)} reason={scope.reason}")
    if paths:
        print("changed paths:")
        for path in paths: print(f"  - {path}")
    return 0
if __name__=="__main__": raise SystemExit(main())
