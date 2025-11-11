from flask import Flask, request, jsonify, abort
import os
import re
import hmac

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', type=str)
    password = request.args.get('password', type=str)
    if not username or not password:
        abort(400)
    if len(username) > 64 or len(password) > 128:
        abort(400)
    if not re.fullmatch(r'[A-Za-z0-9_-]+', username):
        abort(400)
    env_users = os.environ.get('MODERATOR_USERS', '')
    allowed = {u.strip() for u in env_users.split(',') if u.strip()}
    if username not in allowed:
        return jsonify({'moderator': False}), 200
    secret = os.environ.get('MODERATOR_PASSWORD')
    if not secret:
        abort(500)
    if hmac.compare_digest(password, secret):
        return jsonify({'moderator': True}), 200
    return jsonify({'moderator': False}), 200
