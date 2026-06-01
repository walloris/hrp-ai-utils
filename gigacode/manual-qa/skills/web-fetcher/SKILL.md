---
name: web-fetcher
description: Work with Jira, Confluence, and Stash REST APIs via HTTP (Python, corporate SSL). Use when fetching a Jira issue, a Confluence page by pageId, or a Pull Request from Stash. Same approach as senior-backend-developer — scripts/web_fetch only, never the built-in web_fetch tool.
---

# Web Fetcher

HTTP-клиент для корпоративных API в workflow Manual QA. **Идентичен подходу `senior-backend-developer`.**

## Important: Corporate Environment Requirements

**DO NOT use the built-in `web_fetch` / `WebFetch` tool** — it fails on self-signed SSL certificates.

**DO NOT use MCP Atlassian** for Jira/Confluence in `senior-manual-qa` — use REST below.

### Use `scripts/web_fetch` or `scripts/web_fetch.py`

```bash
# Jira issue
./scripts/web_fetch \
  "https://jira.sberbank.ru/rest/api/2/issue/HRM-11330?fields=summary,description,status,issuelinks" \
  -H 'Authorization: Bearer <TOKEN>' \
  -o jira-issue.json

# Confluence page (pageId from URL)
./scripts/web_fetch \
  "https://confluence.sberbank.ru/rest/api/content/17151460968?expand=body.storage,space,version" \
  -H 'Authorization: Bearer <TOKEN>' \
  -o confluence-page.json

# Stash PR
./scripts/web_fetch \
  "https://stash.sigma.sbrf.ru/rest/api/1.0/projects/HRPLATFORM/repos/<repo>/pull-requests/<id>/changes" \
  -H 'Authorization: Bearer <TOKEN>' \
  -o pr-changes.json
```

Authorization may also be `Authorization: Basic <base64>` — same as in `senior-backend-developer`.

## Features

| Feature | Description |
|---------|-------------|
| HTTPS | Self-signed certificates (SSL verify disabled in script) |
| Methods | GET, POST, PUT, DELETE, PATCH |
| Headers | `-H 'Name: value'` (malformed headers without `:` are rejected) |
| Output | stdout or `-o file.json` |

## Security

- Never log tokens.
- Save large responses to `-o` file instead of pasting into chat.
- Only `http`/`https` URLs.

## References

- `../requirements-analysis/references/sources.md` — полные рецепты Jira / Confluence / PR.
- `scripts/web_fetch.md` — опции CLI.
