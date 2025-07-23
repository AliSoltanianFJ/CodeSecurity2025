from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""


from flask import session, url_for, flash
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    if not username or not password:
        flash('Invalid input.', 'error')
        return redirect(url_for('login'))
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4', use_unicode=True)
    cursor = db.cursor()
    cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
    row = cursor.fetchone()
    cursor.close()
    db.close()
    if not row or not check_password_hash(row[0], password):
        flash('Invalid credentials.', 'error')
        return redirect(url_for('login'))
    session.clear()
    session['username'] = username
    return redirect(url_for('dashboard'))
