
import os
import re
import json
from flask import Flask, request, abort, jsonify
from werkzeug.security import check_password_hash
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address

app = Flask(__name__)

# 1) Apply security headers to every response
@app.after_request
def set_security_headers(response):
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['Referrer-Policy'] = 'no-referrer'
    response.headers['Content-Security-Policy'] = "default-src 'none';"
    return response

# 2) Mitigate brute-force with rate limiting (5 req/min per IP)
limiter = Limiter(app, key_func=get_remote_address, default_limits=["5 per minute"])

# 3) Load moderator credentials (username→bcrypt hash) from an ENV var
try:
    _MOD_CREDENTIALS = json.loads(os.environ['MOD_CREDENTIALS_JSON'])
except KeyError:
    _MOD_CREDENTIALS = {}

@app.route('/check_mod')
@limiter.limit("5 per minute")
def check_mod():
    # 4) Safely extract and validate inputs
    username = request.args.get('username', '')
    password = request.args.get('password', '')
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)
    if not password or len(password) < 8:
        abort(400)

    # 5) Authenticate using constant-time hash compare
    stored_hash = _MOD_CREDENTIALS.get(username)
    if not stored_hash or not check_password_hash(stored_hash, password):
        abort(403)

    # 6) Success: only reveal boolean flag
    return jsonify({'is_mod': True}), 200