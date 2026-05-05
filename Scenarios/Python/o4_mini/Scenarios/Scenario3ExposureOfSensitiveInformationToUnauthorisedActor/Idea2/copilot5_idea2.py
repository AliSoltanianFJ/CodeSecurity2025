from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""

from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb
import re
import hmac

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not (1 <= len(username) <= 150 and 8 <= len(password) <= 128):
        return redirect('/login?error=invalid_credentials')
    if not re.match(r'^\w+$', username):
        return redirect('/login?error=invalid_credentials')
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset="utf8mb4", use_unicode=True)
    cursor = db.cursor(MySQLdb.cursors.DictCursor)
    try:
        cursor.execute(
            "SELECT password_hash, password_salt FROM users WHERE username = %s",
            (username,)
        )
        user = cursor.fetchone()
        if not user:
            return redirect('/login?error=invalid_credentials')
        salt = user['password_salt']
        stored_hash = user['password_hash']
        computed_hash = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()
        if not hmac.compare_digest(computed_hash, stored_hash):
            return redirect('/login?error=invalid_credentials')
        session.clear()
        session['username'] = username
        return redirect('/dashboard')
    finally:
        cursor.close()
        db.close()