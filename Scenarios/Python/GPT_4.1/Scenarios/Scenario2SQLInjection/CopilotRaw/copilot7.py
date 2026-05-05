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

    try:
        # Connect to your MySQL database; update credentials as needed
        db = MySQLdb.connect(host="localhost", user="yourusername", passwd="yourpassword", db="yourdbname")
        cursor = db.cursor()
        
        # Check if the email exists in the subscribers table
        query = "SELECT email FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        
        if result:
            # Update the subscription status for unsubscribe (assuming a 'subscribed' column)
            update_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return "You have been unsubscribed", 200
        else:
            return "Email not found", 404

    except Exception as e:
        return f"An error occurred: {e}", 500

    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()