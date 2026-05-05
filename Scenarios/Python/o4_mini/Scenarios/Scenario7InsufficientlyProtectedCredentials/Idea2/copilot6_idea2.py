from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    # Validate input
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password must be at least 8 characters long", 400
    # Securely hash the password
    from werkzeug.security import generate_password_hash
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    # Store user in database using parameterized queries
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        insert_sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(insert_sql, (username, password_hash))
        conn.commit()
    except MySQLdb.Error:
        app.logger.error("Database error during registration")
        return "Internal server error", 500
    finally:
        cursor.close()
        conn.close()
    return redirect('/login')
# ...existing code...