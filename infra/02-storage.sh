#!/usr/bin/env bash
###############################################################################
# 02-storage.sh
# Creates Azure Storage Account with containers and CORS for Sedin Pre-Sales
###############################################################################
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
RESOURCE_GROUP="rg-sedin-presales"
LOCATION="eastus"
STORAGE_ACCOUNT="stsedinpresales"
SKU="Standard_LRS"
KIND="StorageV2"
ACCESS_TIER="Hot"
SOFT_DELETE_DAYS=30
CONTAINERS=("originals" "renditions" "summaries" "templates" "temp")
CORS_ORIGINS="http://localhost:5173,http://localhost:5072"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }

# ---------------------------------------------------------------------------
# Storage Account
# ---------------------------------------------------------------------------
info "Checking if storage account '$STORAGE_ACCOUNT' already exists..."
if az storage account show --name "$STORAGE_ACCOUNT" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "Storage account '$STORAGE_ACCOUNT' already exists. Skipping creation."
else
    info "Creating storage account '$STORAGE_ACCOUNT'..."
    az storage account create \
        --name "$STORAGE_ACCOUNT" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$LOCATION" \
        --sku "$SKU" \
        --kind "$KIND" \
        --access-tier "$ACCESS_TIER" \
        --min-tls-version TLS1_2 \
        --allow-blob-public-access false \
        --tags project=sedin-presales environment=dev
    ok "Storage account '$STORAGE_ACCOUNT' created."
fi

# ---------------------------------------------------------------------------
# Retrieve account key
# ---------------------------------------------------------------------------
info "Retrieving storage account key..."
STORAGE_KEY=$(az storage account keys list \
    --account-name "$STORAGE_ACCOUNT" \
    --resource-group "$RESOURCE_GROUP" \
    --query "[0].value" -o tsv)

# ---------------------------------------------------------------------------
# Enable blob versioning
# ---------------------------------------------------------------------------
info "Enabling blob versioning..."
az storage account blob-service-properties update \
    --account-name "$STORAGE_ACCOUNT" \
    --resource-group "$RESOURCE_GROUP" \
    --enable-versioning true \
    --output none
ok "Blob versioning enabled."

# ---------------------------------------------------------------------------
# Enable soft delete (blobs and containers)
# ---------------------------------------------------------------------------
info "Enabling soft delete (${SOFT_DELETE_DAYS} days)..."
az storage account blob-service-properties update \
    --account-name "$STORAGE_ACCOUNT" \
    --resource-group "$RESOURCE_GROUP" \
    --enable-delete-retention true \
    --delete-retention-days "$SOFT_DELETE_DAYS" \
    --enable-container-delete-retention true \
    --container-delete-retention-days "$SOFT_DELETE_DAYS" \
    --output none
ok "Soft delete enabled for ${SOFT_DELETE_DAYS} days."

# ---------------------------------------------------------------------------
# Create containers
# ---------------------------------------------------------------------------
for CONTAINER in "${CONTAINERS[@]}"; do
    info "Checking container '$CONTAINER'..."
    if az storage container show \
        --name "$CONTAINER" \
        --account-name "$STORAGE_ACCOUNT" \
        --account-key "$STORAGE_KEY" &>/dev/null; then
        warn "Container '$CONTAINER' already exists. Skipping."
    else
        info "Creating container '$CONTAINER'..."
        az storage container create \
            --name "$CONTAINER" \
            --account-name "$STORAGE_ACCOUNT" \
            --account-key "$STORAGE_KEY" \
            --output none
        ok "Container '$CONTAINER' created."
    fi
done

# ---------------------------------------------------------------------------
# Configure CORS
# ---------------------------------------------------------------------------
info "Configuring CORS rules for blob service..."
az storage cors clear \
    --account-name "$STORAGE_ACCOUNT" \
    --account-key "$STORAGE_KEY" \
    --services b

az storage cors add \
    --account-name "$STORAGE_ACCOUNT" \
    --account-key "$STORAGE_KEY" \
    --services b \
    --methods GET HEAD PUT POST DELETE OPTIONS \
    --origins "$CORS_ORIGINS" \
    --allowed-headers "*" \
    --exposed-headers "*" \
    --max-age 3600
ok "CORS configured for: $CORS_ORIGINS"

# ---------------------------------------------------------------------------
# Output connection string
# ---------------------------------------------------------------------------
CONNECTION_STRING=$(az storage account show-connection-string \
    --name "$STORAGE_ACCOUNT" \
    --resource-group "$RESOURCE_GROUP" \
    --query connectionString -o tsv)

BLOB_ENDPOINT=$(az storage account show \
    --name "$STORAGE_ACCOUNT" \
    --resource-group "$RESOURCE_GROUP" \
    --query "primaryEndpoints.blob" -o tsv)

echo ""
ok "Done - Storage provisioning complete."
echo "  Storage Account:     $STORAGE_ACCOUNT"
echo "  Blob Endpoint:       $BLOB_ENDPOINT"
echo "  Connection String:   $CONNECTION_STRING"
