# ...existing code...
from flask import Flask, request, jsonify, abort
import os
import re
import hmac
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

# Load moderator credentials (passwords must be stored as secure hashes)
MODS = {}

def load_moderators():
    """
    Load moderators from a YAML file using safe_load.
    Expected YAML structure (preferred):
      moderators:
        username1: "pbkdf2:sha256:150000$...hash..."
        username2: "pbkdf2:sha256:150000$...hash..."
    Or:
      moderators:
        - username: username1
          password_hash: "pbkdf2:sha256:150000$...hash..."
    The config file must live inside the application directory to avoid arbitrary file reads.
    Path can be overridden by the MODS_CONFIG_PATH env var but only if it resides under the app dir.
    """
    global MODS
    base_dir = os.path.abspath(os.path.dirname(__file__))
    default_path = os.path.join(base_dir, 'config', 'mods.yaml')
    config_path = os.environ.get('MODS_CONFIG_PATH', default_path)
    try:
        config_path = os.path.abspath(config_path)
        # Restrict config file to be inside the application directory
        if os.path.commonpath([config_path, base_dir]) != base_dir:
            # ignore unsafe path
            return
        if not os.path.exists(config_path):
            return
        with open(config_path, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f)
        if not isinstance(data, dict):
            return
        moderators = data.get('moderators')
        if isinstance(moderators, dict):
            for u, h in moderators.items():
                if isinstance(u, str) and isinstance(h, str):
                    MODS[u] = h
        elif isinstance(moderators, list):
            for item in moderators:
                if isinstance(item, dict):
                    u = item.get('username')
                    h = item.get('password_hash')
                    if isinstance(u, str) and isinstance(h, str):
                        MODS[u] = h
    except Exception:
        # Fail safe: do not expose internal errors or file contents
        return

# Load moderators at startup (silent failure if not present)
load_moderators()

# Helper: safe length and character checks for username
def _validate_username(username: str) -> bool:
    if not isinstance(username, str):
        return False
    username = username.strip()
    if not username or len(username) > 150:
        return False
    # Restrict allowed characters to a safe subset (alphanum and common safe symbols)
    if not re.fullmatch(r'[A-Za-z0-9._-]{1,150}', username):
        return False
    return True

# Helper: safe length check for password only (do NOT log passwords)
def _validate_password(password: str) -> bool:
    if not isinstance(password, str):
        return False
    if len(password) == 0 or len(password) > 1024:
        return False
    # no further validation to avoid rejecting valid complex passwords
    return True

@app.route('/check_mod', methods=['GET'])
def check_mod():
    """
    Returns JSON {"is_moderator": true/false}.
    Accepts username and password as query params.
    Security considerations implemented:
      - Input validation and length limits
      - No reflection of user input (prevents XSS)
      - Uses safe YAML loading (yaml.safe_load) to prevent insecure deserialization
      - Password verification uses werkzeug's check_password_hash (PBKDF2)
      - Config file access restricted to application directory (prevent arbitrary file reads)
      - No debug or stack traces returned to clients
    """
    # Retrieve parameters safely
    username = request.args.get('username', default=None, type=str)
    password = request.args.get('password', default=None, type=str)

    # Validate inputs
    if not _validate_username(username) or not _validate_password(password):
        # Generic error message to avoid leaking what was wrong
        return jsonify({'error': 'Invalid request parameters'}), 400

    # Lookup stored hash (do not reveal whether the user exists)
    stored_hash = MODS.get(username)
    if not stored_hash:
        # Return false rather than 401 to avoid user enumeration via timing differences.
        # To further mitigate timing attacks, perform a dummy hash check.
        try:
            # Use constant time comparison against a fixed fake hash to reduce timing side-channels
            fake_hash = 'pbkdf2:sha256:150000$invalid$invalidhash'
            _ = check_password_hash(fake_hash, password)
        except Exception:
            pass
        return jsonify({'is_moderator': False}), 200

    # Verify password using secure hash check
    try:
        is_mod = bool(check_password_hash(stored_hash, password))
    except Exception:
        # On any internal error, return false without revealing details
        is_mod = False

    return jsonify({'is_moderator': is_mod}), 200

# ...existing code...
if __name__ == '__main__':
    # Do not enable debug in production. Bind to localhost by default.
    app.run(host='127.0.0.1', port=int(os.environ.get('PORT', '5000')), debug=False)
# ...existing code...
