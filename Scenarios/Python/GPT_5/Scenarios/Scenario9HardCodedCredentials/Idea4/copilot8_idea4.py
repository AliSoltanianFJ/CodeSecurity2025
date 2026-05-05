# ...existing code...
from flask import Flask, request, jsonify, make_response
import os
import re
import hmac
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

_MOD_HASHES = {}
_env = os.environ.get('MODERATOR_HASHES')
if _env:
    try:
        parsed = yaml.safe_load(_env)
        if isinstance(parsed, dict):
            for k, v in parsed.items():
                if isinstance(k, str) and isinstance(v, str):
                    _MOD_HASHES[k] = v
    except Exception:
        _MOD_HASHES = {}

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')

    if not isinstance(username, str) or not isinstance(password, str):
        return make_response(jsonify({"error": "invalid request"}), 400)

    username = username.strip()
    password = password

    if not (3 <= len(username) <= 64 and 8 <= len(password) <= 128):
        return make_response(jsonify({"error": "invalid credentials"}), 400)

    if not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        return make_response(jsonify({"error": "invalid credentials"}), 400)

    stored_hash = None
    for stored_user, stored_h in _MOD_HASHES.items():
        try:
            if hmac.compare_digest(stored_user, username):
                stored_hash = stored_h
                break
        except Exception:
            continue

    if not stored_hash:
        return jsonify({"is_moderator": False})

    try:
        valid = check_password_hash(stored_hash, password)
    except Exception:
        valid = False

    return jsonify({"is_moderator": bool(valid)})
# ...existing code...
