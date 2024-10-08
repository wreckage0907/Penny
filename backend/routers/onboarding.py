from fastapi import APIRouter, HTTPException
from database.firebase_init import firestore_db
from schema.onboarding import User, CustomClaimsRequest, UpdateCustomClaims
from firebase_admin import auth, storage

router = APIRouter()

@router.post("/onboarding/{user_id}")
def new_user(user_id: str, user: User):
    user_data = {
        "user": {
            "firstName": user.first_name,
            "lastName": user.last_name,
            "email": user.email,
            "phone": user.phone,
            "coins": 0
        }
    }
    # expense_data = {
    #     "years": {
    #         "2024": [
    #             {
    #                 "month": "January",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "February",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "March",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "April",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "May",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "June",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "July",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "August",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "September",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "October",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "November",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             },
    #             {
    #                 "month": "December",
    #                 "monthly budget": 0,
    #                 "amount spent": 0,
    #                 "categories": [
    #                     {
    #                         "category": "Travel and transport",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     },
    #                     {
    #                         "category": "Utility bills",
    #                         "total budget": 0,
    #                         "sub categories": [
    #                             {}
    #                         ]
    #                     }
    #                 ]
    #             }
    #         ]
    #     }
    # }
    expense_data = {}
    chatbot_data = {}

    user_ref = firestore_db.collection(user_id).document("user")
    expense_ref = firestore_db.collection(user_id).document("expenses")
    chatbot_ref = firestore_db.collection(user_id).document("chatbot")

    expense_ref.set(expense_data)
    user_ref.set(user_data)
    chatbot_ref.set(chatbot_data)

    return user_data

@router.get("/onboarding/{user_id}")
def get_user(user_id: str):
    user_ref = firestore_db.collection(user_id).document("user")
    user = user_ref.get()
    if user.exists:
        return user.to_dict()
    else:
        raise HTTPException(status_code=404, detail="User not found")

@router.put("/onboarding/{user_id}")
def update_user(user_id: str, user: User):
    user_ref = firestore_db.collection(user_id).document("user")
    if not user_ref.get().exists:
        raise HTTPException(status_code=404, detail="User not found")

    updated_user_data = {
        "user": {
            "firstName": user.first_name,
            "lastName": user.last_name,
            "email": user.email,
            "phone": user.phone
        }
    }
    user_ref.update(updated_user_data)
    return updated_user_data

@router.delete("/onboarding/{user_id}")
def delete_user(user_id: str):
    user_ref = firestore_db.collection(user_id).document("user")
    expense_ref = firestore_db.collection(user_id).document("expenses")
    chatbot_ref = firestore_db.collection(user_id).document("chatbot")

    if user_ref.get().exists:
        user_ref.delete()
        expense_ref.delete()
        chatbot_ref.delete()

        bucket = storage.bucket()
        blobs = bucket.list_blobs(prefix=f"profile/{user_id}/")
        for blob in blobs:
            blob.delete()

        try:
            auth.delete_user(user_id)
        except auth.UserNotFoundError:
            pass

        return {"message": "User and associated data deleted successfully"}
    else:
        raise HTTPException(status_code=404, detail="User not found")
    
@router.post("/set-custom-claims")
async def set_custom_claims(claims_request: CustomClaimsRequest):
    try:
        auth.set_custom_user_claims(claims_request.uid, {
            'username': claims_request.username, 
            'fullName': claims_request.fullName
        })
        return {"success": True}
    except Exception as e:
        print(f"Error setting custom claims: {e}")
        raise HTTPException(status_code=500, detail="Failed to set custom claims")

@router.put("/update-custom-claims/{user_id}")
async def update_custom_claims(user_id: str, claims_request: UpdateCustomClaims):
    try:
        # Get the current custom claims
        user = auth.get_user(user_id)
        current_claims = user.custom_claims or {}

        # Update the claims
        current_claims.update({
            'username': claims_request.username,
            'fullName': claims_request.fullName
        })

        # Set the updated custom claims
        auth.set_custom_user_claims(user_id, current_claims)
        return {"success": True, "message": "Custom claims updated successfully"}
    except Exception as e:
        print(f"Error updating custom claims: {e}")
        raise HTTPException(status_code=500, detail="Failed to update custom claims")

@router.delete("/delete-custom-claims/{user_id}")
async def delete_custom_claims(user_id: str):
    try:
        auth.set_custom_user_claims(user_id, None)
        return {"success": True, "message": "Custom claims deleted successfully"}
    except Exception as e:
        print(f"Error deleting custom claims: {e}")
        raise HTTPException(status_code=500, detail="Failed to delete custom claims")