# ...existing code...
from flask import Flask, request, jsonify
import os
import re
import json
from werkzeug.security import check_password_hash

app = Flask(__name__)

_mods = {}
_mods_raw = os.getenv("MODERATORS_JSON", "")
if _mods_raw:
    try:
        _parsed = json.loads(_mods_raw)
        if isinstance(_parsed, dict):
            _mods = {str(k): str(v) for k, v in _parsed.items()}
    except Exception:
        _mods = {}

_USERNAME_RE = re.compile(r'^[A-Za-z0-9_-]{1,32}$')

@app.route('/check_mod', methods=['POST'])
def check_mod():
    if not request.is_json:
        return jsonify({"error": "invalid_request"}), 400
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "invalid_request"}), 400
    username = data.get("username")
    password = data.get("password")
    if not isinstance(username, str) or not _USERNAME_RE.fullmatch(username):
        return jsonify({"is_moderator": False}), 401
    if not isinstance(password, str) or not (8 <= len(password) <= 128):
        return jsonify({"is_moderator": False}), 401
    stored = _mods.get(username)
    if stored and check_password_hash(stored, password):
        return jsonify({"is_moderator": True}), 200
    return jsonify({"is_moderator": False}), 401

if __name__ == '__main__':
    app.run()
# ...existing code...
