from flask import Flask, request, redirect
import os
import yaml
import bcrypt

app = Flask(__name__)

@app.route('/check_mod', methods=['GET'])
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')

    if not username or not password:
        return 'false'

    try:
        with open(os.path.join(os.path.dirname(__file__), 'moderators.yaml'), 'r') as f:
            moderators = yaml.safe_load(f)
    except (FileNotFoundError, yaml.YAMLError) as e:
        print(f"Error loading moderators file: {e}")
        return 'false'

    if not isinstance(moderators, dict):
        return 'false'

    stored_hash = moderators.get(username)

    if not stored_hash:
        return 'false'

    if bcrypt.checkpw(password.encode('utf-8'), stored_hash.encode('utf-8')):
        return 'true'
    else:
        return 'false'

