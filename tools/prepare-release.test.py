import base64
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("release", Path(__file__).with_name("prepare-release.py"))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleasePreparationTest(unittest.TestCase):
    def fixture(self, helper=True):
        source = "a" * 40
        data = {"b" * 40: b"PK" + b"example-main" * 1200, "c" * 40: b"PK" + b"example-helper" * 1200}
        def asset(name, key):
            return {"asset_name": name, "asset_blob_sha": key, "asset_size": len(data[key]), "asset_sha256": hashlib.sha256(data[key]).hexdigest()}
        manifest = {"tag": "v2.0.2", "tested_commit": source, "tested_workflow_run": 1, "unit_tests": 1, "integration_checks": 1, "android_ui_checks": 3, **asset("WohnPit-v2.0.2.apk", "b" * 40)}
        if helper:
            manifest["helpers"] = [asset("WohnPit-Uebernahme-v0.0.5.apk", "c" * 40)]
        def api(path):
            if path == "actions/runs/1":
                return {"head_sha": source, "conclusion": "success", "path": ".github/workflows/android.yml", "repository": {"full_name": "example/WohnPit"}}
            if path.startswith("git/blobs/"):
                key = path.rsplit("/", 1)[1]
                return {"sha": key, "encoding": "base64", "content": base64.b64encode(data[key]).decode()}
            if path.startswith("contents/"):
                return {"encoding": "base64", "content": base64.b64encode(b"# Example release\n").decode()}
            if path.startswith("compare/"):
                return {"status": "ahead", "files": [{"filename": "README.md"}]}
            raise AssertionError(path)
        return manifest, api, source

    def test_main_and_migration_helper_have_verified_bytes_and_checksums(self):
        manifest, api, source = self.fixture()
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory)
            release.prepare(manifest, "example/WohnPit", source, api, out)
            paths = json.loads((out / "assets.json").read_text())
            self.assertEqual([Path(p).name for p in paths], ["WohnPit-v2.0.2.apk", "WohnPit-Uebernahme-v0.0.5.apk", "SHA256SUMS"])
            self.assertEqual(len((out / "SHA256SUMS").read_text().splitlines()), 2)
            for entry in [manifest, *manifest["helpers"]]:
                self.assertEqual(hashlib.sha256((out / entry["asset_name"]).read_bytes()).hexdigest(), entry["asset_sha256"])

    def test_existing_manifests_without_helpers_remain_supported(self):
        manifest, api, source = self.fixture(False)
        with tempfile.TemporaryDirectory() as directory:
            release.prepare(manifest, "example/WohnPit", source, api, Path(directory))
            self.assertEqual(len(json.loads((Path(directory) / "assets.json").read_text())), 2)

    def test_tampered_helper_stops_before_any_release_asset_is_materialized(self):
        manifest, api, source = self.fixture()
        manifest["helpers"][0]["asset_sha256"] = "0" * 64
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError):
                release.prepare(manifest, "example/WohnPit", source, api, Path(directory))
            self.assertEqual(list(Path(directory).iterdir()), [])

    def test_private_or_duplicate_helper_names_are_rejected(self):
        for name in ["../private-backup.wohnpit", "signing.json", "WohnPit-v2.0.2.apk"]:
            manifest, _, _ = self.fixture()
            manifest["helpers"][0]["asset_name"] = name
            with self.assertRaises(ValueError):
                release.validate_manifest(manifest)

    def test_readme_only_publication_commit_remains_supported(self):
        manifest, api, _ = self.fixture()
        with tempfile.TemporaryDirectory() as directory:
            release.prepare(manifest, "example/WohnPit", "d" * 40, api, Path(directory))
            self.assertTrue((Path(directory) / manifest["asset_name"]).is_file())


if __name__ == "__main__":
    unittest.main()
