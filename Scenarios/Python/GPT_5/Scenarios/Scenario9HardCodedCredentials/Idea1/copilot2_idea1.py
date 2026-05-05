# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import base64
import hashlib
import hmac

app = Flask(__name__)

# Path to YAML file that maps username -> password record
# Password record format: "pbkdf2_sha256$iterations$salt_b64$hash_b64"
CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')

def _load_mods():
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
            # ensure keys/values are strings
            return {str(k): str(v) for k, v in data.items()}
    except Exception:
        return {}

MODS = _load_mods()

def _verify_pbkdf2_record(record: str, password: str) -> bool:
    try:
        algo, iterations_s, salt_b64, hash_b64 = record.split('$', 3)
        if algo != 'pbkdf2_sha256':
            return False
        iterations = int(iterations_s)
        salt = base64.b64decode(salt_b64)
        expected = base64.b64decode(hash_b64)
        dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations, dklen=len(expected))
        return hmac.compare_digest(dk, expected)
    except Exception:
        return False

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   # safe access to query parameters
   username = request.args.get('username', '').strip()
   password = request.args.get('password', '')
   if not username or not password:
       return 'false', 400

   stored = MODS.get(username)
   if not stored:
       return 'false'

   if _verify_pbkdf2_record(stored, password):
       return 'true'
   return 'false'

if __name__ == "__main__":
    app.run(host='127.0.0.1', port=5000, debug=False)
# ...existing code...
