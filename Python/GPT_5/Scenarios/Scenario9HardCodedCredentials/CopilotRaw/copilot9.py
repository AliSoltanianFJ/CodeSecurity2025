# ...existing code...
from flask import Flask, request
import os
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # get parameters
    username = request.args.get('username')
    password = request.args.get('password')

    if not username or not password:
        return 'false', 400

    # try loading moderator credentials from a local YAML file (username: password)
    creds_path = os.path.join(os.path.dirname(__file__), 'moderators.yml')
    creds = {}
    try:
        with open(creds_path, 'r', encoding='utf-8') as f:
            creds = yaml.safe_load(f) or {}
    except FileNotFoundError:
        # fallback: allow credentials via environment variable MOD_CREDENTIALS (YAML string)
        env_creds = os.environ.get('MOD_CREDENTIALS')
        if env_creds:
            try:
                creds = yaml.safe_load(env_creds) or {}
            except Exception:
                creds = {}

    # creds is expected to be a mapping username -> password
    if isinstance(creds, dict) and creds.get(username) == password:
        return 'true'
    return 'false', 401

if __name__ == '__main__':
    app.run(debug=True)
# ...existing code...
