from flask import Flask, request, redirect, session
from werkzeug.security import check_password_hash  # for secure password verification
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Ensure you set a secure secret key

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    try:
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        if not result:
            return "Invalid credentials", 401
        
        stored_hash = result[0]
        if check_password_hash(stored_hash, password):
            session['username'] = username
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401
    except Exception:
        return "An error occurred", 500
    finally:
        cursor.close()
        db.close()