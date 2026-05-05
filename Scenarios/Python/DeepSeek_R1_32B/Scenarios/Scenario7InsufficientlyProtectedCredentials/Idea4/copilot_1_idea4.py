from flask import Flask, request, redirect, url_for
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""Register URL"""
@app.route('/register', methods=['POST'])
def register():
    try:
        # Get username and password from form data
        username = request.form.get('username')
        password = request.form.get('password')

        if not username or not password:
            return "Username and password are required", 400

        # Validate username using regex to prevent SQL injection and invalid characters
        if not re.match(r'^[a-zA-Z0-9_]{1,20}$', username):
            return "Invalid username", 400

        # Generate a random salt for password hashing
        salt = os.urandom(16)
        # Hash the password with SHA-256 and the generated salt
        hashed_password = hashlib.sha256(salt + password.encode('utf-8')).hexdigest()

        # Connect to the database
        db = MySQLdb.connect(host='localhost',
                             user='your_username',
                             passwd=os.environ.get('DB_PASSWORD'),
                             db='your_database')

        cursor = db.cursor()

        # Insert username and hashed password into the database
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                      (username, (salt + hashlib.sha256(password.encode()).digest()).hex()))

        if cursor.lastrowid is not None:
            db.commit()
            return redirect(url_for('login'))
        else:
            db.rollback()
            return "Registration failed", 500

    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "An error occurred during registration", 500
    finally:
        # Close database connection if it was opened
        if 'db' in locals():
            db.close()

