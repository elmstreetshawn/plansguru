#!/usr/bin/env python3
"""
PlansGuru Model Context Protocol (MCP) Server
Allows AI Agents (Claude Desktop, Cursor, Custom Agents) to interact directly with
PlansGuru's Privacy-First On-Device Social Graph, Preference Engine, and Restaurant Consensus.
"""

import sys
import json
import uuid
import time
from typing import Any, Dict, List

# Restaurant Knowledge Base (Greater Austin, TX)
AUSTIN_RESTAURANTS = [
    {
        "name": "Suerte",
        "cuisine": "Interior Mexican",
        "priceRange": "$$$",
        "location": "East Austin (78702)",
        "dietary": ["Gluten-Free", "Vegetarian", "Vegan"],
        "rating": 4.8
    },
    {
        "name": "Loro",
        "cuisine": "Asian Smokehouse & BBQ",
        "priceRange": "$$",
        "location": "South Lamar (78704)",
        "dietary": ["Gluten-Free", "Dairy-Free"],
        "rating": 4.7
    },
    {
        "name": "Uchi",
        "cuisine": "Japanese & Sushi",
        "priceRange": "$$$$",
        "location": "South Lamar (78704)",
        "dietary": ["Gluten-Free", "Pescatarian"],
        "rating": 4.9
    },
    {
        "name": "Terry Black's BBQ",
        "cuisine": "Texas Pit BBQ",
        "priceRange": "$$",
        "location": "Barton Springs (78704)",
        "dietary": ["Gluten-Free", "Meat Lovers"],
        "rating": 4.8
    },
    {
        "name": "Via 313 Pizza",
        "cuisine": "Detroit Style Pizza",
        "priceRange": "$$",
        "location": "Oak Hill / Central (78749)",
        "dietary": ["Gluten-Free Crust", "Dairy-Free Cheese", "Vegetarian"],
        "rating": 4.6
    },
    {
        "name": "Arlo's Vegan Curbside",
        "cuisine": "Vegan Comfort Food & Burgers",
        "priceRange": "$",
        "location": "East Austin (78702)",
        "dietary": ["100% Vegan", "Plant-Based", "Halal Friendly"],
        "rating": 4.7
    }
]

# Ephemeral in-memory MCP storage
mcp_user_preferences: Dict[str, List[Dict[str, str]]] = {
    "local_user": [
        {"predicate": "likes_cuisine", "value": "Interior Mexican"},
        {"predicate": "budget", "value": "$$"},
        {"predicate": "preferred_time", "value": "Friday @ 7:30 PM"}
    ],
    "Maya": [
        {"predicate": "likes_cuisine", "value": "Interior Mexican"},
        {"predicate": "dietary_restriction", "value": "Gluten-Free"},
        {"predicate": "budget", "value": "$$"}
    ],
    "Jo": [
        {"predicate": "likes_cuisine", "value": "Asian Smokehouse"},
        {"predicate": "budget", "value": "$$"}
    ]
}

mcp_plans: List[Dict[str, Any]] = [
    {
        "planId": "plan_mcp_1",
        "title": "Friday Taco & BBQ Crew",
        "restaurant": "Loro",
        "cuisine": "Asian Smokehouse & BBQ",
        "price": "$$",
        "participants": ["Me", "Maya", "Jo"],
        "matchScore": "96% Match",
        "status": "Active Now"
    }
]


