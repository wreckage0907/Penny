from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List

from database.firebase_init import firestore_db

router = APIRouter()

# Define Pydantic models
class SubCategory(BaseModel):
    sub_category: str
    amount_spent: int

class Category(BaseModel):
    category: str
    total_budget: int
    sub_categories: List[SubCategory]

class Month(BaseModel):
    month: str
    monthly_budget: int
    amount_spent: int
    categories: List[Category]

class Year(BaseModel):
    year: int
    months: List[Month]

class Expense(BaseModel):
    years: List[Year]

# Create new user with initial expense data
@router.post("/expenses/{user_id}")
async def create_expense(user_id: str, expense: Expense):
    try:
        firestore_db.collection(user_id).document("expenses").set(expense.dict())
        return {"message": "Expense created successfully."}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Read entire expense for a user
@router.get("/expenses/{user_id}")
async def read_expense(user_id: str):
    try:
        expense = firestore_db.collection(user_id).document("expenses").get()
        if expense.exists:
            return expense.to_dict()
        else:
            raise HTTPException(status_code=404, detail="Expense not found.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Read expense for a specific month in a specific year for a user
@router.get("/expenses/{user_id}/year/{year}/month/{month}")
async def read_month_expense(user_id: str, year: int, month: str):
    try:
        doc = firestore_db.collection(user_id).document("expenses").get()
        if doc.exists:
            expense = doc.to_dict()
            for y in expense["years"]:
                if y["year"] == year:
                    for m in y["months"]:
                        if m["month"] == month:
                            return m
            raise HTTPException(status_code=404, detail="Year or month not found.")
        else:
            raise HTTPException(status_code=404, detail="Expense not found.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Update user expense
@router.put("/expenses/{user_id}")
async def update_expense(user_id: str, expense: Expense):
    try:
        firestore_db.collection(user_id).document("expenses").update(expense.dict())
        return {"message": "Expense updated successfully."}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Add category to a specific month in a specific year
@router.post("/expenses/{user_id}/year/{year}/month/{month}")
async def add_category(user_id: str, year: int, month: str, category: Category):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        if doc.exists:
            expense = doc.to_dict()
            for y in expense["years"]:
                if y["year"] == year:
                    for m in y["months"]:
                        if m["month"] == month:
                            m["categories"].append(category.dict())
                            doc_ref.set(expense)
                            return {"message": "Category added successfully."}
            raise HTTPException(status_code=404, detail="Year or month not found.")
        else:
            raise HTTPException(status_code=404, detail="Expense not found.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Add subcategory to a specific category in a specific month in a specific year
@router.post("/expenses/{user_id}/year/{year}/month/{month}/category/{category}")
async def add_subcategory(user_id: str, year: int, month: str, category: str, subcategory: SubCategory):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        if doc.exists:
            expense = doc.to_dict()
            for y in expense["years"]:
                if y["year"] == year:
                    for m in y["months"]:
                        if m["month"] == month:
                            for c in m["categories"]:
                                if c["category"] == category:
                                    c["sub_categories"].append(subcategory.dict())
                                    doc_ref.set(expense)
                                    return {"message": "Subcategory added successfully."}
            raise HTTPException(status_code=404, detail="Year, month, or category not found.")
        else:
            raise HTTPException(status_code=404, detail="Expense not found.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Update subcategory
@router.put("/expenses/{user_id}/year/{year}/month/{month}/category/{category}/subcategory/{sub_category}")
async def update_subcategory(user_id: str, year: int, month: str, category: str, sub_category: str, amount_spent: int):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        if doc.exists:
            expense = doc.to_dict()
            for y in expense["years"]:
                if y["year"] == year:
                    for m in y["months"]:
                        if m["month"] == month:
                            for c in m["categories"]:
                                if c["category"] == category:
                                    for sc in c["sub_categories"]:
                                        if sc["sub_category"] == sub_category:
                                            sc["amount_spent"] = amount_spent
                                            doc_ref.set(expense)
                                            return {"message": "Subcategory updated successfully."}
            raise HTTPException(status_code=404, detail="Year, month, category, or subcategory not found.")
        else:
            raise HTTPException(status_code=404, detail="Expense not found.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Delete a subcategory
@router.delete("/expenses/{user_id}/year/{year}/month/{month}/category/{category}/subcategory/{sub_category}")
async def delete_subcategory(user_id: str, year: int, month: str, category: str, sub_category: str):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        if doc.exists:
            expense = doc.to_dict()
            for y in expense["years"]:
                if y["year"] == year:
                    for m in y["months"]:
                        if m["month"] == month:
                            for c in m["categories"]:
                                if c["category"] == category:
                                    c["sub_categories"] = [sc for sc in c["sub_categories"] if sc["sub_category"] != sub_category]
                                    doc_ref.set(expense)
                                    return {"message": "Subcategory deleted successfully."}
            raise HTTPException(status_code=404, detail="Year, month, category, or subcategory not found.")
        else:
            raise HTTPException(status_code=404, detail="Expense not found.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Delete an entire category
@router.delete("/expenses/{user_id}/year/{year}/month/{month}/category/{category}")
async def delete_category(user_id: str, year: int, month: str, category: str):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        if doc.exists:
            expense = doc.to_dict()
            for y in expense["years"]:
                if y["year"] == year:
                    for m in y["months"]:
                        if m["month"] == month:
                            m["categories"] = [c for c in m["categories"] if c["category"] != category]
                            doc_ref.set(expense)
                            return {"message": "Category deleted successfully."}
            raise HTTPException(status_code=404, detail="Year, month, or category not found.")
        else:
            raise HTTPException(status_code=404, detail="Expense not found.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
