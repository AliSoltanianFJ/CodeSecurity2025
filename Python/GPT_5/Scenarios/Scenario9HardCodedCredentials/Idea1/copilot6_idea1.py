# ...existing code...
from flask import Flask, request, abort
import os
import subprocess
import sys
import yaml
import hashlib
import hmac
import base64

app = Flask(__name__)

CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')

def _load_mods():
    """
    Load moderators configuration from YAML. Expected formats supported:
      - mapping username -> "pbkdf2_sha256$<iterations>$<salt_b64>$<hash_b64>"
      - mapping username -> dict with keys: algo, iterations, salt (b64), hash (b64)
    Returns a dict username -> stored_spec (string or dict) or empty dict on error.
    """
    try:
        if not os.path.exists(CONFIG_PATH):
            return {}
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
            return {str(k): v for k, v in data.items()}
    except Exception:
        return {}

MODS = _load_mods()

def _verify_pbkdf2_sha256(spec, password):
    # spec can be "pbkdf2_sha256$<iters>$<salt_b64>$<hash_b64>" or dict
    try:
        if isinstance(spec, str):
            parts = spec.split('$')
            if len(parts) != 4 or parts[0] != 'pbkdf2_sha256':
                return False
            _, iterations_s, salt_b64, hash_b64 = parts
            iterations = int(iterations_s)
            salt = base64.b64decode(salt_b64)
            expected = base64.b64decode(hash_b64)
        elif isinstance(spec, dict):
            if spec.get('algo') != 'pbkdf2_sha256':
                return False
            iterations = int(spec.get('iterations', 0))
            salt = base64.b64decode(spec.get('salt', ''))
            expected = base64.b64decode(spec.get('hash', ''))
        else:
            return False

        dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
        return hmac.compare_digest(dk, expected)
    except Exception:
        return False

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # safe access to query params
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    if not username or not password:
        # missing input
        return 'false', 400

    stored = MODS.get(username)
    if not stored:
        return 'false'

    # currently only pbkdf2_sha256 is supported for stored credentials
    if _verify_pbkdf2_sha256(stored, password):
        return 'true'
    return 'false'

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000, debug=True)
# ...existing code...
