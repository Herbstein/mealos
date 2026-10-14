#!/usr/bin/env python3
"""Run integration checks on a disposable Postgres; never use DATABASE_URL."""
import os
import pathlib
import shutil
import socket
import subprocess
import tempfile
import time

root = pathlib.Path(__file__).resolve().parents[1]
for program in ('initdb', 'postgres', 'pg_isready', 'cargo'):
    if not shutil.which(program):
        raise SystemExit(f'{program} must be on PATH')
with tempfile.TemporaryDirectory(prefix='forkware-pg-') as directory:
    subprocess.run(['initdb', '-D', directory, '-A', 'trust', '-U', 'forkware_test', '--no-locale', '-E', 'UTF8'], check=True, stdout=subprocess.DEVNULL)
    with socket.socket() as listener:
        listener.bind(('127.0.0.1', 0))
        port = str(listener.getsockname()[1])
    database = subprocess.Popen(['postgres', '-D', directory, '-h', '127.0.0.1', '-p', port, '-k', ''], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        for _ in range(100):
            if subprocess.run(['pg_isready', '-h', '127.0.0.1', '-p', port], stdout=subprocess.DEVNULL).returncode == 0:
                break
            if database.poll() is not None:
                raise SystemExit('Temporary Postgres exited')
            time.sleep(.1)
        else:
            raise SystemExit('Temporary Postgres did not start')
        environment = dict(os.environ, TEST_DATABASE_URL=f'postgresql://forkware_test@127.0.0.1:{port}/postgres')
        result = subprocess.run(['cargo', 'test', '--locked', '--features', 'test-fixtures', '--', '--include-ignored'], cwd=root, env=environment)
    finally:
        database.terminate()
        try:
            database.wait(timeout=10)
        except subprocess.TimeoutExpired:
            database.kill()
            database.wait()
    raise SystemExit(result.returncode)
