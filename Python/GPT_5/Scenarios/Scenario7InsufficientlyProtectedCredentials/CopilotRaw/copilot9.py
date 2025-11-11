# ...existing code...
from flask import Flask, request, redirect, abort
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic validation
    if not username or not password:
        abort(400, 'username and password required')

    if not re.match(r'^[A-Za-z0-9_.-]{3,30}$', username):
        abort(400, 'invalid username')

    if len(password) < 12:
        abort(400, 'password must be at least 12 characters')

    # hash the password securely (PBKDF2 with SHA256 via Werkzeug)
    password_hash = generate_password_hash(password)

    # read DB credentials from environment variables (do NOT hardcode secrets)
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'appuser')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cursor = conn.cursor()

        # check if username exists (parameterized query to avoid SQL injection)
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            cursor.close()
            conn.close()
            abort(409, 'username already taken')

        # insert new user with hashed password (parameterized)
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
        cursor.close()
        conn.close()
    except MySQLdb.Error:
        abort(500, 'database error')

    return redirect('/login')
# ...existing code...
