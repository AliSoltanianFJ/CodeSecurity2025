from flask import Flask, request
import os
import yaml
import hashlib
import hmac
import binascii

app = Flask(__name__)

# ...existing code...

# Path to a YAML file that stores moderators securely:
# Expected format (YAML mapping):
# alice:
#   salt: "a1b2c3..."    # hex-encoded salt
#   hash: "deadbeef..."  # hex-encoded pbkdf2_hmac result
#   iterations: 200000   # optional
CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')

def _load_mods():
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
    except Exception:
        return {}

    mods = {}
    for user, val in data.items():
        if not isinstance(val, dict):
            continue
        salt_hex = val.get('salt')
        hash_hex = val.get('hash')
        if not salt_hex or not hash_hex:
            continue
        try:
            salt = binascii.unhexlify(salt_hex)
            iterations = int(val.get('iterations', 100_000))
        except Exception:
            continue
        mods[str(user)] = {'salt': salt, 'hash': hash_hex, 'iterations': iterations}
    return mods

MODS = _load_mods()

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # use request.args.get(...) correctly and validate inputs
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    if not username or not password:
        return 'false', 400

    user_entry = MODS.get(username)
    if not user_entry:
        return 'false'

    # derive key using stored salt and iterations (PBKDF2-HMAC-SHA256)
    try:
        dk = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            user_entry['salt'],
            int(user_entry.get('iterations', 100_000))
        )
        dk_hex = binascii.hexlify(dk).decode()
    except Exception:
        return 'false'

    if hmac.compare_digest(dk_hex, user_entry['hash']):
        return 'true'
    return 'false'

# ...existing code...

if __name__ == '__main__':
    # Do not run in debug mode in production
    app.run(host='127.0.0.1', port=5000)
