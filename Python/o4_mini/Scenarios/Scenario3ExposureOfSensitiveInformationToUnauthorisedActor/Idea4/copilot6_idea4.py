
from flask import Flask, request, redirect, make_response
import os
import re
import bcrypt
import secrets
import MySQLdb

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 1. Input validation (whitelist)
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username or password", 401
    if len(password) < 8:
        return "Invalid username or password", 401

    # 2. Secure DB connection using env vars
    conn = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'app_user'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME')
    )

    try:
        with conn.cursor() as cur:
            # 3. Parameterized query to prevent SQL injection
            cur.execute("SELECT password_hash FROM users WHERE username=%s", (username,))
            row = cur.fetchone()

        if not row:
            return "Invalid username or password", 401

        stored_hash = row[0].encode('utf-8')
        # 4. Verify password using bcrypt
        if not bcrypt.checkpw(password.encode('utf-8'), stored_hash):
            return "Invalid username or password", 401

        # 5. Issue secure session token
        session_token = secrets.token_urlsafe(32)
        # TODO: store session_token mapped to user in a server-side store (Redis, database, etc.)

        resp = make_response(redirect('/dashboard'))
        resp.set_cookie(
            'session_token',
            session_token,
            secure=True,
            httponly=True,
            samesite='Strict'
        )
        return resp

    finally:
        conn.close()