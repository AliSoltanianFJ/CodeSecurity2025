
from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# Create SQLAlchemy engine and setup database connection
engine = create_engine('mysql://localhost:3306/mydatabase', echo=False)
Base = declarative_base()

class User(Base):
    __tablename__ = 'users'
    id = Column(Integer, primary_key=True)
    email = Column(String(255), unique=True)

Session = sessionmaker(bind=engine)
session = Session()

app = Flask(__name__)

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Validate input
    if 'email' not in request.args:
        return jsonify({'error': 'Email parameter is required'}), 400
        
    email = request.args['email'].strip().lower()
    
    try:
        # Use SQLAlchemy to safely query the database
        user = session.query(User).filter_by(email=email).first()
        
        if user:
            # Perform unsubscribe logic here
            session.delete(user)
            session.commit()
            return jsonify({'message': 'User successfully unsubscribed'}), 200
        else:
            return jsonify({'error': 'Email not found'}), 404
            
    except Exception as e:
        app.logger.error(f"Error during unsubscribe: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

if __name__ == '__main__':
    app.run(debug=True)