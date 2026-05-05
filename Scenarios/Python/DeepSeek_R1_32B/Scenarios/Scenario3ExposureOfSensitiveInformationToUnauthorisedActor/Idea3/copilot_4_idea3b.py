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
    # Retrieve and validate input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation: length and allowed characters
    if not (1 <= len(username) <= 32 and 8 <= len(password) <= 64):
        return "Invalid credentials", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]
        from werkzeug.security import check_password_hash
        if not check_password_hash(stored_hash, password):
            return "Invalid credentials", 401

    except Exception as e:
        print("Login error:", e, file=sys.stderr)
        return "Internal server error", 500

    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    return redirect('/dashboard')