# ...existing code...
from flask import Flask, request, jsonify
import os
import json
import re
from werkzeug.security import check_password_hash, generate_password_hash
import logging

# Application setup
app = Flask(__name__)

# Limit request body size to mitigate large payload attacks (1 KB is enough for credentials)
app.config['MAX_CONTENT_LENGTH'] = 1024

# Configure logging conservatively (do not log request bodies or secrets)
logging.getLogger("werkzeug").setLevel(logging.INFO)
logger = logging.getLogger(__name__)

# Load moderator password hashes from environment variable (JSON mapping: username -> password_hash).
# This avoids hard-coding secrets in source code and avoids unsafe deserialization.
# Example env value: {"alice":"pbkdf2:sha256:150000$...","bob":"pbkdf2:sha256:150000$..."}
MOD_HASHES = {}
try:
    env = os.environ.get("MODERATOR_HASHES", "{}")
    parsed = json.loads(env)
    if isinstance(parsed, dict):
        # Ensure keys and values are strings
        MOD_HASHES = {str(k): str(v) for k, v in parsed.items()}
except Exception:
    # Fail closed: leave MOD_HASHES empty rather than crashing or exposing details
    MOD_HASHES = {}

# Dummy hash used to mitigate timing/user enumeration attacks when username is missing
_DUMMY_HASH = generate_password_hash("dummy_password_for_timing_mitigation")

# Validation helpers
_USERNAME_RE = re.compile(r"^[A-Za-z0-9_.-]{3,64}$")


def _valid_username(u):
    return isinstance(u, str) and _USERNAME_RE.fullmatch(u) is not None


def _valid_password(p):
    return isinstance(p, str) and 8 <= len(p) <= 128


@app.route('/check_mod', methods=['POST'])
def check_mod():
    """
    Accepts JSON body: {"username": "...", "password": "..."}
    Returns JSON: {"is_moderator": true|false}
    - Validates input
    - Avoids reflecting user input (prevent XSS)
    - Uses secure password hash verification and timing mitigations
    - Does not reveal whether username exists (prevents enumeration)
    """
    # Require JSON content type
    if not request.is_json:
        return jsonify({"error": "invalid_request"}), 400

    try:
        data = request.get_json(silent=False)
    except Exception:
        # Parsing failed or invalid JSON
        return jsonify({"error": "invalid_request"}), 400

    # Extract and validate inputs
    username = data.get("username")
    password = data.get("password")

    if not (_valid_username(username) and _valid_password(password)):
        # Perform dummy verification to make response timing consistent
        try:
            check_password_hash(_DUMMY_HASH, password if isinstance(password, str) else "")
        except Exception:
            pass
        return jsonify({"is_moderator": False}), 200

    # Lookup stored hash without revealing existence
    stored_hash = MOD_HASHES.get(username)
    if not isinstance(stored_hash, str):
        # Username not found: perform dummy verify to equalize timing
        try:
            check_password_hash(_DUMMY_HASH, password)
        except Exception:
            pass
        return jsonify({"is_moderator": False}), 200

    # Verify password using werkzeug's check_password_hash (secure, constant-time)
    try:
        is_valid = check_password_hash(stored_hash, password)
    except Exception:
        # In case of any internal error, do not expose details
        is_valid = False

    return jsonify({"is_moderator": bool(is_valid)}), 200


# Example: guard main so the module can be imported safely without running server
if __name__ == "__main__":
    # Do not enable debug in production to avoid leaking secrets or internals
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
