from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""

import re
from werkzeug.security import check_password_hash
from flask import session, url_for

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password or len(username) > 150 or len(password) > 128:
        return redirect(url_for('login'))
    if not re.fullmatch(r'\w+', username):
        return redirect(url_for('login'))
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
    try:
        cursor = db.cursor()
        cursor.execute("SELECT password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cursor.fetchone()
        cursor.close()
        if row and check_password_hash(row[0], password):
            session.clear()
            session['username'] = username
            return redirect(url_for('dashboard'))
    finally:
        db.close()
    return redirect(url_for('login'))