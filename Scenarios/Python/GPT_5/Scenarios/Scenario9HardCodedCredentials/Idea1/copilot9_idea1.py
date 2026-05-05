from flask import Flask, request
import os
import yaml
import logging
from werkzeug.security import check_password_hash

app = Flask(__name__)

# ...existing code...

# Path to a YAML file containing a mapping username -> password_hash (Werkzeug/Flask generate_password_hash format)
CONFIG_PATH = os.path.join(os.path.dirname(__file__), "moderators.yaml")

def _load_mods():
    try:
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}
            if isinstance(data, dict):
                # ensure keys/values are strings
                return {str(k): str(v) for k, v in data.items()}
    except FileNotFoundError:
        logging.info("Moderators config not found at %s", CONFIG_PATH)
    except Exception:
        logging.exception("Failed to load moderators config")
    return {}

MODS = _load_mods()

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # use request.args.get correctly and default to empty string
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    if not username or not password:
        return 'false', 400

    stored_hash = MODS.get(username)
    if not stored_hash:
        # user not found
        return 'false', 403

    try:
        # use Werkzeug's check_password_hash (PBKDF2) to validate password safely
        if check_password_hash(stored_hash, password):
            return 'true'
        else:
            return 'false', 403
    except Exception:
        logging.exception("Error verifying password for user %s", username)
        return 'false', 500

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=True)
