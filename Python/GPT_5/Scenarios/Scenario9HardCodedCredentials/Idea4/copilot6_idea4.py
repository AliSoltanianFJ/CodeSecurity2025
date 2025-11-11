# ...existing code...
from flask import Flask, request, jsonify
import os
import re
import hmac

app = Flask(__name__)

AUTH_TOKEN_RE = re.compile(r'^[0-9a-fA-F]{32,128}$')
_allowed_tokens = [t.strip() for t in os.getenv('MODERATOR_TOKENS', '').split(',') if t and AUTH_TOKEN_RE.fullmatch(t.strip())]

@app.route('/check_mod')
def check_mod():
    auth = request.headers.get('Authorization', '')
    if not auth.startswith('Bearer '):
        return jsonify(error='invalid_authorization'), 400
    token = auth[7:].strip()
    if not AUTH_TOKEN_RE.fullmatch(token):
        return jsonify(error='invalid_token'), 400
    for allowed in _allowed_tokens:
        if hmac.compare_digest(allowed, token):
            return jsonify(True)
    return jsonify(False)
# ...existing code...
