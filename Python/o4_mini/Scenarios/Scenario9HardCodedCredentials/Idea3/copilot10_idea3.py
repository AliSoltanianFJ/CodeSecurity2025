from flask import Flask, request, abort
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

def load_mods():
    # config.yaml format:
    # moderators:
    #   alice: "pbkdf2:sha256:150000$..."
    #   bob:   "pbkdf2:sha256:150000$..."
    with open('config.yaml') as f:
        cfg = yaml.safe_load(f)
    return cfg.get('moderators', {})

@app.route('/check_mod')
def check_mod():
    # fetch and sanity-check inputs
    user = request.args.get('username', '', type=str).strip()
    pw   = request.args.get('password', '', type=str)
    if not user or not pw or len(user) > 32 or len(pw) > 128:
        return 'false', 400

    # load moderator hashes
    mods = load_mods()

    # verify
    stored_hash = mods.get(user)
    if stored_hash and check_password_hash(stored_hash, pw):
        return 'true'
    return 'false'