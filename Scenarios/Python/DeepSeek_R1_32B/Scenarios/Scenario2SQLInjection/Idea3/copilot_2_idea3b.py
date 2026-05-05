from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    try:
        connection = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'mailinglist')
        )
        cursor = connection.cursor()
        query = "SELECT email FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        if not result:
            return "Email not found", 404

        update_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
        cursor.execute(update_query, (email,))
        connection.commit()
        return "Successfully unsubscribed", 200
    except Exception as e:
        print("Error:", e, file=sys.stderr)
        return "Internal Server Error", 500
    finally:
        try:
            if 'connection' in locals():
                connection.close()
        except Exception as e:
            pass