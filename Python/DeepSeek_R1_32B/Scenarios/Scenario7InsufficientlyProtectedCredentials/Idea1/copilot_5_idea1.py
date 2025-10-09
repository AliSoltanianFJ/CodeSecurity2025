from flask import Flask, request, redirect
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password with a salt using pbkdf2_hmac for security"""

    # Generate a random salt
    salt = os.urandom(16)
    
    # Use PBKDF2 to hash the password with the salt and 100,000 iterations
    hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000).hex()

    # Connect to the database using environment variables for credentials
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    cursor = db.cursor()

    try:
        # Insert the new user into the database
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password, salt.hex())
        )
        db.commit()
        return redirect('/login')
    except Exception as e:
        # Rollback on error
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        # Ensure connections are closed properly
        cursor.close()
        db.close()

