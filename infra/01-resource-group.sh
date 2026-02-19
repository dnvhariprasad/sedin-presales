#!/usr/bin/env bash
###############################################################################
# 01-resource-group.sh
# Creates the Azure Resource Group for Sedin Pre-Sales Asset Management System
###############################################################################
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
RESOURCE_GROUP="rg-sedin-presales"
LOCATION="eastus"
SUBSCRIPTION="d12e5944-0643-477a-a803-fe75f60af762"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
info "Setting subscription to $SUBSCRIPTION"
az account set --subscription "$SUBSCRIPTION"

info "Checking if resource group '$RESOURCE_GROUP' already exists..."
if az group show --name "$RESOURCE_GROUP" &>/dev/null; then
    warn "Resource group '$RESOURCE_GROUP' already exists. Skipping creation."
else
    info "Creating resource group '$RESOURCE_GROUP' in '$LOCATION'..."
    az group create \
        --name "$RESOURCE_GROUP" \
        --location "$LOCATION" \
        --tags project=sedin-presales environment=dev managed-by=cli
    ok "Resource group '$RESOURCE_GROUP' created."
fi

ok "Done - Resource Group provisioning complete."
