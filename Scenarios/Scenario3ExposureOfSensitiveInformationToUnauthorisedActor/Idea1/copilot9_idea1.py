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
    username = request.form['username']
    password = request.form['password']
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # Use a parameterized query to avoid SQL injection
        cursor.execute("SELECT password FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
        
        if row is None:
            return "Invalid credentials", 401
        
        stored_hash = row[0]
        provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
        
        # Use constant-time comparison to mitigate timing attacks
        if hmac.compare_digest(provided_hash, stored_hash):
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401
    except Exception:
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