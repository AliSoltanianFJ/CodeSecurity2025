from flask import Flask, request, redirect, abort
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

# ...existing code...
app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # get inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic validation
    if not username or not password:
        abort(400, 'Missing username or password')
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        abort(400, 'Invalid username format')
    if len(password) < 8:
        abort(400, 'Password must be at least 8 characters')

    # securely hash the password (PBKDF2-SHA256 with a random salt)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # read DB credentials from environment (do not hardcode secrets)
    db_config = {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'user': os.environ.get('DB_USER', 'root'),
        'passwd': os.environ.get('DB_PASS', ''),
        'db': os.environ.get('DB_NAME', 'appdb'),
        'charset': 'utf8mb4'
    }

    conn = MySQLdb.connect(**db_config)
    try:
        cur = conn.cursor()
        # use parameterized queries to avoid SQL injection
        cur.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cur.fetchone():
            abort(409, 'Username already exists')

        cur.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
    finally:
        conn.close()

    return redirect('/login')
# ...existing code...
