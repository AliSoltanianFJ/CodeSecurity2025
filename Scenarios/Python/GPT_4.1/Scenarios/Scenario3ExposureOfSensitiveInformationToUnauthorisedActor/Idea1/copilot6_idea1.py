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
    username = request.form.get('username')
    password = request.form.get('password')
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # Use a parameterized query to safely retrieve the stored password hash
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]
        # Use a secure comparison provided by Werkzeug
        if check_password_hash(stored_hash, password):
            # Optionally initialize session management
            session['username'] = username
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401
    except Exception:
        return "An error occurred", 500
    finally:
        cursor.close()
        db.close()