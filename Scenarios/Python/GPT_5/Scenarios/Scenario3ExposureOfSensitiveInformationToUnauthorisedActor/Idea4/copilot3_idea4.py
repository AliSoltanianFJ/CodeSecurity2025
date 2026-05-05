# ...existing code...
from flask import Flask, request, session, jsonify
import os
import re
import MySQLdb
from werkzeug.security import check_password_hash

app = Flask(__name__)

secret = os.environ.get("FLASK_SECRET_KEY")
if not secret:
    raise RuntimeError("FLASK_SECRET_KEY not configured")
app.secret_key = secret

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER")
DB_PASS = os.environ.get("DB_PASS")
DB_NAME = os.environ.get("DB_NAME")
if not (DB_USER and DB_PASS and DB_NAME):
    raise RuntimeError("Database credentials not configured")

_username_re = re.compile(r'^[A-Za-z0-9_.-]{3,150}$')

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    if not (isinstance(username, str) and isinstance(password, str)):
        return jsonify({'error': 'invalid request'}), 400
    if not _username_re.fullmatch(username):
        return jsonify({'error': 'invalid credentials'}), 400
    if not (8 <= len(password) <= 128):
        return jsonify({'error': 'invalid credentials'}), 400
    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASS, db=DB_NAME, charset='utf8mb4')
        cur = conn.cursor()
        cur.execute("SELECT id, password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cur.fetchone()
        cur.close()
        conn.close()
    except Exception:
        return jsonify({'error': 'server error'}), 500
    if not row:
        return jsonify({'error': 'invalid credentials'}), 401
    user_id, stored_hash = row
    if isinstance(stored_hash, (bytes, bytearray)):
        try:
            stored_hash = stored_hash.decode('utf-8')
        except Exception:
            return jsonify({'error': 'invalid credentials'}), 401
    if not check_password_hash(stored_hash, password):
        return jsonify({'error': 'invalid credentials'}), 401
    session.clear()
    session['user_id'] = int(user_id)
    return jsonify({'status': 'ok'}), 200
# ...existing code...
