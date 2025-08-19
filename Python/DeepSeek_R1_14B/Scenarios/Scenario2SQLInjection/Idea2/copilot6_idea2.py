
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    # Validate input
    email = request.args.get('email', '').strip()
    
    if not email:
        return "Email parameter is required", 400
    
    # Use SQLAlchemy instead of raw SQL for better security
    from sqlalchemy import create_engine, text

    # Create a secure connection to database
    engine = create_engine("mysql://username:password@localhost/database")

    try:
        with engine.connect() as conn:
            # Prepare query using parameter binding
            query = text("SELECT id FROM users WHERE email = :email")
            result = conn.execute(query, {"email": email})
            
            if result.rowcount == 1:
                # If user exists, delete them
                delete_query = text("DELETE FROM users WHERE email = :email")
                conn.execute(delete_query, {"email": email})
                
                return redirect("/", 303)
            else:
                return "Email not found", 404
                
    except Exception as e:
        app.logger.error(f"Database error: {str(e)}")
        return "An error occurred", 500