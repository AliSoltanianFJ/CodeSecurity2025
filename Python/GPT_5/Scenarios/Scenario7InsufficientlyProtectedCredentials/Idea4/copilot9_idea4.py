from flask import Flask, request, jsonify
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not isinstance(username, str) or not isinstance(password, str):
        return jsonify(error="invalid input"), 400

    username = username.strip()
    if not username or not password:
        return jsonify(error="missing required parameters"), 400

    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return jsonify(error="invalid username"), 400

    if not (12 <= len(password) <= 128):
        return jsonify(error="invalid password"), 400

    db_host = os.getenv('DB_HOST', 'localhost')
    db_user = os.getenv('DB_USER')
    db_pass = os.getenv('DB_PASS')
    db_name = os.getenv('DB_NAME')
    db_port = int(os.getenv('DB_PORT', '3306'))

    if not all([db_user, db_pass, db_name]):
        return jsonify(error="server misconfiguration"), 500

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, port=db_port, charset='utf8mb4')
        cur = conn.cursor()
        cur.execute("SELECT 1 FROM users WHERE username = %s LIMIT 1", (username,))
        if cur.fetchone():
            return jsonify(error="username taken"), 409

        password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
        cur.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()
        return jsonify(status="created"), 201
    except MySQLdb.Error:
        return jsonify(error="server error"), 500
    finally:
        if conn:
            conn.close()
