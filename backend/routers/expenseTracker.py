from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from database.firebase_init import firestore_db

router = APIRouter()

class Expense(BaseModel):
    name: str
    amount: float
    category: str
    year: int
    month: str

class UpdateExpense(BaseModel):
    old_name: str
    old_amount: float
    old_category: str
    new_name: str
    new_amount: float
    new_category: str

@router.post("/expenses/{user_id}")
async def add_expense(user_id: str, expense: Expense):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        
        if not doc.exists:
            expense_data = {}
        else:
            expense_data = doc.to_dict()
        
        year_data = expense_data.setdefault(str(expense.year), {})
        month_data = year_data.setdefault(expense.month, {"categories": {}})
        category_data = month_data["categories"].setdefault(expense.category, [])
        
        category_data.append({
            "name": expense.name,
            "amount": expense.amount
        })
        
        doc_ref.set(expense_data)
        return {"message": "Expense added successfully."}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/expenses/{user_id}")
async def update_expense(user_id: str, expense: UpdateExpense, year: int, month: str):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Expense document not found.")
        
        expense_data = doc.to_dict()
        year_data = expense_data.get(str(year))
        if not year_data:
            raise HTTPException(status_code=404, detail="Year not found.")
        
        month_data = year_data.get(month)
        if not month_data:
            raise HTTPException(status_code=404, detail="Month not found.")
        
        old_category_data = month_data["categories"].get(expense.old_category, [])
        
        # Find and update the expense
        expense_updated = False
        for i, e in enumerate(old_category_data):
            if e["name"] == expense.old_name and e["amount"] == expense.old_amount:
                if expense.old_category == expense.new_category:
                    # Update in the same category
                    old_category_data[i] = {
                        "name": expense.new_name,
                        "amount": expense.new_amount
                    }
                else:
                    # Remove from old category
                    del old_category_data[i]
                    # Add to new category
                    new_category_data = month_data["categories"].setdefault(expense.new_category, [])
                    new_category_data.append({
                        "name": expense.new_name,
                        "amount": expense.new_amount
                    })
                expense_updated = True
                break
        
        if not expense_updated:
            raise HTTPException(status_code=404, detail="Expense not found.")
        
        # Clean up empty categories
        if not old_category_data:
            del month_data["categories"][expense.old_category]
        else:
            month_data["categories"][expense.old_category] = old_category_data
        
        doc_ref.set(expense_data)
        return {"message": "Expense updated successfully."}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/expenses/{user_id}")
async def get_expenses(user_id: str, year: Optional[int] = None, month: Optional[str] = None):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        
        if not doc.exists:
            return {}
        
        expense_data = doc.to_dict()
        
        if year:
            expense_data = {str(year): expense_data.get(str(year), {})}
            if month:
                year_data = expense_data.get(str(year), {})
                expense_data = {str(year): {month: year_data.get(month, {})}}
        
        return expense_data
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/expenses/{user_id}")
async def delete_expense(user_id: str, year: int, month: str, category: str, name: str):
    try:
        doc_ref = firestore_db.collection(user_id).document("expenses")
        doc = doc_ref.get()
        
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Expense document not found.")
        
        expense_data = doc.to_dict()
        year_data = expense_data.get(str(year))
        if not year_data:
            raise HTTPException(status_code=404, detail="Year not found.")
        
        month_data = year_data.get(month)
        if not month_data:
            raise HTTPException(status_code=404, detail="Month not found.")
        
        category_data = month_data["categories"].get(category)
        if not category_data:
            raise HTTPException(status_code=404, detail="Category not found.")
        
        category_data = [e for e in category_data if e["name"] != name]
        if not category_data:
            del month_data["categories"][category]
        else:
            month_data["categories"][category] = category_data
        
        if not month_data["categories"]:
            del year_data[month]
        
        if not year_data:
            del expense_data[str(year)]
        
        doc_ref.set(expense_data)
        return {"message": "Expense deleted successfully."}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

class Income(BaseModel):
    amount: float
    year: int
    month: str

@router.post("/income/{user_id}")
async def add_income(user_id: str, income: Income):
    try:
        doc_ref = firestore_db.collection(user_id).document("finances")
        doc = doc_ref.get()
        
        if not doc.exists:
            finance_data = {}
        else:
            finance_data = doc.to_dict()
        
        year_data = finance_data.setdefault(str(income.year), {})
        month_data = year_data.setdefault(income.month, {})
        
        # Add new income to existing amount, or set if not present
        current_income = month_data.get("income", 0)
        month_data["income"] = current_income + income.amount
        
        doc_ref.set(finance_data)
        return {"message": "Income added successfully."}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/income/{user_id}/{year}/{month}")
async def update_income(user_id: str, year: int, month: str, amount: float):
    try:
        doc_ref = firestore_db.collection(user_id).document("finances")
        doc = doc_ref.get()
        
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Finance document not found.")
        
        finance_data = doc.to_dict()
        year_data = finance_data.setdefault(str(year), {})
        month_data = year_data.setdefault(month, {})
        
        # Add the new amount to the existing amount
        current_income = month_data.get("income", 0)
        month_data["income"] = current_income + amount
        
        doc_ref.set(finance_data)
        return {"message": "Income updated successfully."}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/income/{user_id}")
async def get_income(user_id: str, year: Optional[int] = None, month: Optional[str] = None):
    try:
        doc_ref = firestore_db.collection(user_id).document("finances")
        doc = doc_ref.get()
        
        if not doc.exists:
            return {}
        
        finance_data = doc.to_dict()
        
        if year:
            finance_data = {str(year): finance_data.get(str(year), {})}
            if month:
                year_data = finance_data.get(str(year), {})
                finance_data = {str(year): {month: year_data.get(month, {})}}
        
        # Extract only income data
        income_data = {}
        for y, year_data in finance_data.items():
            income_data[y] = {}
            for m, month_data in year_data.items():
                income_data[y][m] = month_data.get("income", 0)
                
        print(income_data)
        
        return income_data
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))