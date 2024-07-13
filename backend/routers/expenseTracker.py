from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, ValidationError
from typing import List

from database.firebase_init import firestore_db

router = APIRouter()

# Define Pydantic models

# Create new user with initial expense data
@router.post("/expenses/{user_id}")
async def create_expense(user_id: str, expense: dict):
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
async def update_expense(user_id: str, expense: dict):
    try:
        firestore_db.collection(user_id).document("expenses").update(expense.dict())
        return {"message": "Expense updated successfully."}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


class CategoryModel(BaseModel):
    category: str

@router.post("/expenses/{user_id}/year/{year}/month/{month}/category")
async def add_category(user_id: str, year: str, month: str, category_data: CategoryModel):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Expense document not found.")
        
        expense = doc.to_dict()
        years = expense.get("years", {})
        year_data = years.get(year)
        if not year_data:
            raise HTTPException(status_code=404, detail="Year not found.")

        month_data = next((m for m in year_data if m.get("month") == month), None)
        if not month_data:
            raise HTTPException(status_code=404, detail="Month not found.")

        categories = month_data.setdefault("categories", [])
        categories.append({
            "category": category_data.category,
            "total budget": 0,
            "sub categories": [{}]
        })

        doc_ref.set(expense)
        return {"message": "Category added successfully."}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"An error occurred: {str(e)}")

# Add subcategory to a specific category in a specific month in a specific year
@router.post("/expenses/{user_id}/year/{year}/month/{month}/category/{category}")
async def add_subcategory(user_id: str, year: str, month: str, category: str, subcategory: str, amount_spent: int):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        if doc.exists:
            expense = doc.to_dict()
            if year in expense["years"]:
                for m in expense["years"][year]:
                    if m["month"] == month:
                        for c in m["categories"]:
                            if c["category"] == category:
                                c["sub categories"].append({"name": subcategory, "amount_spent": amount_spent})
                                doc_ref.set(expense)
                                return {"message": "Subcategory added successfully."}
                        raise HTTPException(status_code=404, detail=f"Category '{category}' not found.")
                raise HTTPException(status_code=404, detail=f"Month '{month}' not found.")
            raise HTTPException(status_code=404, detail=f"Year '{year}' not found.")
        else:
            raise HTTPException(status_code=404, detail="Expense document not found.")
    except Exception as e:
        print(f"Error: {str(e)}")  # Log the full error message
        raise HTTPException(status_code=400, detail=str(e))

# Update subcategory
@router.put("/expenses/{user_id}/year/{year}/month/{month}/category/{category}/subcategory/{sub_category}")
async def update_subcategory(user_id: str, year: str, month: str, category: str, sub_category: str, new_sub_category: str, new_amount_spent: int):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        if doc.exists:
            expense = doc.to_dict()
            if year in expense["years"]:
                for m in expense["years"][year]:
                    if m["month"] == month:
                        for c in m["categories"]:
                            if c["category"] == category:
                                for i, sc in enumerate(c["sub categories"]):
                                    if sc.get("name") == sub_category:
                                        c["sub categories"][i] = {"name": new_sub_category, "amount_spent": new_amount_spent}
                                        doc_ref.set(expense)
                                        return {"message": "Subcategory updated successfully."}
                                # If subcategory not found, add it
                                c["sub categories"].append({"name": new_sub_category, "amount_spent": new_amount_spent})
                                doc_ref.set(expense)
                                return {"message": "New subcategory added successfully."}
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
