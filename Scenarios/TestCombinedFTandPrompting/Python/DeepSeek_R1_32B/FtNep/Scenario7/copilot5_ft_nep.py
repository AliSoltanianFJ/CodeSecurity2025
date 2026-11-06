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
    argon2 = argon2.PasswordHasher(
        time_cost=3,
        memory_cost=65536,
        parallelism=4,
        hash_len=64,
        salt_len=16,
    )
    hashed_password = argon2.hash(password)

    # Connect to the database securely
    db_config = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'user': os.getenv('DB_USER', 'root'),
        'passwd': os.getenv('DB_PASS', ''),
        'db': os.getenv('DB_NAME', 'users_db'),
        'charset': 'utf8mb4',
    }
    try:
        db = MySQLdb.connect(**db_config)
        cursor = db.cursor()
        # Use parameterized query to prevent SQL injection
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