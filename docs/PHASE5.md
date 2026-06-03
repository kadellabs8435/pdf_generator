# Phase 5 — Future Work (Stubs)

Phase 5 is intentionally **not implemented** in v1. Stubs exist so future work can plug in without refactoring core flows.

## AI Narration Suggestions

**Backend stub:** `NarrationSuggestionService`  
**Endpoint:** `POST /api/statements/suggest-narrations`

Request body:
```json
{
  "context": "merchant name or note",
  "transactionType": "UPI"
}
```

Response returns static suggestions today. Replace service implementation with OpenAI or a local LLM adapter.

## Android App (Phase 2 mobile)

Recommended approach:

- Separate `mobile/` module or repository
- Consume the same JWT REST API
- Screens: login/OTP, statement list (approved for Viewer), PDF download

No mobile code is included in this repo yet.

## Integration Checklist

- [ ] Connect real SMS/email OTP providers (`OtpSender` interface)
- [ ] Replace `NarrationSuggestionService` with LLM provider
- [ ] Add cloud PDF storage (S3/Azure Blob) instead of local `storage/pdfs/`
- [ ] Build Android client against `/api/*` endpoints