def handle_rpc_request(request: Dict[str, Any]) -> Dict[str, Any]:
    method = request.get("method")
    params = request.get("params", {})
    req_id = request.get("id")

    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {}
                },
                "serverInfo": {
                    "name": "plansguru-mcp-server",
                    "version": "1.2.4"
                }
            }
        }

    elif method == "tools/list":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "tools": [
                    {
                        "name": "get_user_profile",
                        "description": "Get dining preferences, dietary restrictions, and budget for a user.",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "user_id": {"type": "string", "description": "User ID or Display Name (e.g. 'local_user', 'Maya')"}
                            },
                            "required": ["user_id"]
                        }
                    },
                    {
                        "name": "add_dining_preference",
                        "description": "Add a dining preference assertion (cuisine, dietary restriction, budget, or preferred time slot).",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "user_id": {"type": "string", "description": "User ID or Display Name"},
                                "predicate": {"type": "string", "description": "likes_cuisine | dislikes_cuisine | dietary_restriction | budget | preferred_time"},
                                "value": {"type": "string", "description": "Preference value (e.g. 'Interior Mexican', 'Vegan', '$', 'Friday @ 7:30 PM')"}
                            },
                            "required": ["user_id", "predicate", "value"]
                        }
                    },
                    {
                        "name": "get_group_consensus_recommendations",
                        "description": "Calculate group consensus dinner recommendations based on all participants' preferences.",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "participants": {
                                    "type": "array",
                                    "items": {"type": "string"},
                                    "description": "List of participant names/IDs (e.g. ['local_user', 'Maya', 'Jo'])"
                                }
                            },
                            "required": ["participants"]
                        }
                    },
                    {
                        "name": "create_dinner_plan",
                        "description": "Create a new dinner plan and generate deep link invite URLs.",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "title": {"type": "string", "description": "Group Plan Title (e.g. 'Friday Taco Crew')"},
                                "restaurant_name": {"type": "string", "description": "Selected Restaurant Name"},
                                "participants": {"type": "array", "items": {"type": "string"}, "description": "List of participant names"},
                                "date_time": {"type": "string", "description": "Dining Date & Time (e.g. 'Friday @ 7:30 PM')"}
                            },
                            "required": ["title", "restaurant_name", "participants"]
                        }
                    },
                    {
                        "name": "list_active_plans",
                        "description": "List all active and upcoming dinner plans.",
                        "inputSchema": {
                            "type": "object",
                            "properties": {}
                        }
                    }
                ]
            }
        }

    elif method == "tools/call":
        tool_name = params.get("name")
        args = params.get("arguments", {})

        if tool_name == "get_user_profile":
            uid = args.get("user_id", "local_user")
            prefs = mcp_user_preferences.get(uid, [])
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [{
                        "type": "text",
                        "text": json.dumps({"user_id": uid, "preferences": prefs}, indent=2)
                    }]
                }
            }

        elif tool_name == "add_dining_preference":
            uid = args.get("user_id", "local_user")
            pred = args.get("predicate", "likes_cuisine")
            val = args.get("value", "")
            if uid not in mcp_user_preferences:
                mcp_user_preferences[uid] = []
            mcp_user_preferences[uid].append({"predicate": pred, "value": val})
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [{
                        "type": "text",
                        "text": json.dumps({"status": "success", "message": f"Added {pred}={val} for {uid}"})
                    }]
                }
            }

        elif tool_name == "get_group_consensus_recommendations":
            parts = args.get("participants", ["local_user", "Maya"])
            recs = []
            for rest in AUSTIN_RESTAURANTS:
                score = 0.85
                if any("Vegan" in str(mcp_user_preferences.get(p, [])) for p in parts) and "Vegan" in rest["dietary"]:
                    score += 0.10
                recs.append({
                    "restaurant": rest["name"],
                    "cuisine": rest["cuisine"],
                    "price": rest["priceRange"],
                    "matchScore": f"{int(score * 100)}% Match"
                })
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [{
                        "type": "text",
                        "text": json.dumps({"participants": parts, "recommendations": recs}, indent=2)
                    }]
                }
            }

        elif tool_name == "create_dinner_plan":
            title = args.get("title", "Dinner Plan")
            rest = args.get("restaurant_name", "Suerte")
            parts = args.get("participants", ["local_user"])
            dt = args.get("date_time", "Friday @ 7:30 PM")
            plan_id = f"plan_mcp_{int(time.time())}"

            new_plan = {
                "planId": plan_id,
                "title": title,
                "restaurant": rest,
                "participants": parts,
                "dateTime": dt,
                "inviteUrl": f"https://plansguru-austin.loca.lt/i/{plan_id[-6:].upper()}"
            }
            mcp_plans.append(new_plan)
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [{
                        "type": "text",
                        "text": json.dumps({"status": "created", "plan": new_plan}, indent=2)
                    }]
                }
            }

        elif tool_name == "list_active_plans":
            return {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "content": [{
                        "type": "text",
                        "text": json.dumps({"active_plans": mcp_plans}, indent=2)
                    }]
                }
            }

    return {
        "jsonrpc": "2.0",
        "id": req_id,
        "error": {
            "code": -32601,
            "message": f"Method or tool not found: {method}"
        }
    }


def main():
    """Main STDIN/STDOUT JSON-RPC Loop for MCP Server."""
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            request = json.loads(line)
            response = handle_rpc_request(request)
            sys.stdout.write(json.dumps(response) + "\n")
            sys.stdout.flush()
        except Exception as e:
            err_res = {
                "jsonrpc": "2.0",
                "id": None,
                "error": {"code": -32603, "message": str(e)}
            }
            sys.stdout.write(json.dumps(err_res) + "\n")
            sys.stdout.flush()


if __name__ == "__main__":
    main()
