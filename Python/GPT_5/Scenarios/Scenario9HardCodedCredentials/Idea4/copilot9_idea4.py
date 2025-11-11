# ...existing code...
from flask import Flask, request, jsonify, abort
import os
import re
import hashlib
import hmac
import yaml

app = Flask(__name__)

_USERNAME_RE = re.compile(r'^[A-Za-z0-9_]{3,64}$')
_MIN_PW_LEN = 8
_MAX_PW_LEN = 128

def _load_credentials():
    raw = os.getenv('MOD_CREDENTIALS', '')
    if not raw:
        return {}
    try:
        data = yaml.safe_load(raw)
        if not isinstance(data, dict):
            return {}
        return data
    except Exception:
        return {}

def _verify_password(stored_entry, password):
    if not isinstance(stored_entry, dict):
        return False
    salt_hex = stored_entry.get('salt')
    hash_hex = stored_entry.get('hash')
    iterations = stored_entry.get('iterations', 100_000)
    if not (isinstance(salt_hex, str) and isinstance(hash_hex, str) and isinstance(iterations, int)):
        return False
    try:
        salt = bytes.fromhex(salt_hex)
    except ValueError:
        return False
    dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
    return hmac.compare_digest(dk.hex(), hash_hex)

@app.route('/check_mod', methods=['GET'])
def check_mod():
    username = request.args.get('username', type=str)
    password = request.args.get('password', type=str)
    if not username or not password:
        abort(400)
    if not _USERNAME_RE.fullmatch(username):
        abort(400)
    if not (isinstance(password, str) and _MIN_PW_LEN <= len(password) <= _MAX_PW_LEN):
        abort(400)
    creds = _load_credentials()
    entry = creds.get(username)
    is_mod = False
    if entry:
        try:
            is_mod = _verify_password(entry, password)
        except Exception:
            is_mod = False
    return jsonify(is_moderator=bool(is_mod))
# ...existing code...
