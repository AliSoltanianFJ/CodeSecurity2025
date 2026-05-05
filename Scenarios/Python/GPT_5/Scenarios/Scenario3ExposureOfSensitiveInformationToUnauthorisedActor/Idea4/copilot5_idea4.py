from flask import Flask, request, redirect, abort, make_response
import os
import re
import MySQLdb
from werkzeug.security import check_password_hash
app = Flask(__name__)
# ...existing code...
"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)

    username = username.strip()
    if not (3 <= len(username) <= 150) or not (8 <= len(password) <= 128):
        abort(400)

    if not re.match(r'^[A-Za-z0-9_]+$', username):
        abort(400)

    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_user, db_pass, db_name]):
        abort(500)

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cursor = conn.cursor()
        cursor.execute("SELECT id, password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cursor.fetchone()
        if not row:
            abort(401)
        _, password_hash = row
        if not isinstance(password_hash, str) or not check_password_hash(password_hash, password):
            abort(401)
        return make_response('', 200)
    except Exception:
        abort(500)
    finally:
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass
# ...existing code...
