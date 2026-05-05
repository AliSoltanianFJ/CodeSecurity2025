from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

import hmac

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')

    # Load moderator credentials from external YAML file
    cred_file = os.getenv(
        'MOD_CREDENTIALS_FILE',
        os.path.join(os.path.dirname(__file__), 'moderators.yaml')
    )
    try:
        with open(cred_file) as f:
            data = yaml.safe_load(f)
            moderators = data.get('moderators', {})
    except Exception:
        return 'false'

    # Safely compare credentials
    stored_pass = moderators.get(username)
    if stored_pass and hmac.compare_digest(stored_pass, password):
        return 'true'
    return 'false'