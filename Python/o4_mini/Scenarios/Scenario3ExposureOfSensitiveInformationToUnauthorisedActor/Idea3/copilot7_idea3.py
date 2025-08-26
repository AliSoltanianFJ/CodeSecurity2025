from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

from flask import session, flash, url_for
from werkzeug.security import check_password_hash

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        flash('Username and password required')
        return redirect(url_for('show_login'))

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root_pass", db="db")
        cursor = db.cursor()
        # Used parameterized query to prevent SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
    except MySQLdb.Error:
        flash('Internal database error')
        return redirect(url_for('show_login'))
    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass
    
    if row and check_password_hash(row[0], password):
        session.clear()
        session['username'] = username
        return redirect(url_for('dashboard'))
    else:
        flash('Invalid username or password')
        return redirect(url_for('show_login'))