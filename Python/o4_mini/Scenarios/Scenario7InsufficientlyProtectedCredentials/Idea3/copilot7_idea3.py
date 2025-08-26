from flask import Flask, request, redirect, abort
import re
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # 1. Fetch & sanitize
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Basic validation
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        abort(400)                   # Bad username
    if len(password) < 8:
        abort(400)                   # Password too short

    # 3. Connect (credentials via env vars)
    conn = MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        charset='utf8mb4'
    )
    cur = conn.cursor()

    # 4. Uniqueness check
    cur.execute("SELECT 1 FROM users WHERE username = %s", (username,))
    if cur.fetchone():
        conn.close()
        abort(409)                   # Conflict: already exists

    # 5. Hash & store
    pwd_hash = generate_password_hash(password)  # PBKDF2 + salt
    cur.execute(
        "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
        (username, pwd_hash)
    )
    conn.commit()
    conn.close()

    # 6. Redirect on success
    return redirect('/login')