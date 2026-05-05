# ...existing code...
from flask import Flask, request
import os
import subprocess
import sys
import yaml
import hashlib
import hmac
import binascii

app = Flask(__name__)

# path to a YAML file containing moderator credentials.
# The file should be a mapping: username -> {salt: hex, hash: hex, iterations: int}
# Example YAML:
# alice:
#   salt: "a1b2c3..."
#   hash: "d4e5f6..."
#   iterations: 150000
CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'moderators.yaml')


def _load_mods():
    try:
        if not os.path.exists(CONFIG_PATH):
            return {}
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}
        mods = {}
        for user, entry in data.items():
            if not isinstance(user, str):
                continue
            if isinstance(entry, dict):
                salt = entry.get('salt')
                hash_hex = entry.get('hash')
                iterations = int(entry.get('iterations', 150000))
                if isinstance(salt, str) and isinstance(hash_hex, str):
                    mods[user] = {
                        'salt': salt,
                        'hash': hash_hex,
                        'iterations': iterations
                    }
        return mods
    except Exception:
        # On any problem, return empty set rather than exposing details
        return {}


MODS = _load_mods()


def _verify_password(password: str, salt_hex: str, stored_hash_hex: str, iterations: int) -> bool:
    try:
        salt = binascii.unhexlify(salt_hex)
    except (binascii.Error, TypeError):
        return False
    derived = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
    derived_hex = binascii.hexlify(derived).decode('ascii')
    return hmac.compare_digest(derived_hex, stored_hash_hex)


"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # safe access to query parameters
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    if not username or not password:
        return 'false', 400

    entry = MODS.get(username)
    if not entry:
        return 'false', 403

    salt = entry.get('salt')
    stored_hash = entry.get('hash')
    iterations = int(entry.get('iterations', 150000))

    if not salt or not stored_hash:
        return 'false', 403

    if _verify_password(password, salt, stored_hash, iterations):
        return 'true'
    return 'false'


if __name__ == "__main__":
    # Do not enable debug in production
    app.run(host='127.0.0.1', port=5000, debug=False)
# ...existing code...
