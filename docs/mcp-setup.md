# 🔌 PlansGuru Model Context Protocol (MCP) Server

The **PlansGuru MCP Server** allows AI agents (such as Claude Desktop, Cursor, or custom local AI agents) to query your social graph, manage dining preferences, compute group consensus recommendations, and create dinner plans using standardized Model Context Protocol (MCP) tool calls.

---

### 🛠️ Exposed MCP Tools

1. **`get_user_profile(user_id)`**: Inspects a user's encrypted dining preferences, dietary restrictions, and budget.
2. **`add_dining_preference(user_id, predicate, value)`**: Adds dining preferences (`likes_cuisine`, `dietary_restriction`, `budget`, `preferred_time`).
3. **`get_group_consensus_recommendations(participants)`**: Evaluates multi-agent satisfaction across all participants.
4. **`create_dinner_plan(title, restaurant_name, participants, date_time)`**: Creates a group plan and generates a short invite link.
5. **`list_active_plans()`**: Lists active and upcoming dinner plans.

---

### ⚙️ Claude Desktop Configuration (`claude_desktop_config.json`)

To connect PlansGuru to Claude Desktop, add this snippet to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "plansguru": {
      "command": "python",
      "args": [
        "C:/Users/elmst/StudioProjects/plansguru/server/mcp_server.py"
      ]
    }
  }
}
```

Now you can chat with Claude Desktop:
> *"Claude, check my PlansGuru profile and create a Friday night Taco plan with Maya and Jo under $20!"*
