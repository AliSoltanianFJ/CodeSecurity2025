from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
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
        
        # Use a parameterized query to safely retrieve the user password hash.
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        row = cursor.fetchone()
        
        if row and check_password_hash(row[0], password):
            return redirect('/dashboard')
        else:
            return "Invalid username or password", 401
    except Exception as e:
        # In production, log the exception internally without exposing details to the user.
        return "An error occurred", 500
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()