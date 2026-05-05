# ...existing code...
from flask import Flask, request
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    if not username or not password:
        return 'false', 400

    # try loading moderators from a YAML file next to this script
    mods_file = os.path.join(os.path.dirname(__file__), 'moderators.yaml')
    moderators = []
    if os.path.exists(mods_file):
        try:
            with open(mods_file, 'r', encoding='utf-8') as f:
                data = yaml.safe_load(f)
                if isinstance(data, list):
                    moderators = data
                elif isinstance(data, dict):
                    # allow dict mapping username -> password
                    moderators = [{'username': k, 'password': v} for k, v in data.items()]
        except Exception:
            moderators = []
    return 'false'

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000, debug=True)
