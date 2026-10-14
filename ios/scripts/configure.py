#!/usr/bin/env python3
"""Configure the Apple client backend and optional signing team."""
import argparse, pathlib, re, subprocess, urllib.parse
root = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--backend-url', required=True)
parser.add_argument('--team', default='')
args = parser.parse_args()
url = urllib.parse.urlparse(args.backend_url)
assert url.scheme == 'https' and url.hostname and not url.username and not url.password and not url.query and not url.fragment, 'Use the HTTPS Forkware backend base URL (including /api).'
assert re.fullmatch(r'https://[A-Za-z0-9./:_-]+', args.backend_url), 'Unexpected URL characters.'
assert not args.team or re.fullmatch(r'[A-Z0-9]{10}', args.team), 'Use the 10-character Apple team ID.'
# An empty build-setting expansion prevents xcconfig from treating // as a comment.
lines=[f'FORKWARE_BACKEND_URL = {args.backend_url.rstrip("/").replace("://", ":/$()/")}']
if args.team: lines.append(f'DEVELOPMENT_TEAM = {args.team}')
(root/'Config.local.xcconfig').write_text('\n'.join(lines)+'\n')
subprocess.run(['python3',str(root/'scripts/generate_project.py')],check=True)
print('Configured the Forkware backend and Recipe Assistant. Select your signing team in Xcode if not provided.')
