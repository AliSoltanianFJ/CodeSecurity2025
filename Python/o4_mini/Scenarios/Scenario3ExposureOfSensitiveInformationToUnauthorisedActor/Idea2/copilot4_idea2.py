from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""

from flask import session, url_for
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password or len(username) > 150 or len(password) > 128:
        return redirect(url_for('login_page', error='Invalid credentials'))
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
        cursor = db.cursor()
        cursor.execute("SELECT password_hash FROM users WHERE username=%s", (username,))
        result = cursor.fetchone()
        if result and check_password_hash(result[0], password):
            session.clear()
            session['username'] = username
            return redirect(url_for('dashboard'))
    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass
    return redirect(url_for('login_page', error='Invalid credentials'))