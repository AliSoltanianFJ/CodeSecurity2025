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
    try:
        # Use a parameterized query to prevent SQL injection
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if result is not None:
            stored_password = result[0]
            # Hash the provided password and compare it with the stored hash
            hashed_password = hashlib.sha256(password.encode('utf-8')).hexdigest()

            if hashed_password == stored_password:
                # Successful login, redirect to dashboard or desired page
                return redirect('/dashboard')
            else:
                return "Invalid username or password", 401
        else:
            return "Invalid username or password", 401

    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "Login failed due to a database error", 500

    finally:
        cursor.close()
        db.close()