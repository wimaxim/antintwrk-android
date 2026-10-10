#!/usr/bin/env python3
"""Convert the public Firebase Android download to local resources; never reads server credentials."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("config", type=Path)
parser.add_argument("output", type=Path)
parser.add_argument("--package", choices=["com.antintwrk.app.debug", "com.antintwrk.app"], default="com.antintwrk.app.debug")
args = parser.parse_args()
config = json.loads(args.config.read_text())
project = config["project_info"]
if project["project_id"] != "effective-fire-159508" or project["project_number"] != "889486529036":
    raise SystemExit("Expected the antintwrk Firebase project.")
client = next((item for item in config["client"] if item["client_info"].get("android_client_info", {}).get("package_name") == args.package), None)
if client is None:
    raise SystemExit("Expected the requested antintwrk Android client.")
values = {
    "google_app_id": client["client_info"]["mobilesdk_app_id"],
    "google_api_key": client["api_key"][0]["current_key"],
    "google_crash_reporting_api_key": client["api_key"][0]["current_key"],
    "gcm_defaultSenderId": project["project_number"],
    "project_id": project["project_id"],
    "google_storage_bucket": project.get("storage_bucket", ""),
    "firebase_database_url": "",
    "default_web_client_id": "889486529036-vdragtfp74n4g1884pl3h0ur82b855rv.apps.googleusercontent.com",
}
root = ET.Element("resources")
for name, value in values.items():
    ET.SubElement(root, "string", name=name, translatable="false").text = value
destination = args.output / "values"
destination.mkdir(parents=True, exist_ok=True)
ET.ElementTree(root).write(destination / "firebase.xml", encoding="utf-8", xml_declaration=True)
(args.output / "package-name").write_text(args.package + "\n")
print("Own Firebase client resources prepared; key values not emitted.")
