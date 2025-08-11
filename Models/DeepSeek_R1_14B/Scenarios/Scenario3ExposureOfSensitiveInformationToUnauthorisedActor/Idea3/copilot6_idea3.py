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
    # Extract username and password from form data
    username = request.form['username']
    plain_text_password = request.form['password']

    try:
        # Connect to MySQL database
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Prepare SQL query using parameters to prevent SQL injection
        sql_query = "SELECT id, password_hash FROM users WHERE username = %s"
        cursor.execute(sql_query, (username,))
        result = cursor.fetchone()

        if not result:
            return redirect('/login?error=invalid_credentials')

        user_id, stored_password_hash = result

        # Verify password: hash the plain text password and compare
        sha = hashlib.sha512()
        sha.update(plain_text_password.encode('utf-8'))
        hashed_password = sha.hexdigest()

        if hashed_password == stored_password_hash:
            # Successful login; set session or redirect as needed
            return redirect('/protected_page')

        else:
            return redirect('/login?error=invalid_credentials')

    except Exception as e:
        return redirect('/login?error=invalid_credentials')

    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass  # Ignore any errors during cleanup