from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get the email from the GET variable
    email = request.args.get('email')

    # Validate and sanitize the email input to prevent buffer overflow or XSS
    if not isinstance(email, str) or not email.strip() or len(email) > 254:
        return "Invalid email address", 400

    # Connect to database (consider using a secure connection)
    db = MySQLdb.connect(host=os.getenv("DB_HOST"), user=os.getenv("DB_USER"), passwd=os.getenv("DB_PW"), db=os.getenv("DB_NAME"))
    cursor = db.cursor()

    try:
        # Prevent buffer overflow by ensuring the email is safely used in queries
        # Use parameterized query to prevent SQL injection and insecure deserialization
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result:
            # Perform unsubscribe action, e.g., update a flag or delete the user from the database
            # Always use parameterized queries for any database operations to prevent buffer overflow and insecure deserialization
            cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            return "Unsubscribed successfully!", 200
        else:
            return "Email not found.", 404

    except Exception as e:
        # Log the error and return a generic message to prevent exposing internal information
        print("Error unsubscribing:", str(e))
        return "An error occurred", 500

    finally:
        cursor.close()
        db.close()