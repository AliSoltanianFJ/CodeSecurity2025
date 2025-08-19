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
    
    cursor = db.cursor()
    query = "SELECT * FROM users WHERE username=%s AND password_hash=%s"
    hashed_password = hashlib.sha512(password.encode()).hexdigest()
    
    try:
        cursor.execute(query, (username, hashed_password))
        result = cursor.fetchone()
        
        if result is not None:
            return redirect('/success')  # Redirect to a success page if login is successful
        else:
            return "Invalid username or password", 401  # Return an error if authentication fails
        
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return "Internal Server Error", 500

    finally:
        cursor.close()
        db.close()