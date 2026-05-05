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
        return "Missing email parameter.", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        cursor = connection.cursor()

        # Use a parameterized query to avoid SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe by deleting the record with a parameterized query
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            connection.commit()
            response, status = f"The email {email} has been successfully unsubscribed.", 200
        else:
            response, status = f"The email {email} was not found.", 404

        cursor.close()
        connection.close()
        return response, status

    except MySQLdb.Error as e:
        return f"Database error: {e}", 500