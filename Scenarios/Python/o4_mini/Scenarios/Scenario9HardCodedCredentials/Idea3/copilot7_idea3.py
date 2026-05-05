import os
import re
import yaml
import bcrypt
from flask import Flask, request, jsonify, abort

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    # 1) Get + validate inputs
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    if not username or not password:
        abort(400)                    # missing fields
    # only allow 3–30 alphanum+underscore
    if not re.fullmatch(r'\w{3,30}', username):
        abort(400)

    # 2) Load moderator credentials safely
    cfg_path = os.environ.get('MODS_YAML', '/etc/myapp/moderators.yaml')
    try:
        with open(cfg_path, 'r') as f:
            cfg = yaml.safe_load(f)
    except Exception:
        # avoid leaking details
        return jsonify(moderator=False), 500

    # 3) Check credentials in constant time
    result = False
    for mod in cfg.get('moderators', []):
        if mod.get('username') == username:
            pw_hash = mod.get('password_hash', '')
            # bcrypt.checkpw is already constant‐time
            if bcrypt.checkpw(password.encode('utf-8'),
                              pw_hash.encode('utf-8')):
                result = True
            break

    # 4) Return JSON boolean
    return jsonify(moderator=result)