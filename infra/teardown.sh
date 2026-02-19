#!/usr/bin/env bash
###############################################################################
# teardown.sh
# Deletes the entire Sedin Pre-Sales resource group (with confirmation)
###############################################################################
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
RESOURCE_GROUP="rg-sedin-presales"
SUBSCRIPTION="d12e5944-0643-477a-a803-fe75f60af762"
APP_DISPLAY_NAME="Sedin Pre-Sales Asset Manager"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }

# ---------------------------------------------------------------------------
# Pre-flight
# ---------------------------------------------------------------------------
info "Setting subscription to $SUBSCRIPTION..."
az account set --subscription "$SUBSCRIPTION"

# Check if resource group exists
if ! az group show --name "$RESOURCE_GROUP" &>/dev/null; then
    warn "Resource group '$RESOURCE_GROUP' does not exist. Nothing to tear down."
    exit 0
fi

# ---------------------------------------------------------------------------
# List resources
# ---------------------------------------------------------------------------
info "Resources in '$RESOURCE_GROUP':"
az resource list \
    --resource-group "$RESOURCE_GROUP" \
    --query "[].{Name:name, Type:type}" \
    --output table

echo ""

# ---------------------------------------------------------------------------
# Confirmation prompt
# ---------------------------------------------------------------------------
warn "WARNING: This will permanently delete the resource group '$RESOURCE_GROUP'"
warn "and ALL resources within it. This action CANNOT be undone."
echo ""
read -rp "Type the resource group name to confirm deletion [$RESOURCE_GROUP]: " CONFIRM

if [ "$CONFIRM" != "$RESOURCE_GROUP" ]; then
    info "Confirmation did not match. Aborting teardown."
    exit 1
fi

# ---------------------------------------------------------------------------
# Delete Entra ID app registration (not in resource group)
# ---------------------------------------------------------------------------
info "Checking for Entra ID app registration '$APP_DISPLAY_NAME'..."
APP_ID=$(az ad app list --display-name "$APP_DISPLAY_NAME" \
    --query "[0].appId" -o tsv 2>/dev/null || echo "")

if [ -n "$APP_ID" ] && [ "$APP_ID" != "None" ]; then
    info "Deleting app registration (Client ID: $APP_ID)..."
    az ad app delete --id "$APP_ID"
    ok "App registration deleted."
else
    info "No app registration found. Skipping."
fi

# ---------------------------------------------------------------------------
# Delete resource group
# ---------------------------------------------------------------------------
info "Deleting resource group '$RESOURCE_GROUP'..."
info "This may take several minutes..."

az group delete \
    --name "$RESOURCE_GROUP" \
    --yes \
    --no-wait

ok "Resource group deletion initiated (running in background)."
info "Use 'az group show --name $RESOURCE_GROUP' to check status."

# ---------------------------------------------------------------------------
# Clean up local output
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_FILE="${SCRIPT_DIR}/output/azure-resources.env"
if [ -f "$OUTPUT_FILE" ]; then
    info "Removing local output file: $OUTPUT_FILE"
    rm -f "$OUTPUT_FILE"
    ok "Local output file removed."
fi

echo ""
ok "Teardown complete. Resource group '$RESOURCE_GROUP' is being deleted."
