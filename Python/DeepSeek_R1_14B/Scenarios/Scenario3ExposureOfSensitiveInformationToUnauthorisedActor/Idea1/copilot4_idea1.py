
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
    
    try:
        with MySQLdb.connect(host="localhost", user="root", passwd="root", db="db") as db:
            with db.cursor() as cursor:
                # Use parameterized query to prevent SQL injection
                cursor.execute("SELECT password FROM users WHERE username=%s", (username,))
                result = cursor.fetchone()

                if not result:
                    return "Invalid credentials", 401

                stored_hash = result[0]
                
                # Hash the provided password using SHA-256
                hashed_password = hashlib.sha256(password.encode()).hexdigest()
                
                if hashed_password == stored_hash:
                    return redirect("/dashboard")
                else:
                    return "Invalid credentials", 401

    except Exception as e:
        # Log error and return internal server error
        return "An error occurred", 500