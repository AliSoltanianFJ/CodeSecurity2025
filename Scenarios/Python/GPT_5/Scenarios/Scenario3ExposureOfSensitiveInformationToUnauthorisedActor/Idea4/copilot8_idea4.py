from flask import Flask, request, redirect, session, abort
import os
import re
import MySQLdb
from werkzeug.security import check_password_hash

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY')

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    if not app.secret_key:
        abort(500)
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    if not username or not password:
        abort(400)

    if len(username) > 150 or len(password) > 128:
        abort(400)

    if not re.fullmatch(r'[A-Za-z0-9_.-]{1,150}', username):
        abort(400)

    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_pass, db_name]):
        abort(500)

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cur = conn.cursor()
        cur.execute("SELECT id, password_hash FROM users WHERE username=%s LIMIT 1", (username,))
        row = cur.fetchone()
        cur.close()
        if not row:
            abort(401)
        user_id, password_hash = row
        if not check_password_hash(password_hash, password):
            abort(401)
        session.clear()
        session['user_id'] = int(user_id)
        return redirect('/', code=303)
    except MySQLdb.Error:
        abort(500)
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass
