#!/bin/bash
# Git workflow helper functions
# Source this file to use the functions: source git_workflow.sh

# Get developer login
get_developer_login() {
    # Try to get login from SIGMA environment or git config
    if [ -n "$SIGMA_LOGIN" ]; then
        echo "$SIGMA_LOGIN"
    else
        git config user.name 2>/dev/null || echo "developer"
    fi
}

# Extract JIRA key from URL
# Usage: extract_jira_key "https://jira.sberbank.ru/browse/HRM-11330"
extract_jira_key() {
    local url="$1"
    echo "$url" | grep -oE '[A-Z]+-[0-9]+' | head -1
}

# Get task name from JIRA URL (simplified - requires API)
# For now, use placeholder that will be replaced by user
get_task_name() {
    local jira_key="$1"
    echo "<task-name-from-JIRA>"
}

# Determine branch type from task type
# Usage: get_branch_type "Bug" -> "bugfix"
get_branch_type() {
    local task_type="$1"
    case "${task_type,,}" in
        *bug*|*defect*)
            echo "bugfix"
            ;;
        *feature*|*story*)
            echo "feature"
            ;;
        *tech*debt*|*task*|*improvement*)
            echo "techdebt"
            ;;
        *)
            echo "feature"
            ;;
    esac
}

# Create git branch from JIRA URL
# Usage: create_git_branch "https://jira.sberbank.ru/browse/HRM-11330"
create_git_branch() {
    local jira_url="$1"
    local jira_key
    local branch_type
    local login
    
    jira_key=$(extract_jira_key "$jira_url")
    if [ -z "$jira_key" ]; then
        echo "Error: Could not extract JIRA key from URL"
        return 1
    fi
    
    branch_type=$(get_branch_type "Bug")  # Default to bugfix
    login=$(get_developer_login)
    
    git checkout -b "${branch_type}/${login}/${jira_key}"
}

# Create commit with formatted message
# Usage: commit_changes "[HRM-11330]: <task-name>"
commit_changes() {
    local message="$1"
    git commit -m "$message"
}

# Push current branch
# Usage: push_branch
push_branch() {
    git push origin HEAD
}

# Full workflow: checkout branch, commit, and push
# Usage: complete_workflow "https://jira.sberbank.ru/browse/HRM-11330"
complete_workflow() {
    local jira_url="$1"
    
    create_git_branch "$jira_url" || return 1
    commit_changes "WIP: Work in progress"
    push_branch
}

# Get JIRA info (requires curl and JIRA credentials)
# Usage: get_jira_info "HRM-11330"
get_jira_info() {
    local jira_key="$1"
    local api_url="https://jira.sberbank.ru/rest/api/2/issue/${jira_key}"
    
    # For now, return placeholder
    echo "{\"key\":\"${jira_key}\",\"summary\":\"<task-name>\",\"issuetype\":{\"name\":\"<type>\"}}"
}
