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
        db = MySQLdb.connect(host="localhost", user="username", passwd="password", db="database")
        cursor = db.cursor()
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        if not result:
            return "Email not found", 404

        update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
        cursor.execute(update_query, (email,))
        db.commit()
        return "Successfully unsubscribed", 200
    except Exception as e:
        return "Internal server error", 500
    finally:
        try:
            if cursor:
                cursor.close()
            if db:
                db.close()
        except Exception:
            pass