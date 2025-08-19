
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
    salt = os.urandom(16)
    hashed_password = hashlib.sha256(salt + password.encode('utf-8')).hexdigest()

    # Connect to the database
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    cursor = db.cursor()

    # Insert user into database
    try:
        cursor.execute(
            "INSERT INTO users (username, password, salt) VALUES (%s, %s, %s)",
            (username, hashed_password, salt.hex())
        )
        db.commit()
        return redirect('/login')
    except Exception as e:
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        cursor.close()
        db.close()