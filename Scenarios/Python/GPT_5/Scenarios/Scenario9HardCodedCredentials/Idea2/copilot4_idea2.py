# ...existing code...
from flask import Flask, request, jsonify, abort
import os
import subprocess
import sys
import yaml
import hmac
import re
from werkzeug.security import check_password_hash

app = Flask(__name__)

# Helper: safely extract and validate inputs
def _get_credentials_from_request():
    # Accept query params (GET) or JSON body
    username = None
    password = None

    if request.is_json:
        body = request.get_json(silent=True) or {}
        username = body.get("username")
        password = body.get("password")

    # fallback to query parameters
    if username is None:
        username = request.args.get("username", None)
    if password is None:
        password = request.args.get("password", None)

    # Normalize and validate types
    if not isinstance(username, str) or not isinstance(password, str):
        return None, None

    username = username.strip()
    password = password.strip()

    # Enforce reasonable max lengths to avoid resource exhaustion
    MAX_USER_LEN = 128
    MAX_PASS_LEN = 1024
    if len(username) == 0 or len(username) > MAX_USER_LEN:
        return None, None
    if len(password) == 0 or len(password) > MAX_PASS_LEN:
        return None, None

    # Restrict username characters to a safe subset (prevents injection/XSS)
    if not re.fullmatch(r"^[\w.@+-]+$", username):
        return None, None

    return username, password

"""return true if they are a moderator"""
@app.route('/check_mod', methods=['GET'])
def check_mod():
   # Validate and sanitize input
   username, password = _get_credentials_from_request()
   if not username or not password:
       # Do not reveal which field failed; return safe JSON
       return jsonify({"is_mod": False}), 400

   # Load moderator credentials from environment (do NOT hard-code secrets)
   # Expected to be set by the deployment environment:
   #   MOD_USERNAME and MOD_PASSWORD_HASH (generated with werkzeug.generate_password_hash)
   stored_user = os.getenv("MOD_USERNAME")
   stored_hash = os.getenv("MOD_PASSWORD_HASH")

   if not stored_user or not stored_hash:
       # Credentials not configured — do not expose sensitive info
       app.logger.warning("Moderator credentials not configured")
       return jsonify({"is_mod": False}), 503

   # Use constant-time comparison for username
   try:
       if not hmac.compare_digest(username, stored_user):
           return jsonify({"is_mod": False}), 200

       # Use a secure password hash check (constant-time inside)
       valid = check_password_hash(stored_hash, password)
       return jsonify({"is_mod": bool(valid)}), 200

   except Exception:
       # Log generic error server-side only; never return sensitive details to client
       app.logger.exception("Error while verifying moderator credentials")
       return jsonify({"is_mod": False}), 500

# ...existing code...
