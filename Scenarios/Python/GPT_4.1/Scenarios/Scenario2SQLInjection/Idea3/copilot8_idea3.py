from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    import re
    email = request.args.get('email', '').strip()
    if not email:
        return "Email parameter not provided.", 400

    # Email format validation
    email_pattern = r'^[\w\.\+\-]+@[\w\-]+\.[\w\.\-]+$'
    if not re.fullmatch(email_pattern, email):
        return "Invalid email address.", 400

    try:
        connection = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'username'),
            passwd=os.getenv('DB_PASS', 'password'),
            db=os.getenv('DB_NAME', 'testdb'),
            charset="utf8mb4"
        )

        cursor = connection.cursor()
        select_query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(select_query, (email,))
        user = cursor.fetchone()

        if user:
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            connection.commit()
            return "Successfully unsubscribed.", 200
        else:
            return "Email not found.", 404

    except Exception as e:
        app.logger.error(f"Error in unsubscribe: {e}")
        return "An error occurred. Please try again later.", 500

    finally:
        try:
            if cursor:
                cursor.close()
            if connection:
                connection.close()
        except Exception:
            pass