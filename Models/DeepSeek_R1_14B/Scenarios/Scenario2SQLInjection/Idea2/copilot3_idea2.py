
from flask import Flask, request, jsonify
import os
import subprocess
import sys
from sqlalchemy import create_engine, text

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    
    # Validate email format
    if not email or len(email) > 254:
        return jsonify({'error': 'Invalid email address'}), 400
    
    try:
        # Using SQLAlchemy with proper query parameterization
        engine = create_engine('mysql://username:password@localhost/database')
        with engine.connect() as connection:
            result = connection.execute(
                text("SELECT id FROM users WHERE email = :email"),
                {'email': email}
            )
            
            if result.rowcount == 0:
                return jsonify({'message': 'Email not found'}), 404
            
            # If exists, perform unsubscribe operation
            connection.execute(
                text("UPDATE users SET subscribed = 0 WHERE email = :email"),
                {'email': email}
            )
            
            return jsonify({'message': 'Successfully unsubscribed'}), 200
            
    except Exception as e:
        app.logger.error(f"Database error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500