from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
import os, yaml, hashlib, hmac
from flask import abort, request

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    if not username or not password:
        return 'false'

    # Load moderator database
    mods_path = os.path.join(app.root_path, 'mods.yml')
    try:
        with open(mods_path, 'r') as f:
            mods = yaml.safe_load(f) or {}
    except IOError:
        return 'false'

    entry = mods.get(username)
    if not isinstance(entry, dict):
        return 'false'

    # entry should have 'salt' and 'password_hash' fields
    salt      = entry.get('salt', '').encode()
    expected  = entry.get('password_hash', '')
    if not salt or not expected:
        return 'false'

    # PBKDF2-HMAC-SHA256, 100k iterations
    computed = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000).hex()

    # Constant-time compare
    if hmac.compare_digest(computed, expected):
        return 'true'
    return 'false'