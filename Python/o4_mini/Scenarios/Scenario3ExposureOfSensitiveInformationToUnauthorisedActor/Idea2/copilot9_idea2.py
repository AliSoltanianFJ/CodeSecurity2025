from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""

import re
import hmac
from flask import session

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return redirect('/login')
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return redirect('/login')
    if len(password) < 8 or len(password) > 128:
        return redirect('/login')
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
    try:
        cursor = db.cursor()
        cursor.execute("SELECT password_hash, salt FROM users WHERE username=%s", (username,))
        row = cursor.fetchone()
    finally:
        cursor.close()
        db.close()
    if row:
        stored_hash, salt = row
        hashed_input = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt.encode('utf-8'), 100000).hex()
        if hmac.compare_digest(hashed_input, stored_hash):
            session.clear()
            session['user'] = username
            return redirect('/dashboard')
    return redirect('/login')