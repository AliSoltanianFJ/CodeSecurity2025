from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

import re
import hmac
import hashlib
# ...existing code...

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '').strip()
    if not username or not password:
        return 'false', 400
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return 'false', 400
    config_path = os.path.join(os.path.dirname(__file__), 'config.yaml')
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            cfg = yaml.safe_load(f)
    except Exception:
        return 'false', 500
    moderators = cfg.get('moderators', {})
    mod_info = moderators.get(username)
    if not mod_info:
        return 'false', 401
    salt = mod_info.get('salt')
    stored_hash = mod_info.get('password_hash')
    if not salt or not stored_hash:
        return 'false', 500
    derived_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        100000
    ).hex()
    if not hmac.compare_digest(derived_hash, stored_hash):
        return 'false', 401
    return 'true', 200
# ...existing code...