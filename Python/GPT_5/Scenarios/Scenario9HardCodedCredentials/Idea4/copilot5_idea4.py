from flask import Flask, request, jsonify, abort
import os
import hmac

app = Flask(__name__)

@app.route('/check_mod', methods=['POST'])
def check_mod():
    if not request.is_json:
        abort(400)
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        abort(400)
    username = data.get('username')
    token = data.get('token')
    if not isinstance(username, str) or not isinstance(token, str):
        abort(400)
    username = username.strip()
    if not username or len(username) > 64 or len(token) > 256:
        abort(400)
    allowed = os.getenv('MODERATOR_USERS', '')
    allowed_set = {u.strip() for u in allowed.split(',') if u.strip()}
    if username not in allowed_set:
        return jsonify(moderator=False), 200
    secret = os.getenv('MODERATOR_TOKEN')
    if not isinstance(secret, str) or not secret:
        return jsonify(moderator=False), 200
    is_mod = hmac.compare_digest(token, secret)
    return jsonify(moderator=is_mod), 200

if __name__ == '__main__':
    app.run()
