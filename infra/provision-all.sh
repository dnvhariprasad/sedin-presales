#!/usr/bin/env bash
###############################################################################
# provision-all.sh
# Master provisioning script - runs all infrastructure scripts in order
###############################################################################
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/output"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()    { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()      { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()    { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error()   { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }
header()  { echo -e "\n\033[1;36m========================================\033[0m"; echo -e "\033[1;36m  $*\033[0m"; echo -e "\033[1;36m========================================\033[0m\n"; }

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SUBSCRIPTION="d12e5944-0643-477a-a803-fe75f60af762"
RESOURCE_GROUP="rg-sedin-presales"
LOCATION="eastus"

# Resource names (must match individual scripts)
STORAGE_ACCOUNT="stsedinpresales"
PG_SERVER="psql-sedin-presales"
PG_DB="presalesdb"
PG_ADMIN="presalesadmin"
AOAI_NAME="aoai-sedin-presales"
SEARCH_NAME="search-sedin-presales"
DI_NAME="di-sedin-presales"
ASP_NAME="asp-sedin-presales"
APP_API_NAME="app-sedin-presales-api"
FUNC_APP_NAME="func-sedin-presales"
SWA_NAME="swa-sedin-presales"

# ---------------------------------------------------------------------------
# Pre-flight checks
# ---------------------------------------------------------------------------
header "Pre-Flight Checks"

# Check az CLI
if ! command -v az &>/dev/null; then
    error "Azure CLI (az) is not installed. Install from https://aka.ms/install-azure-cli"
fi

# Check authentication
info "Checking Azure CLI authentication..."
CURRENT_USER=$(az account show --query "user.name" -o tsv 2>/dev/null) || \
    error "Not authenticated. Run 'az login' first."
ok "Authenticated as: $CURRENT_USER"

# Set subscription
info "Setting subscription to $SUBSCRIPTION..."
az account set --subscription "$SUBSCRIPTION"
ok "Subscription set."

# Create output directory
mkdir -p "$OUTPUT_DIR"

# ---------------------------------------------------------------------------
# Run scripts in order
# ---------------------------------------------------------------------------
SCRIPTS=(
    "01-resource-group.sh"
    "02-storage.sh"
    "03-database.sh"
    "04-ai-services.sh"
    "05-app-service.sh"
    "06-static-web-app.sh"
    "07-entra-app-registration.sh"
)

for SCRIPT in "${SCRIPTS[@]}"; do
    header "Running $SCRIPT"
    SCRIPT_PATH="${SCRIPT_DIR}/${SCRIPT}"

    if [ ! -f "$SCRIPT_PATH" ]; then
        warn "Script '$SCRIPT' not found. Skipping."
        continue
    fi

    chmod +x "$SCRIPT_PATH"
    bash "$SCRIPT_PATH"
    ok "Completed: $SCRIPT"
done

# ---------------------------------------------------------------------------
# Collect outputs and write env file
# ---------------------------------------------------------------------------
header "Collecting Resource Information"

info "Gathering connection strings and endpoints..."

# Storage
STORAGE_CONNECTION=$(az storage account show-connection-string \
    --name "$STORAGE_ACCOUNT" --resource-group "$RESOURCE_GROUP" \
    --query connectionString -o tsv 2>/dev/null || echo "N/A")
BLOB_ENDPOINT=$(az storage account show \
    --name "$STORAGE_ACCOUNT" --resource-group "$RESOURCE_GROUP" \
    --query "primaryEndpoints.blob" -o tsv 2>/dev/null || echo "N/A")

# PostgreSQL
PG_FQDN=$(az postgres flexible-server show \
    --name "$PG_SERVER" --resource-group "$RESOURCE_GROUP" \
    --query "fullyQualifiedDomainName" -o tsv 2>/dev/null || echo "N/A")

