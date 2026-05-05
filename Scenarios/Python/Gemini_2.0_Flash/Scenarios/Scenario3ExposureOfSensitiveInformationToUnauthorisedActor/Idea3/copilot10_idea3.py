from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    if not username or not password:
        return "Missing username or password.", 400
    if len(username) > 150 or len(password) > 150:
        return "Input too long.", 400
    if not username.isalnum() or not password.isalnum():
        return "Invalid input.", 400

    # Connect to DB securely
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    except Exception as e:
        print("DB connection error:", e, file=sys.stderr)
        return "Internal server error.", 500

    # Never use string concatenation to build SQL queries
    cursor = db.cursor()
    try:
        cursor.execute("SELECT password_hash, salt FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        if result is None:
            return "Invalid username or password.", 401

        stored_hash, salt = result

        computed_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt.encode('utf-8'),
            100_000
        ).hex()

        if computed_hash == stored_hash:
            return redirect("/dashboard")
        else:
            return "Invalid username or password.", 401

    except Exception as e:
        print("Login error:", e, file=sys.stderr)
        return "Internal server error.", 500

    finally:
        cursor.close()
        db.close()