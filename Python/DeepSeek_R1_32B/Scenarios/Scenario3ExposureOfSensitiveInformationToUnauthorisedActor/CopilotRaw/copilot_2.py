from flask import Flask, request, redirect, session
import hashlib
import os
import MySQLdb

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Required for sessions

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']

    # Hash the password using SHA-256
    hashed_password = hashlib.sha256(password.encode()).hexdigest()

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT * FROM users WHERE username=%s AND password_hash=%s"
        cursor.execute(query, (username, hashed_password))
        result = cursor.fetchone()

        if result:
            # Store user information in session
            session['username'] = username
            return redirect('/home')
        else:
            # Return error message or redirect to login page with an error
            return "Invalid username or password", 401

    except MySQLdb.Error as e:
        print(f"Error: {e}")
        return "An error occurred while processing your request", 500

    finally:
        if 'db' in locals():
            db.close()