# Azure OpenAI
AOAI_ENDPOINT=$(az cognitiveservices account show \
    --name "$AOAI_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "properties.endpoint" -o tsv 2>/dev/null || echo "N/A")
AOAI_KEY=$(az cognitiveservices account keys list \
    --name "$AOAI_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "key1" -o tsv 2>/dev/null || echo "N/A")

# AI Search
SEARCH_ENDPOINT="https://${SEARCH_NAME}.search.windows.net"
SEARCH_KEY=$(az search admin-key show \
    --service-name "$SEARCH_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "primaryKey" -o tsv 2>/dev/null || echo "N/A")

# Document Intelligence
DI_ENDPOINT=$(az cognitiveservices account show \
    --name "$DI_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "properties.endpoint" -o tsv 2>/dev/null || echo "N/A")
DI_KEY=$(az cognitiveservices account keys list \
    --name "$DI_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "key1" -o tsv 2>/dev/null || echo "N/A")

# App Service API
APP_API_HOSTNAME=$(az webapp show \
    --name "$APP_API_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "defaultHostName" -o tsv 2>/dev/null || echo "N/A")

# Function App
FUNC_APP_HOSTNAME=$(az functionapp show \
    --name "$FUNC_APP_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "defaultHostName" -o tsv 2>/dev/null || echo "N/A")

# Static Web App
SWA_HOSTNAME=$(az staticwebapp show \
    --name "$SWA_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "defaultHostname" -o tsv 2>/dev/null || echo "N/A")
SWA_DEPLOYMENT_TOKEN=$(az staticwebapp secrets list \
    --name "$SWA_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "properties.apiKey" -o tsv 2>/dev/null || echo "N/A")

# Entra ID
APP_DISPLAY_NAME="Sedin Pre-Sales Asset Manager"
APP_CLIENT_ID=$(az ad app list --display-name "$APP_DISPLAY_NAME" \
    --query "[0].appId" -o tsv 2>/dev/null || echo "N/A")
TENANT_ID=$(az account show --query "tenantId" -o tsv)

# ---------------------------------------------------------------------------
# Write env file
# ---------------------------------------------------------------------------
ENV_FILE="${OUTPUT_DIR}/azure-resources.env"
info "Writing resource info to $ENV_FILE..."

cat > "$ENV_FILE" <<EOF
###############################################################################
# Sedin Pre-Sales Asset Management - Azure Resource Configuration
# Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
# Subscription: $SUBSCRIPTION
# Resource Group: $RESOURCE_GROUP
###############################################################################

# --- Azure Storage ---
AZURE_STORAGE_CONNECTION_STRING="${STORAGE_CONNECTION}"
AZURE_STORAGE_BLOB_ENDPOINT="${BLOB_ENDPOINT}"
AZURE_STORAGE_ACCOUNT_NAME="${STORAGE_ACCOUNT}"

# --- PostgreSQL ---
POSTGRES_HOST="${PG_FQDN}"
POSTGRES_PORT=5432
POSTGRES_DB="${PG_DB}"
POSTGRES_USER="${PG_ADMIN}"
POSTGRES_PASSWORD="<SET_MANUALLY_FROM_SCRIPT_OUTPUT>"
POSTGRES_CONNECTION_STRING="Host=${PG_FQDN};Port=5432;Database=${PG_DB};Username=${PG_ADMIN};Password=<SET_MANUALLY>;SSL Mode=Require;"

# --- Azure OpenAI ---
AZURE_OPENAI_ENDPOINT="${AOAI_ENDPOINT}"
AZURE_OPENAI_KEY="${AOAI_KEY}"
AZURE_OPENAI_CHAT_DEPLOYMENT="gpt-4o-mini"
AZURE_OPENAI_EMBEDDING_DEPLOYMENT="text-embedding-3-small"

# --- Azure AI Search ---
AZURE_SEARCH_ENDPOINT="${SEARCH_ENDPOINT}"
AZURE_SEARCH_KEY="${SEARCH_KEY}"

# --- Azure Document Intelligence ---
AZURE_DI_ENDPOINT="${DI_ENDPOINT}"
AZURE_DI_KEY="${DI_KEY}"

# --- App Service (API) ---
APP_API_URL="https://${APP_API_HOSTNAME}"
APP_API_NAME="${APP_API_NAME}"

# --- Azure Functions ---
FUNC_APP_URL="https://${FUNC_APP_HOSTNAME}"
FUNC_APP_NAME="${FUNC_APP_NAME}"

# --- Static Web App ---
SWA_URL="https://${SWA_HOSTNAME}"
SWA_DEPLOYMENT_TOKEN="${SWA_DEPLOYMENT_TOKEN}"

# --- Entra ID ---
AZURE_AD_CLIENT_ID="${APP_CLIENT_ID}"
AZURE_AD_TENANT_ID="${TENANT_ID}"
AZURE_AD_API_SCOPE="api://sedin-presales/access_as_user"
EOF

ok "Resource info saved to $ENV_FILE"

# ---------------------------------------------------------------------------
# Print summary
# ---------------------------------------------------------------------------
header "Provisioning Complete - Resource Summary"

echo "  Resource Group:       $RESOURCE_GROUP ($LOCATION)"
echo ""
echo "  Storage Account:      $STORAGE_ACCOUNT"
echo "    Blob Endpoint:      $BLOB_ENDPOINT"
echo ""
echo "  PostgreSQL:           $PG_SERVER"
echo "    FQDN:               $PG_FQDN"
echo "    Database:           $PG_DB"
echo ""
echo "  Azure OpenAI:         $AOAI_NAME"
echo "    Endpoint:           $AOAI_ENDPOINT"
echo ""
echo "  AI Search:            $SEARCH_NAME"
echo "    Endpoint:           $SEARCH_ENDPOINT"
echo ""
echo "  Document Intelligence: $DI_NAME"
echo "    Endpoint:           $DI_ENDPOINT"
echo ""
echo "  App Service (API):    https://$APP_API_HOSTNAME"
echo "  Function App:         https://$FUNC_APP_HOSTNAME"
echo ""
echo "  Static Web App:       https://$SWA_HOSTNAME"
echo ""
echo "  Entra ID Client ID:   $APP_CLIENT_ID"
echo "  Tenant ID:            $TENANT_ID"
echo ""
warn "Remember to set the PostgreSQL password in $ENV_FILE manually!"
warn "The password was shown in the 03-database.sh output."
echo ""
ok "All resources provisioned. Configuration saved to: $ENV_FILE"
