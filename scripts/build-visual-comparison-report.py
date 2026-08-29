#!/usr/bin/env python3
"""Build a bounded target-vs-actual visual evidence report using only the stdlib."""

from __future__ import annotations

import hashlib
import html
import json
import sys
from pathlib import Path

MAPPINGS = (
    ("Document", "target/document.png", "compact/01-import-compact.png", "MATCH: branded top bar, document-first hero, local-AI status, PDF-dominant entry actions."),
    ("Protection", "target/protection.png", "compact/02-protection-compact.png", "MATCH/ADAPT: recommended profiles first, semantic PII families, analysis CTA; advanced options stay disclosed."),
    ("Analysis", "target/analysis.png", "compact/03-analysis-compact.png", "MATCH/ADAPT: shield/status hierarchy and truthful phases; no fabricated percentage."),
    ("Review compact", "target/review-compact.png", "compact/04-review-compact.png", "MATCH/ADAPT: masked context, hidden value, dominant redact decision and bounded navigation."),
    ("Outcome", "target/outcome.png", "compact/05-outcome-compact.png", "MATCH/ADAPT: protected-document completion, truthful counters and owned actions only."),
    ("Recovery", None, "compact/06-recovery-compact.png", "ADAPT: no dedicated approved target crop; reuse the accepted visual system while preserving cause-specific recovery and collapsed diagnostics."),
    ("Review expanded", "target/review-expanded.png", "expanded/07-review-expanded.png", "MATCH/ADAPT: truthful summary, context and decision zones with hidden sensitive value."),
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_file(root: Path, relative: str) -> Path:
    path = root / relative
    if not path.is_file():
        raise SystemExit(f"Missing visual evidence file: {path}")
    return path


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: build-visual-comparison-report.py <evidence-root> <target-provenance.json>")

    root = Path(sys.argv[1])
    provenance_path = Path(sys.argv[2])
    provenance = json.loads(provenance_path.read_text(encoding="utf-8"))
    expected_target_sha = provenance["source"]["sha256"]
    approved_target = Path(provenance["source"]["repository_path"])
    if not approved_target.is_file():
        raise SystemExit(f"Missing approved target source: {approved_target}")
    actual_target_sha = sha256(approved_target)
    if actual_target_sha != expected_target_sha:
        raise SystemExit(
            f"Approved target SHA mismatch: expected {expected_target_sha}, got {actual_target_sha}"
        )

    records: list[dict[str, object]] = []
    for label, target_relative, actual_relative, acceptance in MAPPINGS:
        actual = require_file(root, actual_relative)
        target = require_file(root, target_relative) if target_relative is not None else None
        records.append(
            {
                "surface": label,
                "target": target_relative,
                "target_sha256": sha256(target) if target is not None else None,
                "actual": actual_relative,
                "actual_sha256": sha256(actual),
                "acceptance": acceptance,
            }
        )

    comparison_dir = root / "comparison"
    comparison_dir.mkdir(parents=True, exist_ok=True)
    manifest = {
        "schema_version": 2,
        "evidence_kind": "android_emulator_visual_reference_v2",
        "approved_target_sha256": expected_target_sha,
        "surface_count": len(records),
        "comparison_mode": "human-reviewed side-by-side; no brittle pixel-equality acceptance",
        "records": records,
    }
    (comparison_dir / "manifest.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )

    cards = []
    for record in records:
        target_markup = (
            f'<img src="../{html.escape(str(record["target"]))}" alt="Approved target for {html.escape(str(record["surface"]))}">'
            if record["target"] is not None
            else '<div class="no-target">No dedicated target crop.<br>Contract/style adaptation only.</div>'
        )
        cards.append(
            f"""
            <section class="comparison">
              <h2>{html.escape(str(record['surface']))}</h2>
              <p>{html.escape(str(record['acceptance']))}</p>
              <div class="pair">
                <figure><figcaption>Approved target</figcaption>{target_markup}</figure>
                <figure><figcaption>Actual emulator capture</figcaption><img src="../{html.escape(str(record['actual']))}" alt="Actual {html.escape(str(record['surface']))} capture"></figure>
              </div>
            </section>
            """
        )

    document = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>RedactGuard Visual Evidence v2</title>
<style>
body {{ font-family: system-ui, sans-serif; margin: 24px; background: #f5f7fb; color: #172033; }}
header, .comparison {{ max-width: 1280px; margin: 0 auto 24px; }}
.comparison {{ background: white; border: 1px solid #dfe5ef; border-radius: 16px; padding: 18px; }}
.pair {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 18px; align-items: start; }}
figure {{ margin: 0; }}
figcaption {{ font-weight: 700; margin-bottom: 8px; }}
img {{ width: 100%; height: auto; border: 1px solid #e8edf5; border-radius: 12px; background: white; }}
.no-target {{ min-height: 220px; display: grid; place-items: center; text-align: center; border: 1px dashed #a9b4c5; border-radius: 12px; padding: 24px; color: #526078; }}
code {{ overflow-wrap: anywhere; }}
</style>
</head>
<body>
<header>
<h1>RedactGuard Visual Evidence v2</h1>
<p>Approved target SHA-256: <code>{html.escape(expected_target_sha)}</code></p>
<p>Production Compose screenshots are synthetic emulator evidence. Acceptance is explicit target-vs-actual review of hierarchy and composition; pixel equality is not used as a product-quality shortcut. Physical-device accessibility and usability remain separate evidence.</p>
</header>
{''.join(cards)}
</body>
</html>
"""
    (comparison_dir / "index.html").write_text(document, encoding="utf-8")

    print(f"Visual comparison report: {comparison_dir / 'index.html'}")
    print(f"Visual comparison manifest: {comparison_dir / 'manifest.json'}")


if __name__ == "__main__":
    main()
