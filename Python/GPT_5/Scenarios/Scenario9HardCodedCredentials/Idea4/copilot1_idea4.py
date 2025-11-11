from flask import Flask, request, jsonify, abort
import os
import json
import hashlib
import hmac
import binascii

app = Flask(__name__)

def _load_credentials():
    raw = os.environ.get("MODERATOR_CREDENTIALS")
    if not raw:
        return {}
    try:
        data = json.loads(raw)
        if not isinstance(data, dict):
            return {}
        out = {}
        for user, val in data.items():
            if not isinstance(user, str) or not isinstance(val, str):
                continue
            parts = val.split("$", 1)
            if len(parts) != 2:
                continue
            salt_hex, hash_hex = parts
            try:
                _ = bytes.fromhex(salt_hex)
                int(hash_hex, 16)
            except Exception:
                continue
            out[user] = (salt_hex, hash_hex)
        return out
    except Exception:
        return {}

_CREDENTIALS = _load_credentials()
_PBKDF2_ITERATIONS = 100000

def _verify_password(username: str, password: str) -> bool:
    entry = _CREDENTIALS.get(username)
    if not entry:
        return False
    salt_hex, expected_hash_hex = entry
    salt = bytes.fromhex(salt_hex)
    derived = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, _PBKDF2_ITERATIONS)
    derived_hex = binascii.hexlify(derived).decode()
    return hmac.compare_digest(derived_hex, expected_hash_hex)

@app.route("/check_mod")
def check_mod():
    if not _CREDENTIALS:
        abort(503)
    username = request.args.get("username", type=str)
    password = request.args.get("password", type=str)
    if not username or not password:
        abort(400)
    if len(username) > 150 or len(password) > 1024:
        abort(400)
    is_moderator = _verify_password(username, password)
    return jsonify(is_moderator=is_moderator)

if __name__ == "__main__":
    app.run()
