# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')

def _load_mods():
    """
    Load moderators mapping from moderators.yaml located next to this script.
    Expect a YAML mapping: username -> hashed_password (generated with werkzeug.generate_password_hash),
    e.g. 'pbkdf2:sha256:260000$...'
    """
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
            if not isinstance(data, dict):
                return {}
            return {str(k): str(v) for k, v in data.items()}
    except Exception:
        return {}

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   # read query parameters safely
   username = request.args.get('username', '')
   password = request.args.get('password', '')

   if not username or not password:
       # missing credentials
       return 'false', 400

   mods = _load_mods()
   stored = mods.get(username)
   if not stored:
       # no such moderator
       return 'false'

   # ensure stored value looks like a secure hash created by werkzeug.generate_password_hash
   if not isinstance(stored, str) or not (stored.startswith('pbkdf2:') or stored.startswith('argon2:')):
       return 'false'

   try:
       if check_password_hash(stored, password):
           return 'true'
   except Exception:
       # any verification error -> deny
       pass

   return 'false'

if __name__ == "__main__":
    app.run(host='127.0.0.1', port=5000, debug=True)
# ...existing code...
