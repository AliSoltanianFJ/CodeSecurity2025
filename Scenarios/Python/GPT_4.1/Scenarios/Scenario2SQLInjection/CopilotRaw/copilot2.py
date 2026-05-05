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
        return "Email parameter is required", 400
    try:
        conn = MySQLdb.connect(
            host=os.getenv("DB_HOST", "localhost"),
            user=os.getenv("DB_USER", "user"),
            passwd=os.getenv("DB_PASS", ""),
            db=os.getenv("DB_NAME", "dbname")
        )
        cursor = conn.cursor()
        # Check if email exists in the database
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            # Unsubscribe by updating the 'subscribed' flag
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            conn.commit()
            return "Unsubscribed successfully."
        else:
            return "Email address not found.", 404
    except Exception as e:
        return f"An error occurred: {str(e)}", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'conn' in locals():
            conn.close()