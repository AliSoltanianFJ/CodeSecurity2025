from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
import hmac

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username')
    password = request.form.get('password')
    if not username or not password:
        return "Missing credentials", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # Use a parameterized query to safely fetch the password hash
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        row = cursor.fetchone()

        if row is None:
            return "Invalid credentials", 401

        stored_hash = row[0]
        computed_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

        # Use constant-time comparison to prevent timing attacks
        if hmac.compare_digest(computed_hash, stored_hash):
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except Exception as e:
        app.logger.error(f"Error during login: {e}")
        return "An error occurred", 500

    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass