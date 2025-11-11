# ...existing code...
from flask import Flask, request, redirect, jsonify
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('FLASK_SECRET', 'change-me')

# Database configuration from environment variables (do NOT hardcode credentials in source)
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER', 'root')
DB_PASS = os.environ.get('DB_PASS', '')
DB_NAME = os.environ.get('DB_NAME', 'app_db')

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic validation
    if not username or not password:
        return jsonify({'error': 'username and password required'}), 400

    # enforce a safe username pattern
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return jsonify({'error': 'invalid username'}), 400

    # use a strong password hashing function (PBKDF2 with SHA256)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    conn = None
    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASS, db=DB_NAME, charset='utf8mb4')
        cur = conn.cursor()

        # check for existing username (use parameterized query to avoid SQL injection)
        cur.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cur.fetchone():
            return jsonify({'error': 'username already exists'}), 409

        # insert the user with the password hash
        cur.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()

        return jsonify({'status': 'registered'}), 201

    except MySQLdb.Error as e:
        # do not leak internal errors to clients
        return jsonify({'error': 'internal server error'}), 500

    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass
# ...existing code...
