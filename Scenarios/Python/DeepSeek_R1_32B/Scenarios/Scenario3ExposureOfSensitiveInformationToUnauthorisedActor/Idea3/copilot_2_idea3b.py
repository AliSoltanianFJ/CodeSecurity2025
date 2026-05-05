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
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        return "Missing credentials", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash, salt FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if result is None:
            return "Invalid credentials", 401

        stored_hash, salt = result
        computed_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt.encode('utf-8'), 100000).hex()

        if computed_hash == stored_hash:
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except Exception as e:
        print("Login error:", e, file=sys.stderr)
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass