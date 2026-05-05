# ...existing code...
from flask import Flask, request, redirect, session, abort
import os
import re
from werkzeug.security import check_password_hash
import MySQLdb

app = Flask(__name__)
if os.environ.get("FLASK_SECRET_KEY"):
    app.secret_key = os.environ["FLASK_SECRET_KEY"]
# ...existing code...
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)

    username = username.strip()
    if not (3 <= len(username) <= 50) or not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        abort(400)
    if not (8 <= len(password) <= 128):
        abort(400)

    db_host = os.environ.get('DB_HOST', 'localhost')
    db_name = os.environ.get('DB_NAME', 'db')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    if not db_user or not db_pass:
        abort(500)

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name)
        cursor = conn.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute("SELECT id, password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cursor.fetchone()
        cursor.close()

        if not row or not row.get('password_hash'):
            abort(401)

        if not check_password_hash(row['password_hash'], password):
            abort(401)

        session.clear()
        session['user_id'] = int(row['id'])
        return redirect('/dashboard')
    except MySQLdb.Error:
        abort(500)
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass
# ...existing code...
