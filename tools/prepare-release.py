#!/usr/bin/env python3
"""Prepare the already signed/tested APK; signing keys never enter the release job."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import urllib.request

PUBLISH_FILES = {
    ".github/workflows/release.yml", "tools/prepare-release.py",
    "releases/current.json", "README.md",
}


def validate_manifest(manifest):
    if not re.fullmatch(r"v\d+\.\d+\.\d+", manifest["tag"]):
        raise ValueError("Invalid release tag")
    if manifest["asset_name"] != "WohnPit-" + manifest["tag"] + ".apk":
        raise ValueError("Unexpected APK filename")
    for key, length in [("tested_commit", 40), ("asset_blob_sha", 40), ("asset_sha256", 64)]:
        if not re.fullmatch(r"[a-f0-9]{" + str(length) + "}", manifest[key]):
            raise ValueError("Invalid " + key)
    if not isinstance(manifest["asset_size"], int) or not 10000 <= manifest["asset_size"] <= 50 * 1024 * 1024:
        raise ValueError("Unexpected APK size")


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
        if any(row["filename"] not in PUBLISH_FILES or row.get("previous_filename", row["filename"]) not in PUBLISH_FILES for row in comparison["files"]):
            raise ValueError("Untested app changes must not be included in this release")
    blob = api("git/blobs/" + manifest["asset_blob_sha"])
    if blob.get("encoding") != "base64" or blob.get("sha") != manifest["asset_blob_sha"]:
        raise ValueError("Unexpected APK object")
    apk = base64.b64decode("".join(blob["content"].split()), validate=True)
    if len(apk) != manifest["asset_size"] or hashlib.sha256(apk).hexdigest() != manifest["asset_sha256"]:
        raise ValueError("The release APK differs from the signed and tested download")
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
    destination = output / manifest["asset_name"]
    destination.write_bytes(apk)
    (output / "SHA256SUMS").write_text(manifest["asset_sha256"] + "  " + manifest["asset_name"] + "\n")
    (output / "notes.md").write_text(body)
    return {"tag": manifest["tag"], "title": "WohnPit " + manifest["tag"][1:], "apk": str(destination)}


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
    with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
        for key, value in outputs.items():
            stream.write(key + "=" + value + "\n")
    print("Verified original signed APK and successful source checks for " + outputs["tag"])


if __name__ == "__main__":
    main()
