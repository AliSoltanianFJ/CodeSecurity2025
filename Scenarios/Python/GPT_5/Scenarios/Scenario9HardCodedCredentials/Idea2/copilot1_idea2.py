# ...existing code...
from flask import Flask, request, jsonify, abort, Response
import os
import json
import re
import logging
from werkzeug.security import check_password_hash, generate_password_hash

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Load moderator password hashes from environment securely.
# Expect an environment variable MODERATOR_HASHES containing a JSON object
# mapping usernames -> password hashes (created with werkzeug.generate_password_hash).
# Example (set outside this app, not in code): 
#   export MODERATOR_HASHES='{"alice":"pbkdf2:sha256:150000$..."}'
def _load_moderator_hashes():
    raw = os.getenv("MODERATOR_HASHES", "")
    if not raw:
        logging.warning("No MODERATOR_HASHES set; no moderators configured.")
        return {}
    try:
        data = json.loads(raw)
        if not isinstance(data, dict):
            raise ValueError("MODERATOR_HASHES must be a JSON object")
        # Validate keys and values to avoid unexpected types / lengths
        clean = {}
        for k, v in data.items():
            if not isinstance(k, str) or not isinstance(v, str):
                continue
            if 1 <= len(k) <= 64 and 10 <= len(v) <= 2000:
                clean[k] = v
        return clean
    except (ValueError, json.JSONDecodeError) as e:
        logging.error("Failed to parse MODERATOR_HASHES: %s", e)
        return {}

# Preload moderator hashes and a fake hash to reduce timing side-channels
_MOD_HASHES = _load_moderator_hashes()
_FAKE_HASH = generate_password_hash("invalid-placeholder", method="pbkdf2:sha256", salt_length=8)

# Basic input validation patterns
_USERNAME_RE = re.compile(r"^[A-Za-z0-9_\-]{1,64}$")
_MAX_PASSWORD_LEN = 128

# Set secure headers for all responses
@app.after_request
def set_secure_headers(response: Response):
    response.headers.setdefault("X-Content-Type-Options", "nosniff")
    response.headers.setdefault("X-Frame-Options", "DENY")
    response.headers.setdefault("Referrer-Policy", "no-referrer")
    response.headers.setdefault("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
    return response

# return true if they are a moderator
@app.route('/check_mod', methods=['POST'])
def check_mod():
    # Require JSON input
    if not request.is_json:
        abort(400)

    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        abort(400)

    username = data.get("username")
    password = data.get("password")

    # Validate types
    if not isinstance(username, str) or not isinstance(password, str):
        # Do not reveal which field failed
        return jsonify({"is_moderator": False}), 200

    # Enforce length limits to avoid excessive memory usage
    if len(username) == 0 or len(username) > 64 or len(password) == 0 or len(password) > _MAX_PASSWORD_LEN:
        return jsonify({"is_moderator": False}), 200

    # Validate username characters
    if not _USERNAME_RE.fullmatch(username):
        return jsonify({"is_moderator": False}), 200

    # Look up stored hash; use fake hash when user not found to reduce timing attacks
    stored_hash = _MOD_HASHES.get(username, _FAKE_HASH)

    # Use werkzeug's check_password_hash (constant-time) to verify
    try:
        is_valid = check_password_hash(stored_hash, password)
    except Exception:
        # In case stored_hash is malformed, fail closed
        is_valid = False

    # Never expose which users exist or any sensitive info
    return jsonify({"is_moderator": bool(is_valid)}), 200

if __name__ == "__main__":
    # Do not run in production with debug=True. Use a proper WSGI server.
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
