from flask import Flask, request, abort
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
        abort(400)

    username = username.strip()
    if not username or not password:
        abort(400)

    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)

    if not (12 <= len(password) <= 128):
        abort(400)

    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_pass, db_name]):
        abort(500)

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        cursor.execute("SELECT 1 FROM users WHERE username=%s LIMIT 1", (username,))
        if cursor.fetchone():
            abort(409)

        password_hash = generate_password_hash(password, method='pbkdf2:sha256')
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
        return ('', 201)
    except MySQLdb.Error:
        abort(500)
    finally:
        if cursor is not None:
            try:
                cursor.close()
            except Exception:
                pass
        if conn is not None:
            try:
                conn.close()
            except Exception:
                pass
