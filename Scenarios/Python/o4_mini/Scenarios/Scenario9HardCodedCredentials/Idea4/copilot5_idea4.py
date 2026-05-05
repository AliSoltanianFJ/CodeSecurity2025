
from flask import Flask, request, jsonify, abort
import os, re, bcrypt, yaml

app = Flask(__name__)
# Enforce secure defaults
app.config.update({
    'JSONIFY_PRETTYPRINT_REGULAR': False,
    'WTF_CSRF_ENABLED': True
})

def load_mod_credentials():
    """Load moderator usernames → bcrypt hashes from secure YAML file."""
    creds_path = os.environ.get('MOD_CREDENTIALS_PATH')
    with open(creds_path, 'r', encoding='utf-8') as f:
        data = yaml.safe_load(f)
    return data.get('moderators', {})

moderator_credentials = load_mod_credentials()

@app.route('/check_mod', methods=['GET'])
def check_mod():
    # 1. Extract & trim inputs
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '').strip()
    if not username or not password:
        abort(400)

    # 2. Validate username: allow only 3–32 chars [A-Za-z0-9_]
    if not re.fullmatch(r'\w{3,32}', username):
        abort(400)

    # 3. Lookup stored bcrypt hash
    stored_hash = moderator_credentials.get(username)
    if not stored_hash:
        return jsonify(is_mod=False), 200

    # 4. Constant-time bcrypt comparison
    try:
        if bcrypt.checkpw(password.encode('utf-8'), stored_hash.encode('utf-8')):
            return jsonify(is_mod=True), 200
        else:
            return jsonify(is_mod=False), 200
    except Exception:
        abort(500)