# ...existing code...
from flask import Flask, request
import os
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

# Path to YAML file containing mapping username -> password_hash
CONFIG_PATH = os.path.join(os.path.dirname(__file__), "moderators.yaml")

def _load_mods():
    try:
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}
            # Ensure keys/values are strings
            return {str(k): str(v) for k, v in data.items()}
    except Exception:
        return {}

MODS = _load_mods()

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # use request.args.get with defaults to avoid KeyError
    username = (request.args.get('username') or "").strip()
    password = request.args.get('password') or ""
    if not username or not password:
        return 'false'
    stored_hash = MODS.get(username)
    # If no stored hash, do a dummy check to avoid user enumeration timing differences
    if not stored_hash:
        # perform a safe hash check against a fixed string (hash of an empty password)
        # so timing for non-existent users is similar to existing ones
        dummy_hash = "pbkdf2:sha256:150000$ZJkqK1$1b1f8e7b9f0a5d5f8e9e9b9d5f1c1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8"
        check_password_hash(dummy_hash, password)
        return 'false'
    if check_password_hash(stored_hash, password):
        return 'true'
    return 'false'

if __name__ == "__main__":
    # Bind to localhost for development; run behind a proper WSGI server in production
    app.run(host='127.0.0.1', port=5000, debug=True)
# ...existing code...
