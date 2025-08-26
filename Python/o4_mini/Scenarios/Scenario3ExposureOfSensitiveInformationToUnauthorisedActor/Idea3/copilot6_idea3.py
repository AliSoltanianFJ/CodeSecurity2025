from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

from flask import session, flash, url_for
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        flash('Username and password required')
        return redirect(url_for('show_login'))

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # parameterized query prevents SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
    except MySQLdb.Error:
        flash('Database error')
        return redirect(url_for('show_login'))
    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass

    # row is None if user not found
    if row and check_password_hash(row[0], password):
        # credentials OK: set session and redirect
        session.clear()
        session['username'] = username
        return redirect(url_for('dashboard'))
    else:
        flash('Invalid username or password')
        return redirect(url_for('show_login'))