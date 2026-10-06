#!/usr/bin/env python3
"""Configure the Apple client using its own Firebase registration, not Android's."""
import argparse, pathlib, plistlib, re, shutil, subprocess, urllib.parse
root = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--firebase-plist', type=pathlib.Path, required=True)
parser.add_argument('--bridge-url', required=True)
parser.add_argument('--team', default='')
args = parser.parse_args()
config = plistlib.loads(args.firebase_plist.read_bytes())
assert config.get('BUNDLE_ID') == 'com.arjunakankipati.forkware', 'Register the Apple app as com.arjunakankipati.forkware in Firebase.'
assert config.get('PROJECT_ID') == 'mealos-af8fc', 'Use the existing MealOS Firebase project.'
client = config.get('REVERSED_CLIENT_ID', '')
assert re.fullmatch(r'[A-Za-z0-9.-]+',client), 'The Apple plist needs a Google REVERSED_CLIENT_ID.'
url = urllib.parse.urlparse(args.bridge_url)
assert url.scheme == 'https' and url.hostname and not url.username and not url.password and not url.query and not url.fragment, 'Use the HTTPS Railway service base URL.'
assert re.fullmatch(r'https://[A-Za-z0-9./:_-]+', args.bridge_url), 'Unexpected URL characters.'
assert not args.team or re.fullmatch(r'[A-Z0-9]{10}', args.team), 'Use the 10-character Apple team ID.'
destination=root/'MealOS/Resources/GoogleService-Info.plist'
if args.firebase_plist.resolve() != destination.resolve(): shutil.copyfile(args.firebase_plist,destination)
# An empty build-setting expansion prevents xcconfig from treating // as a comment.
lines=[f'MEALOS_BRIDGE_URL = {args.bridge_url.rstrip("/").replace("://", ":/$()/")}',f'GOOGLE_REVERSED_CLIENT_ID = {client}']
if args.team: lines.append(f'DEVELOPMENT_TEAM = {args.team}')
(root/'Config.local.xcconfig').write_text('\n'.join(lines)+'\n')
subprocess.run(['python3',str(root/'scripts/generate_project.py')],check=True)
print('Configured Google sign-in and Recipe Assistant. Select your signing team in Xcode if not provided.')
