#!/usr/bin/env bash
###############################################################################
# 04-ai-services.sh
# Creates Azure OpenAI, AI Search, and Document Intelligence for Sedin Pre-Sales
###############################################################################
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
RESOURCE_GROUP="rg-sedin-presales"
LOCATION="eastus"

# Azure OpenAI
AOAI_NAME="aoai-sedin-presales"
AOAI_SKU="S0"
AOAI_MODEL_CHAT="gpt-4o-mini"
AOAI_DEPLOYMENT_CHAT="gpt-4o-mini"
AOAI_MODEL_EMBED="text-embedding-3-small"
AOAI_DEPLOYMENT_EMBED="text-embedding-3-small"

# Azure AI Search
SEARCH_NAME="search-sedin-presales"
SEARCH_SKU="basic"
SEARCH_REPLICAS=1
SEARCH_PARTITIONS=1

# Azure Document Intelligence
DI_NAME="di-sedin-presales"
DI_SKU="S0"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }

###############################################################################
# Azure OpenAI
###############################################################################
info "--- Azure OpenAI ---"

info "Checking if Azure OpenAI account '$AOAI_NAME' exists..."
if az cognitiveservices account show \
    --name "$AOAI_NAME" \
    --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "Azure OpenAI account '$AOAI_NAME' already exists. Skipping creation."
else
    info "Creating Azure OpenAI account '$AOAI_NAME'..."
    az cognitiveservices account create \
        --name "$AOAI_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$LOCATION" \
        --kind "OpenAI" \
        --sku "$AOAI_SKU" \
        --custom-domain "$AOAI_NAME" \
        --tags project=sedin-presales environment=dev
    ok "Azure OpenAI account '$AOAI_NAME' created."
fi

# Deploy chat model
info "Checking if deployment '$AOAI_DEPLOYMENT_CHAT' exists..."
if az cognitiveservices account deployment show \
    --name "$AOAI_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --deployment-name "$AOAI_DEPLOYMENT_CHAT" &>/dev/null; then
    warn "Deployment '$AOAI_DEPLOYMENT_CHAT' already exists. Skipping."
else
    info "Deploying model '$AOAI_MODEL_CHAT' as '$AOAI_DEPLOYMENT_CHAT'..."
    az cognitiveservices account deployment create \
        --name "$AOAI_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --deployment-name "$AOAI_DEPLOYMENT_CHAT" \
        --model-name "$AOAI_MODEL_CHAT" \
        --model-version "2024-07-18" \
        --model-format "OpenAI" \
        --sku-capacity 30 \
        --sku-name "GlobalStandard"
    ok "Model '$AOAI_MODEL_CHAT' deployed."
fi

# Deploy embedding model
info "Checking if deployment '$AOAI_DEPLOYMENT_EMBED' exists..."
if az cognitiveservices account deployment show \
    --name "$AOAI_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --deployment-name "$AOAI_DEPLOYMENT_EMBED" &>/dev/null; then
    warn "Deployment '$AOAI_DEPLOYMENT_EMBED' already exists. Skipping."
else
    info "Deploying model '$AOAI_MODEL_EMBED' as '$AOAI_DEPLOYMENT_EMBED'..."
    az cognitiveservices account deployment create \
        --name "$AOAI_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --deployment-name "$AOAI_DEPLOYMENT_EMBED" \
        --model-name "$AOAI_MODEL_EMBED" \
        --model-version "1" \
        --model-format "OpenAI" \
        --sku-capacity 30 \
        --sku-name "GlobalStandard"
    ok "Model '$AOAI_MODEL_EMBED' deployed."
fi

AOAI_ENDPOINT=$(az cognitiveservices account show \
    --name "$AOAI_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query "properties.endpoint" -o tsv)

AOAI_KEY=$(az cognitiveservices account keys list \
    --name "$AOAI_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query "key1" -o tsv)

###############################################################################
# Azure AI Search
###############################################################################
info ""
info "--- Azure AI Search ---"

info "Checking if Azure AI Search '$SEARCH_NAME' exists..."
if az search service show \
    --name "$SEARCH_NAME" \
    --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "Azure AI Search '$SEARCH_NAME' already exists. Skipping creation."
else
    info "Creating Azure AI Search '$SEARCH_NAME' (SKU: $SEARCH_SKU)..."
    az search service create \
        --name "$SEARCH_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$LOCATION" \
        --sku "$SEARCH_SKU" \
        --replica-count "$SEARCH_REPLICAS" \
        --partition-count "$SEARCH_PARTITIONS" \
        --tags project=sedin-presales environment=dev
    ok "Azure AI Search '$SEARCH_NAME' created."
fi

SEARCH_ENDPOINT="https://${SEARCH_NAME}.search.windows.net"
SEARCH_KEY=$(az search admin-key show \
    --service-name "$SEARCH_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query "primaryKey" -o tsv)

###############################################################################
# Azure Document Intelligence
###############################################################################
info ""
info "--- Azure Document Intelligence ---"

info "Checking if Document Intelligence '$DI_NAME' exists..."
if az cognitiveservices account show \
    --name "$DI_NAME" \
    --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "Document Intelligence '$DI_NAME' already exists. Skipping creation."
else
    info "Creating Document Intelligence '$DI_NAME'..."
    az cognitiveservices account create \
        --name "$DI_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$LOCATION" \
        --kind "FormRecognizer" \
        --sku "$DI_SKU" \
        --custom-domain "$DI_NAME" \
        --tags project=sedin-presales environment=dev
    ok "Document Intelligence '$DI_NAME' created."
fi

DI_ENDPOINT=$(az cognitiveservices account show \
    --name "$DI_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query "properties.endpoint" -o tsv)

DI_KEY=$(az cognitiveservices account keys list \
    --name "$DI_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query "key1" -o tsv)

###############################################################################
# Summary
###############################################################################
echo ""
ok "Done - AI Services provisioning complete."
echo ""
echo "  Azure OpenAI:"
echo "    Endpoint:          $AOAI_ENDPOINT"
echo "    Key:               $AOAI_KEY"
echo "    Chat Deployment:   $AOAI_DEPLOYMENT_CHAT"
echo "    Embed Deployment:  $AOAI_DEPLOYMENT_EMBED"
echo ""
echo "  Azure AI Search:"
echo "    Endpoint:          $SEARCH_ENDPOINT"
echo "    Admin Key:         $SEARCH_KEY"
echo ""
echo "  Document Intelligence:"
echo "    Endpoint:          $DI_ENDPOINT"
echo "    Key:               $DI_KEY"
