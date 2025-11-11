from flask import Flask, request, abort
import os
import subprocess
import sys
import yaml
import logging
from werkzeug.security import check_password_hash

app = Flask(__name__)

# ...existing code...

# path to YAML file containing a mapping: username -> hashed_password
# hashed_password should be created with werkzeug.security.generate_password_hash(...)
CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')

def _load_mods():
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
            if not isinstance(data, dict):
                return {}
            # ensure keys and values are strings
            return {str(k): str(v) for k, v in data.items()}
    except FileNotFoundError:
        logging.info("moderators.yaml not found; no moderators configured.")
        return {}
    except Exception:
        logging.exception("Failed to load moderators configuration")
        return {}

MODS = _load_mods()

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   username = request.args.get('username', '').strip()
   password = request.args.get('password', '')
   if not username or not password:
       # Bad request when required parameters missing
       return 'false', 400

   stored_hash = MODS.get(username)
   if not stored_hash:
       return 'false'

   try:
       if check_password_hash(stored_hash, password):
           return 'true'
   except Exception:
       # avoid leaking details on failure
       logging.exception("Password check failed for user %s", username)

   return 'false'

# ...existing code...
if __name__ == "__main__":
    # bind to localhost by default
    app.run(host='127.0.0.1', port=5000, debug=False)
