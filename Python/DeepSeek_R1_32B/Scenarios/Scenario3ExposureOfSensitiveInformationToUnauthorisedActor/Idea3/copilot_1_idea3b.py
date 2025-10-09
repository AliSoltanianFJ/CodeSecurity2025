from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from flask import session
from werkzeug.security import check_password_hash

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Retrieve and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Basic validation: check length and allowed characters
    if not (1 <= len(username) <= 50 and 1 <= len(password) <= 50):
        return "Invalid input", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if result and check_password_hash(result[0], password):
            session['username'] = username
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except Exception as e:
        print("Login error:", e, file=sys.stderr)
        return "An error occurred", 500

    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass