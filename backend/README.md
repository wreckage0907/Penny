# Backend Project Structure

## `main.py`
The entry point of the FastAPI application. It sets up the app, includes routers, and configures CORS.

## `database/`
Contains database-related code.
- `firebase_init.py`: Initializes Firebase app, providing access to Firestore and Storage.

## `routers/`
Contains route handlers for different parts of the API.
- `user.py`: Handles user-related routes (e.g., user information).
- `stock.py`: Manages stock-related routes (e.g., stock information).
- `file.py`: Deals with file-related routes (e.g., retrieving files from Firebase Storage).

## `schemas/`
Defines Pydantic models for data validation.
- `stock.py`: Specifies the structure of a Stock object.

## `services/`
Houses background services or tasks.
- `stock_simulator.py`: Simulates stock price changes and updates the database.

## `firebase.json`
Firebase configuration file containing project settings and credentials.

This structure separates concerns, enhancing modularity and maintainability. It centralizes database initialization, separates API handlers, defines clear data models, and isolates background tasks.


![image](https://github.com/wreckage0907/Penny/assets/119794177/11f7a595-e7a5-4587-b054-f265da8f8aad)
