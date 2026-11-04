#!/usr/bin/env python3
"""Verify that the inherited handover, transport and storage implementation is intact."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = json.loads((root / 'UPSTREAM.json').read_text())
for path, expected in source['unchanged_sha256'].items():
    actual = hashlib.sha256((root / path).read_bytes()).hexdigest()
    if actual != expected:
        raise SystemExit(f'Inherited source changed: {path}. Review the change and its provenance explicitly.')
    print(f'PASS unchanged {path}')


for path, review in source.get('reviewed_changes', {}).items():
    actual = hashlib.sha256((root / path).read_bytes()).hexdigest()
    if actual != review['sha256'] or not review.get('reason') or not review.get('upstream_sha256'):
        raise SystemExit(f'Reviewed source changed: {path}. Review the change and its provenance explicitly.')
    print(f"PASS reviewed {path}: {review['reason']}")
