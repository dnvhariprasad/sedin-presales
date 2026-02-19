#!/usr/bin/env bash
###############################################################################
# 05-app-service.sh
# Creates App Service and Azure Functions infrastructure for Sedin Pre-Sales
# (Java 21 / Spring Boot - direct JAR deployment, no Docker)
###############################################################################
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
RESOURCE_GROUP="rg-sedin-presales"
LOCATION="centralus"  # eastus has zero VM quota

ASP_NAME="asp-sedin-presales"
ASP_SKU="B1"

APP_API_NAME="app-sedin-presales-api"

FUNC_APP_NAME="func-sedin-presales"
FUNC_STORAGE_ACCOUNT="stsedinpresalesfunc"
FUNC_PLAN_NAME="asp-sedin-presales-func"

# Resource names for app settings (must match other infra scripts)
STORAGE_ACCOUNT="stsedinpresales"
PG_SERVER="psql-sedin-presales"
PG_DB="presalesdb"
PG_ADMIN="presalesadmin"
AOAI_NAME="aoai-sedin-presales"
SEARCH_NAME="search-sedin-presales"
DI_NAME="di-sedin-presales"
SWA_NAME="swa-sedin-presales"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }

###############################################################################
# App Service Plan
###############################################################################
info "--- App Service Plan ---"

info "Checking if App Service Plan '$ASP_NAME' exists..."
if az appservice plan show --name "$ASP_NAME" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "App Service Plan '$ASP_NAME' already exists. Skipping creation."
else
    info "Creating App Service Plan '$ASP_NAME' (SKU: $ASP_SKU, Linux, Java 21)..."
    az appservice plan create \
        --name "$ASP_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$LOCATION" \
        --sku "$ASP_SKU" \
        --is-linux \
        --tags project=sedin-presales environment=dev
    ok "App Service Plan '$ASP_NAME' created."
fi

###############################################################################
# App Service: API (Spring Boot)
###############################################################################
info ""
info "--- App Service: API ---"

info "Checking if App Service '$APP_API_NAME' exists..."
if az webapp show --name "$APP_API_NAME" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "App Service '$APP_API_NAME' already exists. Skipping creation."
else
    info "Creating App Service '$APP_API_NAME' (Java 21, Spring Boot)..."
    az webapp create \
        --name "$APP_API_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --plan "$ASP_NAME" \
        --runtime "JAVA:21-java21" \
        --tags project=sedin-presales environment=dev
    ok "App Service '$APP_API_NAME' created."
fi

# Gather connection info for app settings
info "Gathering resource endpoints for app settings..."

STORAGE_CONNECTION=$(az storage account show-connection-string \
    --name "$STORAGE_ACCOUNT" --resource-group "$RESOURCE_GROUP" \
    --query connectionString -o tsv 2>/dev/null || echo "")

PG_FQDN=$(az postgres flexible-server show \
    --name "$PG_SERVER" --resource-group "$RESOURCE_GROUP" \
    --query "fullyQualifiedDomainName" -o tsv 2>/dev/null || echo "")

