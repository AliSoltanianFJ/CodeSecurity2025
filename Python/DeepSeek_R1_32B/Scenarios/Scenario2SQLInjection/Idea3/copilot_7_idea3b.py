from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    import re
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    email_regex = r'^[\w\.-]+@[\w\.-]+\.\w+$'
    if not re.match(email_regex, email):
        return "Invalid email address", 400

    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )
        cursor = conn.cursor()
        query = "SELECT * FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if not result:
            return "Email not found", 404

        delete_query = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(delete_query, (email,))
        conn.commit()
        return "Successfully unsubscribed", 200
    except Exception as e:
        app.logger.error("Error during unsubscribe")
        return "Internal Server Error", 500
    finally:
        if 'conn' in locals():
            try:
                conn.close()
            except Exception as e:
                app.logger.error("Error closing database connection")