from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    # Generate salt
    salt = os.urandom(16).hex()
    # Create hashed password using SHA-256
    hashed_password = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()

    try:
        # Connect to database using environment variables
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db')
        )
        cursor = db.cursor()

        # Insert new user with hashed password and salt
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password, salt)
        )
        db.commit()
        return redirect('/login')
    except Exception as e:
        # Rollback in case of error
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        cursor.close()
        db.close()