AOAI_ENDPOINT=$(az cognitiveservices account show \
    --name "$AOAI_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "properties.endpoint" -o tsv 2>/dev/null || echo "")

AOAI_KEY=$(az cognitiveservices account keys list \
    --name "$AOAI_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "key1" -o tsv 2>/dev/null || echo "")

SEARCH_ENDPOINT="https://${SEARCH_NAME}.search.windows.net"
SEARCH_KEY=$(az search admin-key show \
    --service-name "$SEARCH_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "primaryKey" -o tsv 2>/dev/null || echo "")

DI_ENDPOINT=$(az cognitiveservices account show \
    --name "$DI_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "properties.endpoint" -o tsv 2>/dev/null || echo "")

DI_KEY=$(az cognitiveservices account keys list \
    --name "$DI_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "key1" -o tsv 2>/dev/null || echo "")

SWA_HOSTNAME=$(az staticwebapp show \
    --name "$SWA_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "defaultHostname" -o tsv 2>/dev/null || echo "")

# Configure application settings
info "Configuring App Service application settings..."
az webapp config appsettings set \
    --name "$APP_API_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --settings \
        SPRING_DATASOURCE_URL="jdbc:postgresql://${PG_FQDN}:5432/${PG_DB}?sslmode=require" \
        SPRING_DATASOURCE_USERNAME="$PG_ADMIN" \
        AZURE_STORAGE_CONNECTION_STRING="$STORAGE_CONNECTION" \
        AZURE_OPENAI_ENDPOINT="$AOAI_ENDPOINT" \
        AZURE_OPENAI_KEY="$AOAI_KEY" \
        AZURE_SEARCH_ENDPOINT="$SEARCH_ENDPOINT" \
        AZURE_SEARCH_KEY="$SEARCH_KEY" \
        AZURE_DI_ENDPOINT="$DI_ENDPOINT" \
        AZURE_DI_KEY="$DI_KEY" \
    --output none
ok "App settings configured."

# Configure CORS
info "Configuring CORS for frontend access..."
CORS_ORIGINS="https://${SWA_HOSTNAME}"
if [ -n "$SWA_HOSTNAME" ] && [ "$SWA_HOSTNAME" != "" ]; then
    az webapp cors add \
        --name "$APP_API_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --allowed-origins "$CORS_ORIGINS" "http://localhost:5173" \
        --output none
    ok "CORS configured for $CORS_ORIGINS and http://localhost:5173."
else
    warn "SWA hostname not available. Configuring localhost CORS only."
    az webapp cors add \
        --name "$APP_API_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --allowed-origins "http://localhost:5173" \
        --output none
fi

###############################################################################
# Function App Storage Account
###############################################################################
info ""
info "--- Function App Storage Account ---"

info "Checking if Storage Account '$FUNC_STORAGE_ACCOUNT' exists..."
if az storage account show --name "$FUNC_STORAGE_ACCOUNT" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "Storage Account '$FUNC_STORAGE_ACCOUNT' already exists. Skipping creation."
else
    info "Creating Storage Account '$FUNC_STORAGE_ACCOUNT'..."
    az storage account create \
        --name "$FUNC_STORAGE_ACCOUNT" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$LOCATION" \
        --sku Standard_LRS \
        --tags project=sedin-presales environment=dev
    ok "Storage Account '$FUNC_STORAGE_ACCOUNT' created."
fi

###############################################################################
# Azure Functions App (Java 21, Consumption plan)
###############################################################################
info ""
info "--- Azure Functions App ---"

info "Checking if Function App '$FUNC_APP_NAME' exists..."
if az functionapp show --name "$FUNC_APP_NAME" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "Function App '$FUNC_APP_NAME' already exists. Skipping creation."
else
    info "Creating Function App '$FUNC_APP_NAME' (Java 21, Consumption plan)..."
    az functionapp create \
        --name "$FUNC_APP_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --storage-account "$FUNC_STORAGE_ACCOUNT" \
        --consumption-plan-location "$LOCATION" \
        --runtime java \
        --runtime-version 21 \
        --functions-version 4 \
        --os-type Linux \
        --tags project=sedin-presales environment=dev
    ok "Function App '$FUNC_APP_NAME' created."
fi

# Configure Function App settings
info "Configuring Function App application settings..."
az functionapp config appsettings set \
    --name "$FUNC_APP_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --settings \
        SPRING_DATASOURCE_URL="jdbc:postgresql://${PG_FQDN}:5432/${PG_DB}?sslmode=require" \
        SPRING_DATASOURCE_USERNAME="$PG_ADMIN" \
        AZURE_STORAGE_CONNECTION_STRING="$STORAGE_CONNECTION" \
        AZURE_OPENAI_ENDPOINT="$AOAI_ENDPOINT" \
        AZURE_OPENAI_KEY="$AOAI_KEY" \
        AZURE_SEARCH_ENDPOINT="$SEARCH_ENDPOINT" \
        AZURE_SEARCH_KEY="$SEARCH_KEY" \
        AZURE_DI_ENDPOINT="$DI_ENDPOINT" \
        AZURE_DI_KEY="$DI_KEY" \
    --output none
ok "Function App settings configured."

###############################################################################
# Summary
###############################################################################
APP_API_HOSTNAME=$(az webapp show \
    --name "$APP_API_NAME" --resource-group "$RESOURCE_GROUP" \
    --query "defaultHostName" -o tsv 2>/dev/null || echo "N/A")

echo ""
ok "Done - App Service & Functions provisioning complete."
echo ""
echo "  App Service Plan:     $ASP_NAME ($ASP_SKU, Linux)"
echo "  API App:              $APP_API_NAME"
echo "  API URL:              https://${APP_API_HOSTNAME}"
echo "  Function App:         $FUNC_APP_NAME"
echo "  Function Storage:     $FUNC_STORAGE_ACCOUNT"
