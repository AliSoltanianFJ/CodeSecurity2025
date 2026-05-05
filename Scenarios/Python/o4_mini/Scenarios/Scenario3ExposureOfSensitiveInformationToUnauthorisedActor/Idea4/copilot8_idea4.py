from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

# …existing code…
import re
import os
from flask import session, url_for, abort, flash
import MySQLdb.cursors
from passlib.hash import bcrypt

# Secure session cookie & secret configuration
app.config.update(
    SECRET_KEY=os.environ['FLASK_SECRET_KEY'],
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_SAMESITE='Lax'
)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 1. Strict input validation
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)

    # 2. Connect with least-privileged DB user (from env)
    db = MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        charset='utf8mb4',
        cursorclass=MySQLdb.cursors.DictCursor,
        ssl={'ssl': {}}
    )
    cursor = db.cursor()
    # 3. Parameterized query to fetch stored hash
    cursor.execute(
        'SELECT password_hash FROM users WHERE username=%s',
        (username,)
    )
    row = cursor.fetchone()
    cursor.close()
    db.close()

    # 4. Constant-time password check & generic error
    if not row or not bcrypt.verify(password, row['password_hash']):
        flash('Invalid credentials')
        return redirect(url_for('login_page'))

    # 5. Prevent session fixation
    session.clear()
    session['username'] = username
    return redirect(url_for('dashboard'))
# …existing code…