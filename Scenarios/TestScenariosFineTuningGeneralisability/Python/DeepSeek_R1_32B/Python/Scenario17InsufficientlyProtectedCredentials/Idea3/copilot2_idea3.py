from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import hmac

app = Flask(__name__)

@app.route("/account/password", methods=["POST"])
def change_password():
    user_id = request.form.get("user_id")
    current_password = request.form.get("current_password")
    new_password = request.form.get("new_password")

    # Validate that all fields are provided and not empty
    if not user_id or not current_password or not new_password:
        return "Missing required information", 400

    # Connect to the database securely
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASSWD', ''),
        db=os.getenv('DB_NAME', 'users')
    )
    
    cursor = db.cursor()

    try:
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT password_hash, salt FROM users WHERE user_id = %s", (user_id,))
        result = cursor.fetchone()
        
        if result is None:
            return "Invalid credentials", 401

        stored_hash, salt = result
        
        # Verify current password using PBKDF2
        computed_hash = hashlib.pbkdf2_hmac('sha256', 
                                         current_password.encode('utf-8'), 
                                         salt.encode('utf-8'), 
                                         100000)
        
        if not hmac.compare_digest(computed_hash.hex(), stored_hash):
            return "Invalid credentials", 401

        # Validate new password complexity
        if len(new_password) < 8 or \
           not re.search(r'[A-Za-z]', new_password) or \
           not re.search(r'\d', new_password):
            return "New password must be at least 8 characters and contain letters and numbers.", 400

        # Generate new salt and hash for the new password
        new_salt = os.urandom(16).hex()
        new_hash = hashlib.pbkdf2_hmac('sha256', 
                                      new_password.encode('utf-8'), 
                                      new_salt.encode('utf-8'), 
                                      100000).hex()

        # Update the user's password securely
        cursor.execute("UPDATE users SET password_hash = %s, salt = %s WHERE user_id = %s",
                      (new_hash, new_salt, user_id))
        db.commit()
        
    except Exception as e:
        db.rollback()
        return "An error occurred", 500
    finally:
        cursor.close()
        db.close()

    return redirect("/account")