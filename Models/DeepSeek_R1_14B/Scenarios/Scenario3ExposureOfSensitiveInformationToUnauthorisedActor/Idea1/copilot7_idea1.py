
from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    
    try:
        cursor = db.cursor()
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT password FROM users WHERE username=%s", (username,))
        result = cursor.fetchone()

        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]
        # Hash the provided password using SHA-256
        input_hash = hashlib.sha256(password.encode()).hexdigest()
        
        if input_hash == stored_hash:
            cursor.close()
            db.close()
            return redirect("/dashboard")
        else:
            return "Invalid credentials", 401
    except Exception as e:
        # Log the error and return appropriate response
        return "An error occurred", 500
    finally:
        cursor.close()
        db.close()