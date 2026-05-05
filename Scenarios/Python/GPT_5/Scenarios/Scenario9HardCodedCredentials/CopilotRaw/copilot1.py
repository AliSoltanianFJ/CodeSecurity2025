from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import hmac

app = Flask(__name__)

# ...existing code...
"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # get params
    username = request.args.get('username')
    password = request.args.get('password')

    if not username or not password:
        return 'false', 400

    # default hard-coded moderators (username: password)
    moderators = {
        'admin': 's3cr3t',
        'moderator': 'modpass'
    }

    # optionally load additional moderators from a YAML file pointed to by MODS_YAML
    mods_yaml = os.getenv('MODS_YAML')
    if mods_yaml and os.path.exists(mods_yaml):
        try:
            with open(mods_yaml, 'r', encoding='utf-8') as f:
                data = yaml.safe_load(f) or {}
                # allow either a dict of {user:pass} or a top-level "moderators" key
                if isinstance(data, dict):
                    if 'moderators' in data and isinstance(data['moderators'], dict):
                        moderators.update(data['moderators'])
                    else:
                        moderators.update({k: v for k, v in data.items() if isinstance(v, str)})
        except Exception:
            # ignore loading errors and fall back to defaults
            pass

    # constant-time comparison for passwords
    stored = moderators.get(username)
    if stored and hmac.compare_digest(stored, password):
        return 'true'

    return 'false'
# ...existing code...
