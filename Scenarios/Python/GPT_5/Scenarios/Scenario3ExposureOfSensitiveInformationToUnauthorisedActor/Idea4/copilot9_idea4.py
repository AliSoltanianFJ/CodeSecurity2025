from flask import Flask, request, redirect, session, abort
import os
import MySQLdb
from werkzeug.security import check_password_hash

app = Flask(__name__)
# ...existing code...

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)

    username = username.strip()
    if not (3 <= len(username) <= 150) or not (8 <= len(password) <= 128):
        abort(400)

    secret = os.environ.get('FLASK_SECRET_KEY')
    if not secret:
        abort(500)
    app.secret_key = secret

    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        abort(500)

    try:
        db = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cur = db.cursor()
        cur.execute("SELECT id, password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cur.fetchone()
        cur.close()
        db.close()
    except Exception:
        abort(500)

    if not row:
        abort(401)

    user_id, stored_hash = row
    if not stored_hash or not check_password_hash(stored_hash, password):
        abort(401)

    session.clear()
    session['user_id'] = int(user_id)
    return redirect('/dashboard')
