from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import MySQLdb
from werkzeug.security import check_password_hash

app = Flask(__name__)
app.secret_key = os.urandom(32)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    db = None
    cursor = None
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        row = cursor.fetchone()
        if not row or not check_password_hash(row[0], password):
            return "Invalid username or password", 401

        session.clear()
        session['username'] = username
        return redirect('/dashboard')
    except Exception:
        return "An error occurred", 500
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()