#!/usr/bin/env python3
"""Apply the corresponding call-pickup.patch to pinned packaged call JS only.

Copyright 2026 Super David Universe LLC
SPDX-License-Identifier: AGPL-3.0-only
"""
import argparse
import hashlib
import json
import re
import shutil
import zipfile
from pathlib import Path


SPECS = {
    "0.26.0": (
        "index-Bz16m3ir.js",
        "afed02c82fc563f4c0ee2673b33195b8128da1f79fd25e9badc613efe42b9e92",
        "outcome$:$pe(c,l,u).pipe(oO(1),e.share)",
        "outcome$:$pe(c,l,u).pipe(oO(1),s=>e.behavior(s,null).pipe(iO(v=>v!==null),oO(1)))",
    ),
    "0.25.0": (
        "index-DPkEeOAp.js",
        "d430e06908dcf717538fb91a8f6bc85f36f0fb90a2924f429b6848a81477665b",
        "outcome$:PS(c,l,u).pipe(zS(1),e.share)",
        "outcome$:PS(c,l,u).pipe(zS(1),s=>e.behavior(s,null).pipe(NS(v=>v!==null),zS(1)))",
    ),
}


def patch(content, version):
    name, digest, old, new = SPECS[version]
    if hashlib.sha256(content).hexdigest() != digest:
        raise ValueError(f"Unexpected embedded call artifact: {name}; revalidate the source patch")
    text = content.decode("utf-8")
    if text.count(old) != 1:
        raise ValueError("Expected exactly one pickup outcome; refusing a broad replacement")
    # Never ship a source-map reference with stale generated offsets.
    return re.sub(r"(?m)^//# sourceMappingURL=[^\n]*$", "", text.replace(old, new)).encode("utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", choices=SPECS, required=True)
    parser.add_argument("--aar", type=Path)
    parser.add_argument("--dist", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if (args.aar is None) == (args.dist is None):
        parser.error("Choose exactly one pinned input: --aar or --dist")
    if args.dist and args.output.resolve() == args.dist.resolve():
        parser.error("The pinned dependency must remain unchanged")
    name = SPECS[args.version][0]
    relative = Path("assets") / name
    if args.aar:
        with zipfile.ZipFile(args.aar) as archive:
            original = archive.read("assets/element-call/" + relative.as_posix())
        destination = args.output / "element-call-antintwrk-pickup-v1"
    else:
        original = (args.dist / relative).read_bytes()
        destination = args.output
    updated = patch(original, args.version)
    if args.output.exists():
        shutil.rmtree(args.output)
    if args.aar:
        with zipfile.ZipFile(args.aar) as archive:
            for entry in archive.infolist():
                if not entry.filename.startswith("assets/element-call/") or entry.is_dir():
                    continue
                asset = Path(entry.filename[len("assets/element-call/"):])
                if asset.is_absolute() or ".." in asset.parts:
                    raise ValueError("Unsafe dependency asset path")
                target = destination / asset
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(entry))
    else:
        shutil.copytree(args.dist, destination)
    (destination / (relative.as_posix() + ".map")).unlink(missing_ok=True)
    target = destination / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists():
        target.chmod(0o644)
    target.write_bytes(updated)
    (destination / "antintwrk-call-patch.json").write_text(json.dumps({
        "patch": "call-pickup-replay-v1",
        "upstream": args.version,
        "input_sha256": hashlib.sha256(original).hexdigest(),
        "output_sha256": hashlib.sha256(updated).hexdigest(),
    }, indent=2) + "\n")
    print(f"Verified call pickup replay overlay for embedded {args.version}")


if __name__ == "__main__":
    main()
