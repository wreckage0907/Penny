from fastapi import APIRouter, HTTPException
from database.firebase_init import firestore_db

router = APIRouter()

@router.post("/onboarding/{user_id}")
def new_user(user_id: str, first_name: str, last_name: str, email: str, phone: str):
    user_data = {
        "user":{
            "firstName": {first_name},
            "lastName": {last_name},
            "email": {email},
            "phone": {phone},
            "coins": 0
        }
    }
    expense_data = {
                        "years": {
                            "2024": [
                                {
                                    "month": "January",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "February",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "March",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "April",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "May",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "June",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "July",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "August",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "September",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "October",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "November",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "month": "December",
                                    "monthly budget": 0,
                                    "amount spent": 0,
                                    "categories": [
                                        {
                                            "category": "Travel and transport",
                                            "total budget": 0,
                                            "sub categories": [
                                                {}
                                            ]
                                        },
                                        {
                                            "category": "Utility bills",
                                            "total budget": 0,
                                            "sub categories": [
                                                {} 
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    }
    
                
    chatbot_data = {}


    user_ref = firestore_db.collection(user_id).document("user")
    expense_ref = firestore_db.collection(user_id).document("expenses")
    chatbot_ref = firestore_db.collection(user_id).document("chatbot")

    expense_ref.set(expense_data)
    user_ref.set(user_data)
    chatbot_ref.set(chatbot_data)


    return user_data

