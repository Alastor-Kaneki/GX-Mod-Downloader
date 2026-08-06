#!/usr/bin/env python3
"""Reconstruct the native wrapper source tree from the compact repository bundle."""
from __future__ import annotations

import base64
import io
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parents[1]
PARTS = ROOT / "source.parts"
encoded = "".join(path.read_text(encoding="ascii").strip() for path in sorted(PARTS.glob("part-*.b64")))
if not encoded:
    raise SystemExit("No source bundle parts were found")
payload = base64.b64decode(encoded, validate=True)

with tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz") as archive:
    root_resolved = ROOT.resolve()
    for member in archive.getmembers():
        destination = (ROOT / member.name).resolve()
        if destination != root_resolved and root_resolved not in destination.parents:
            raise SystemExit(f"Unsafe archive entry: {member.name}")
    archive.extractall(ROOT, filter="data")

print(f"Reconstructed native source tree from {len(payload)} bytes")
