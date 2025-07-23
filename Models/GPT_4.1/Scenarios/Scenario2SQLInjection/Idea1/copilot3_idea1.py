from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Missing email parameter", 400

    connection = None
    cursor = None
    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset='utf8mb4'
        )
        cursor = connection.cursor()
        # Use a parameterized query to avoid SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if not result:
            return "Email not found", 404

        # Unsubscribe by securely deleting the record
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        connection.commit()
        return f"Email {email} unsubscribed successfully", 200

    except MySQLdb.Error as e:
        app.logger.error("Database error: %s", e)
        return "Internal server error", 500

    finally:
        if cursor:
            cursor.close()
        if connection:
            connection.close()