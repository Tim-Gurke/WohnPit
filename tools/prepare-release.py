#!/usr/bin/env python3
"""Prepare the already signed/tested APK; signing keys never enter the release job."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.request

PUBLISH_FILES = {
    ".github/workflows/release.yml", "tools/prepare-release.py",
    "releases/current.json", "README.md",
}


def publish_file(path):
    return path in PUBLISH_FILES or bool(re.fullmatch(r"releases/artifacts/v[0-9]+\.[0-9]+\.[0-9]+/part-[0-9]{2}\.b64",path)) or bool(re.fullmatch(r"docs/images/autarpie-[0-9]+\.[0-9]+\.[0-9]+-(?:dark|light|settings|central)\.svg",path))


def verify_apk(path,pin):
    sdk=Path(os.environ.get("ANDROID_HOME",os.environ.get("ANDROID_SDK_ROOT","/usr/local/lib/android/sdk")))
    signers=sorted(sdk.glob("build-tools/*/apksigner"),reverse=True)
    if not signers:raise ValueError("Android apksigner is required before publishing")
    result=subprocess.run([str(signers[0]),"verify","--print-certs",str(path)],check=True,text=True,capture_output=True)
    actual=re.findall(r"certificate SHA-256 digest: ([a-f0-9]+)",result.stdout)
    if actual!=[pin]:raise ValueError("The APK does not carry the original installed update signature")


def validate_manifest(manifest):
    if not re.fullmatch(r"v\d+\.\d+\.\d+", manifest["tag"]):
        raise ValueError("Invalid release tag")
    if manifest["asset_name"] not in ["WohnPit-"+manifest["tag"]+".apk","AutarPie-"+manifest["tag"]+".apk"]:
        raise ValueError("Unexpected APK filename")
    for key, length in [("tested_commit", 40), ("asset_blob_sha", 40), ("asset_sha256", 64)]:
        if not re.fullmatch(r"[a-f0-9]{" + str(length) + "}", manifest[key]):
            raise ValueError("Invalid " + key)
    if not isinstance(manifest["asset_size"], int) or not 10000 <= manifest["asset_size"] <= 50 * 1024 * 1024:
        raise ValueError("Unexpected APK size")
    if manifest["asset_name"].startswith("AutarPie-") and not re.fullmatch(r"[a-f0-9]{64}",manifest.get("signer_sha256","")):
        raise ValueError("Original update signer fingerprint is required")
    if manifest.get("asset_encoding") is not None:
        parts=manifest.get("asset_blob_shas",[])
        if manifest["asset_encoding"]!="base64-parts" or not isinstance(parts,list) or not 1<=len(parts)<=64 or any(not re.fullmatch(r"[a-f0-9]{40}",part) for part in parts) or parts[0]!=manifest["asset_blob_sha"]:
            raise ValueError("Invalid signed APK parts")
    helpers = manifest.get("helpers", [])
    if not isinstance(helpers, list) or len(helpers) > 4:
        raise ValueError("Invalid release helpers")
    names = {manifest["asset_name"]}
    for helper in helpers:
        name = helper.get("asset_name", "")
        if not re.fullmatch(r"WohnPit-[A-Za-z0-9.-]+\.apk", name) or name in names:
            raise ValueError("Invalid helper APK filename")
        names.add(name)
        if not re.fullmatch(r"[a-f0-9]{40}", helper.get("asset_blob_sha", "")) or not re.fullmatch(r"[a-f0-9]{64}", helper.get("asset_sha256", "")):
            raise ValueError("Invalid helper APK checksum")
        if not isinstance(helper.get("asset_size"), int) or not 10000 <= helper["asset_size"] <= 50 * 1024 * 1024:
            raise ValueError("Unexpected helper APK size")


def prepare(manifest, repository, target, api, output):
    validate_manifest(manifest)
    source = manifest["tested_commit"]
    run = api("actions/runs/" + str(int(manifest["tested_workflow_run"])))
    if (run.get("head_sha") != source or run.get("conclusion") != "success"
            or run.get("path") != ".github/workflows/android.yml"
            or run.get("repository", {}).get("full_name") != repository):
        raise ValueError("The APK source has no matching successful Android verification run")
    if target != source:
        comparison = api("compare/" + source + "..." + target)
        if comparison.get("status") not in ("ahead", "identical") or "files" not in comparison:
            raise ValueError("The release target must descend from the tested source")
        if any(not publish_file(row["filename"]) or not publish_file(row.get("previous_filename",row["filename"])) for row in comparison["files"]):
            raise ValueError("Untested app changes must not be included in this release")
    assets = []
    for entry in [manifest, *manifest.get("helpers", [])]:
        parts=[]
        for sha in entry.get("asset_blob_shas",[entry["asset_blob_sha"]]):
            blob=api("git/blobs/"+sha)
            if blob.get("encoding")!="base64" or blob.get("sha")!=sha:raise ValueError("Unexpected APK object")
            parts.append(base64.b64decode("".join(blob["content"].split()),validate=True))
        apk=base64.b64decode(b"".join(parts),validate=True) if entry.get("asset_encoding")=="base64-parts" else parts[0]
        if len(apk) != entry["asset_size"] or hashlib.sha256(apk).hexdigest() != entry["asset_sha256"]:
            raise ValueError("A release APK differs from the signed and tested download")
        assets.append((entry, apk))
    note_path = "docs/changes-" + manifest["tag"][1:] + ".md"
    notes = api("contents/" + note_path + "?ref=" + source)
    if notes.get("encoding") != "base64":
        raise ValueError("Release notes are missing from the tested source")
    body = base64.b64decode(notes["content"]).decode("utf-8")
    body = body.replace("](GAS-WASSER.md)", "](https://github.com/" + repository + "/blob/main/docs/GAS-WASSER.md)")
    body = body.replace("](../CHANGELOG.md)", "](https://github.com/" + repository + "/blob/main/CHANGELOG.md)")
    body += ("\n\nGeprüfter APK-Quellstand: `" + source + "`.\n\n"
             "Validierung: " + str(manifest["unit_tests"]) + " Tests, "
             + str(manifest["integration_checks"]) + " Integrationsprüfungen und "
             + str(manifest["android_ui_checks"]) + " Android-Oberflächenprüfungen bestanden. "
             "Funkverbindungen und Stromverbrauch am echten Gerät wurden hier nicht gemessen.\n\n"
             "Installation und Update-Reihenfolge wie oben beschrieben beachten. "
             "Persönliche Sicherungen bleiben privat und gehören niemals in öffentliche Releases.\n")
    output.mkdir(parents=True, exist_ok=True)
    paths = []
    for entry, apk in assets:
        destination = output / entry["asset_name"]
        destination.write_bytes(apk)
        paths.append(str(destination))
    (output / "SHA256SUMS").write_text("".join(entry["asset_sha256"] + "  " + entry["asset_name"] + "\n" for entry, _ in assets))
    paths.append(str(output / "SHA256SUMS"))
    (output / "assets.json").write_text(json.dumps(paths))
    (output / "notes.md").write_text(body)
    return {"tag": manifest["tag"], "title": ("AutarPie " if manifest["asset_name"].startswith("AutarPie-") else "WohnPit ")+manifest["tag"][1:], "apk": paths[0]}


def main():
    repository = os.environ["GITHUB_REPOSITORY"]
    token = os.environ["GH_TOKEN"]
    if not re.fullmatch(r"[\w.-]+/[\w.-]+", repository):
        raise ValueError("Invalid repository")

    def api(path):
        request = urllib.request.Request("https://api.github.com/repos/" + repository + "/" + path,
            headers={"Authorization": "Bearer " + token, "Accept": "application/vnd.github+json",
                     "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "WohnPit-release"})
        with urllib.request.urlopen(request, timeout=90) as response:
            return json.load(response)

    manifest = json.loads(Path("releases/current.json").read_text())
    outputs = prepare(manifest, repository, os.environ["GITHUB_SHA"], api, Path("release-out"))
    if manifest.get("signer_sha256"):verify_apk(Path(outputs["apk"]),manifest["signer_sha256"])
    with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
        for key, value in outputs.items():
            stream.write(key + "=" + value + "\n")
    print("Verified original signed APK and successful source checks for " + outputs["tag"])


if __name__ == "__main__":
    main()
