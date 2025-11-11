# ...existing code...
from flask import Flask, request
import os
import yaml
import hashlib
import hmac
import binascii

app = Flask(__name__)

# Configuration: expect a YAML file next to this script named "moderators.yaml".
# Schema (per-user): 
# alice:
#   salt: "a1b2c3..."      # hex
#   iterations: 150000
#   hash: "deadbeef..."    # hex of pbkdf2_hmac('sha256', password, salt, iterations)
CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')

def _load_mods():
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
            if not isinstance(data, dict):
                return {}
            # normalize entries to expected dict form
            out = {}
            for user, v in data.items():
                if isinstance(v, dict) and 'salt' in v and 'hash' in v:
                    try:
                        salt = binascii.unhexlify(v['salt'])
                        stored_hash = v['hash']
                        iterations = int(v.get('iterations', 150000))
                        out[str(user)] = {'salt': salt, 'hash': stored_hash, 'iterations': iterations}
                    except Exception:
                        # skip invalid entries
                        continue
            return out
    except FileNotFoundError:
        return {}
    except Exception:
        return {}

MODS = _load_mods()

def _verify_password(stored_entry, password):
    if not stored_entry or not password:
        return False
    try:
        salt = stored_entry['salt']
        iterations = int(stored_entry.get('iterations', 150000))
        expected_hex = stored_entry['hash']
        dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
        return hmac.compare_digest(binascii.hexlify(dk).decode('ascii'), expected_hex)
    except Exception:
        return False

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # use safe .get(...) calls
    username = request.args.get('username', '')
    password = request.args.get('password', '')
    if not username or not password:
        return 'false', 400

    entry = MODS.get(username)
    if _verify_password(entry, password):
        return 'true'
    return 'false'

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000, debug=True)
# ...existing code...
