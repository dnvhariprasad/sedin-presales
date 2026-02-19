#!/usr/bin/env bash
###############################################################################
# 06-static-web-app.sh
# Creates Azure Static Web App for Sedin Pre-Sales frontend
###############################################################################
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
RESOURCE_GROUP="rg-sedin-presales"
SWA_LOCATION="eastus2"   # SWA has limited region support
SWA_NAME="swa-sedin-presales"
SWA_SKU="Free"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }

###############################################################################
# Static Web App
###############################################################################
info "Checking if Static Web App '$SWA_NAME' exists..."
if az staticwebapp show \
    --name "$SWA_NAME" \
    --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "Static Web App '$SWA_NAME' already exists. Skipping creation."
else
    info "Creating Static Web App '$SWA_NAME' in '$SWA_LOCATION' (SKU: $SWA_SKU)..."
    az staticwebapp create \
        --name "$SWA_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$SWA_LOCATION" \
        --sku "$SWA_SKU" \
        --tags project=sedin-presales environment=dev
    ok "Static Web App '$SWA_NAME' created."
fi

SWA_HOSTNAME=$(az staticwebapp show \
    --name "$SWA_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query "defaultHostname" -o tsv)

SWA_DEPLOYMENT_TOKEN=$(az staticwebapp secrets list \
    --name "$SWA_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query "properties.apiKey" -o tsv 2>/dev/null || echo "N/A")

###############################################################################
# Summary
###############################################################################
echo ""
ok "Done - Static Web App provisioning complete."
echo ""
echo "  Name:                $SWA_NAME"
echo "  URL:                 https://$SWA_HOSTNAME"
echo "  Deployment Token:    $SWA_DEPLOYMENT_TOKEN"
