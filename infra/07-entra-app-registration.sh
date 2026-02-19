#!/usr/bin/env bash
###############################################################################
# 07-entra-app-registration.sh
# Registers Entra ID (Azure AD) app for Sedin Pre-Sales authentication
###############################################################################
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
APP_DISPLAY_NAME="Sedin Pre-Sales Asset Manager"
SPA_REDIRECT_URIS=(
    "http://localhost:5173"
    "https://sedin-presales.azurewebsites.net"
)
API_IDENTIFIER_URI="api://sedin-presales"
API_SCOPE_NAME="access_as_user"
API_SCOPE_DESCRIPTION="Access the Pre-Sales API as a user"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }

# ---------------------------------------------------------------------------
# Get Tenant ID
# ---------------------------------------------------------------------------
TENANT_ID=$(az account show --query "tenantId" -o tsv)
info "Tenant ID: $TENANT_ID"

# ---------------------------------------------------------------------------
# Check for existing app registration
# ---------------------------------------------------------------------------
info "Checking if app registration '$APP_DISPLAY_NAME' already exists..."
EXISTING_APP_ID=$(az ad app list \
    --display-name "$APP_DISPLAY_NAME" \
    --query "[0].appId" -o tsv 2>/dev/null || echo "")

if [ -n "$EXISTING_APP_ID" ] && [ "$EXISTING_APP_ID" != "None" ]; then
    warn "App registration '$APP_DISPLAY_NAME' already exists (Client ID: $EXISTING_APP_ID)."
    APP_ID="$EXISTING_APP_ID"
    OBJECT_ID=$(az ad app show --id "$APP_ID" --query "id" -o tsv)
else
    # -----------------------------------------------------------------------
    # Create app registration
    # -----------------------------------------------------------------------
    info "Creating app registration '$APP_DISPLAY_NAME'..."

    APP_ID=$(az ad app create \
        --display-name "$APP_DISPLAY_NAME" \
        --sign-in-audience "AzureADMyOrg" \
        --query "appId" -o tsv)

    OBJECT_ID=$(az ad app show --id "$APP_ID" --query "id" -o tsv)
    ok "App registration created. Client ID: $APP_ID"
fi

# ---------------------------------------------------------------------------
# Configure SPA redirect URIs
# ---------------------------------------------------------------------------
info "Configuring SPA redirect URIs..."

# Build the redirect URIs JSON array
REDIRECT_JSON=$(printf '%s\n' "${SPA_REDIRECT_URIS[@]}" | jq -R . | jq -s .)

az rest --method PATCH \
    --uri "https://graph.microsoft.com/v1.0/applications/${OBJECT_ID}" \
    --headers "Content-Type=application/json" \
    --body "{
        \"spa\": {
            \"redirectUris\": ${REDIRECT_JSON}
        }
    }"
ok "SPA redirect URIs configured."

# ---------------------------------------------------------------------------
# Set Application ID URI
# ---------------------------------------------------------------------------
info "Setting Application ID URI to '$API_IDENTIFIER_URI'..."
az ad app update \
    --id "$APP_ID" \
    --identifier-uris "$API_IDENTIFIER_URI" 2>/dev/null || \
    warn "Could not set identifier URI (may already be set or require admin consent)."

# ---------------------------------------------------------------------------
# Expose API scope
# ---------------------------------------------------------------------------
info "Configuring API scope '$API_SCOPE_NAME'..."

# Generate a UUID for the scope
SCOPE_ID=$(python3 -c "import uuid; print(uuid.uuid4())" 2>/dev/null || uuidgen | tr '[:upper:]' '[:lower:]')

az rest --method PATCH \
    --uri "https://graph.microsoft.com/v1.0/applications/${OBJECT_ID}" \
    --headers "Content-Type=application/json" \
    --body "{
        \"api\": {
            \"oauth2PermissionScopes\": [
                {
                    \"id\": \"${SCOPE_ID}\",
                    \"adminConsentDescription\": \"${API_SCOPE_DESCRIPTION}\",
                    \"adminConsentDisplayName\": \"${API_SCOPE_NAME}\",
                    \"isEnabled\": true,
                    \"type\": \"User\",
                    \"userConsentDescription\": \"${API_SCOPE_DESCRIPTION}\",
                    \"userConsentDisplayName\": \"${API_SCOPE_NAME}\",
                    \"value\": \"${API_SCOPE_NAME}\"
                }
            ]
        }
    }" 2>/dev/null || warn "API scope may already exist. Skipping."

ok "API scope configured: ${API_IDENTIFIER_URI}/${API_SCOPE_NAME}"

# ---------------------------------------------------------------------------
# Create service principal (if not exists)
# ---------------------------------------------------------------------------
info "Ensuring service principal exists..."
az ad sp show --id "$APP_ID" &>/dev/null || \
    az ad sp create --id "$APP_ID" --output none
ok "Service principal ready."

###############################################################################
# Summary
###############################################################################
echo ""
ok "Done - Entra ID App Registration complete."
echo ""
echo "  App Display Name:    $APP_DISPLAY_NAME"
echo "  Client ID (App ID):  $APP_ID"
echo "  Tenant ID:           $TENANT_ID"
echo "  Object ID:           $OBJECT_ID"
echo "  API Scope:           ${API_IDENTIFIER_URI}/${API_SCOPE_NAME}"
echo "  SPA Redirect URIs:   ${SPA_REDIRECT_URIS[*]}"
