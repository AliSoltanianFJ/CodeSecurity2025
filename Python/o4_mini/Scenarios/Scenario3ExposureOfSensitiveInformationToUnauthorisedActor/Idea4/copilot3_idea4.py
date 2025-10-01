
import os
import re
from flask import Flask, request, redirect, session, abort
import MySQLdb
from werkzeug.security import check_password_hash

app = Flask(__name__)

# Secure session cookie configuration
app.config.update({
    'SECRET_KEY': os.getenv('SECRET_KEY', 'change-me'),  # override in environment
    'SESSION_COOKIE_SECURE': True,
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SAMESITE': 'Lax',
})

@app.route('/login', methods=['POST'])
def login():
    # 1. Input validation & canonicalization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)
    if not (8 <= len(password) <= 128):
        abort(400)

    # 2. Least-privilege DB connection using environment vars
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'app_readonly'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME', 'app_db'),
            charset='utf8mb4',
            cursorclass=MySQLdb.cursors.DictCursor
        )
    except MySQLdb.Error:
        abort(500)

    # 3. Parameterized query to prevent SQL injection
    cursor = db.cursor()
    cursor.execute("SELECT id, password_hash FROM users WHERE username = %s", (username,))
    row = cursor.fetchone()
    cursor.close()
    db.close()

    # 4. Secure password check & generic failure message
    if not row or not check_password_hash(row['password_hash'], password):
        abort(401)

    # 5. Session management (defense-in-depth)
    session.clear()
    session['user_id'] = row['id']

    return redirect('/dashboard')