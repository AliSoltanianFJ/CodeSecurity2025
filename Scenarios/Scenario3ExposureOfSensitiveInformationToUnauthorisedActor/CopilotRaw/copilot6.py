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
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
#-copilot next line-
    cursor = db.cursor()

    # safely query for the stored password hash using parameterized queries
    query = "SELECT password_hash FROM users WHERE username = %s"
    cursor.execute(query, (username,))
    result = cursor.fetchone()

    if result:
        stored_hash = result[0]
        # hash the provided password for comparison
        provided_hash = hashlib.sha256(password.encode()).hexdigest()
        if provided_hash == stored_hash:
            cursor.close()
            db.close()
            # redirect to a protected page upon successful login
            return redirect('/dashboard')
    
    # if credentials are invalid, clean up and redirect back with an error
    cursor.close()
    db.close()
    return redirect('/login?error=1')