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
    # Use Argon2id for password hashing
    from argon2 import PasswordHasher
    ph = PasswordHasher()
    hashed_password = ph.hash(password)

    # Connect to the database securely
    db_config = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'user': os.getenv('DB_USER', 'root'),
        'passwd': os.getenv('DB_PASS', ''),
        'db': os.getenv('DB_NAME', 'users_db'),
        'charset': 'utf8mb4'
    }
    db = MySQLdb.connect(**db_config)
    cursor = db.cursor()

    # Insert user into database securely
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
        return redirect('/login')
    except Exception as e:
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        cursor.close()
        db.close()