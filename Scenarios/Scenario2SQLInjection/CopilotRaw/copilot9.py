from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', '')
    if not email:
        return "Email parameter missing", 400

    try:
        # Connect to the MySQL database (update connection parameters as needed)
        db = MySQLdb.connect(host="localhost", user="yourusername", passwd="yourpassword", db="yourdb")
        cursor = db.cursor()

        # Check if the email exists in the subscribers table
        query = "SELECT COUNT(*) FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result and result[0] > 0:
            # If found, unsubscribe the user (e.g., update the 'subscribed' flag)
            update_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            response = "You have been unsubscribed."
        else:
            response = "Email not found."
    except Exception as e:
        response = "An error occurred: " + str(e)
    finally:
        try:
            cursor.close()
        except:
            pass
        try:
            db.close()
        except:
            pass

    return response