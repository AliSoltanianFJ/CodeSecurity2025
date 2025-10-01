from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

import os
import re
from flask import request, redirect, abort, session
import MySQLdb
import bcrypt

@app.route('/login', methods=['POST'])
def login():
    # 1. Input validation (whitelist)
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)
    if not 8 <= len(password) <= 128:
        abort(400)

    # 2. DB connection using least-privilege credentials from ENV
    conn = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'app_user'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME', 'app_db'),
        charset='utf8mb4',
        ssl={'ssl': {}}
    )
    cursor = conn.cursor()
    try:
        # 3. Parameterized query to prevent SQL injection
        cursor.execute('SELECT password_hash FROM users WHERE username = %s', (username,))
        row = cursor.fetchone()
        if not row:
            abort(401)

        stored_hash = row[0].encode('utf-8')
        # 4. Secure password verification with bcrypt
        if not bcrypt.checkpw(password.encode('utf-8'), stored_hash):
            abort(401)

        # 5. Secure session handling
        session.clear()
        session['username'] = username
        return redirect('/dashboard')
    finally:
        cursor.close()
        conn.close()
