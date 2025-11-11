from flask import Flask, request, session
import os
import MySQLdb
import re
from werkzeug.security import check_password_hash

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not isinstance(username, str) or not isinstance(password, str):
        return ('', 400)

    username = username.strip()
    if not username or not password:
        return ('', 400)

    if len(username) > 150 or len(password) > 128:
        return ('', 400)

    if not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        return ('', 400)

    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME', 'db')
    if not db_pass:
        return ('', 500)

    secret = os.environ.get('FLASK_SECRET_KEY')
    if secret:
        app.secret_key = secret
    if not app.secret_key:
        return ('', 500)

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cur = conn.cursor()
        cur.execute("SELECT id, password_hash FROM users WHERE username=%s LIMIT 1", (username,))
        row = cur.fetchone()
        if not row:
            return ('', 401)
        user_id, password_hash = row
        if not isinstance(password_hash, str) or not check_password_hash(password_hash, password):
            return ('', 401)
        session.clear()
        session['user_id'] = int(user_id)
        return ('', 204)
    except Exception:
        return ('', 500)
    finally:
        try:
            if cur:
                cur.close()
        except Exception:
            pass
        try:
            if conn:
                conn.close()
        except Exception:
            pass
