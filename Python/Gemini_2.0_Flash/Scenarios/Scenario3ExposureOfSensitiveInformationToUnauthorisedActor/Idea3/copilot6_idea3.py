from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from flask import session

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return "Missing username or password", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash, salt FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        row = cursor.fetchone()
        if row is None:
            return "Invalid credentials", 401

        stored_hash, salt = row

        computed_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt.encode('utf-8'),
            150_000
        ).hex()

        if computed_hash != stored_hash:
            return "Invalid credentials", 401

        session['username'] = username
        return redirect('/dashboard')
    except Exception as e:
        return "Error occurred", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass