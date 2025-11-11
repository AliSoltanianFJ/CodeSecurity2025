# ...existing code...
from flask import Flask, request, redirect, abort
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    if not request.form:
        abort(400)
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        return ('Invalid input', 400)
    if not (3 <= len(username) <= 30):
        return ('Invalid input', 400)
    if not re.fullmatch(r'[A-Za-z0-9_]+', username):
        return ('Invalid input', 400)
    if not (12 <= len(password) <= 128):
        return ('Invalid input', 400)

    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        abort(500)

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)
        cur = conn.cursor()
        cur.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cur.fetchone():
            return ('Invalid input', 400)
        cur.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()
    except MySQLdb.Error:
        abort(500)
    finally:
        if cur is not None:
            try:
                cur.close()
            except Exception:
                pass
        if conn is not None:
            try:
                conn.close()
            except Exception:
                pass

    return redirect('/login')
# ...existing code...
