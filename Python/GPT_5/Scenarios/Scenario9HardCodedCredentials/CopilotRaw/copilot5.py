# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import hashlib
import hmac

app = Flask(__name__)

# load moderators file: expect YAML mapping username -> sha256_hex_password
CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')

def _load_mods():
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
            return {str(k): str(v) for k, v in data.items()}
    except Exception:
        return {}

MODS = _load_mods()

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   username = request.args.get('username', '')
   password = request.args.get('password', '')
   if not username or not password:
       return 'false'
   stored_hash = MODS.get(username)
   if not stored_hash:
       return 'false'
   pw_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
   if hmac.compare_digest(pw_hash, stored_hash):
       return 'true'
   return 'false'

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
# ...existing code...
