# ...existing code...
from flask import Flask, request, redirect, abort
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # get form data safely
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    # basic validation
    if not username or not password:
        abort(400, "username and password are required")

    # username whitelist: 3-30 chars, letters/numbers and _ . -
    if not re.match(r'^[A-Za-z0-9_.-]{3,30}$', username):
        abort(400, "invalid username")

    # password policy: minimum length
    if len(password) < 8:
        abort(400, "password must be at least 8 characters")

    # hash the password using a slow, salted algorithm (PBKDF2)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # DB connection using environment variables (do not hardcode credentials)
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_port = int(os.environ.get('DB_PORT', 3306))
    db_user = os.environ.get('DB_USER', 'appuser')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, port=db_port, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cursor = conn.cursor()

        # check for existing username (use parameterized query)
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            abort(409, "username already exists")

        # insert new user with parameterized query to prevent injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()

    except MySQLdb.Error:
        # Do not leak DB errors to the client
        abort(500, "database error")
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    # registration successful: redirect to login (or return 201)
    return redirect('/login')
# ...existing code...
