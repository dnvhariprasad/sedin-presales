#!/usr/bin/env bash
###############################################################################
# 03-database.sh
# Creates Azure PostgreSQL Flexible Server for Sedin Pre-Sales
###############################################################################
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
RESOURCE_GROUP="rg-sedin-presales"
LOCATION="eastus"
SERVER_NAME="psql-sedin-presales"
SKU="Standard_B1ms"
TIER="Burstable"
STORAGE_SIZE=32
PG_VERSION="16"
ADMIN_USER="presalesadmin"
DB_NAME="presalesdb"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; exit 1; }

# ---------------------------------------------------------------------------
# Generate random password
# ---------------------------------------------------------------------------
generate_password() {
    # 24-char password: upper, lower, digits, special chars
    local pw
    pw=$(openssl rand -base64 32 | tr -dc 'A-Za-z0-9!@#$%^&*' | head -c 24)
    # Ensure at least one of each category
    pw="${pw:0:20}A1a!"
    echo "$pw"
}

# ---------------------------------------------------------------------------
# PostgreSQL Flexible Server
# ---------------------------------------------------------------------------
info "Checking if PostgreSQL server '$SERVER_NAME' already exists..."
if az postgres flexible-server show \
    --name "$SERVER_NAME" \
    --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    warn "PostgreSQL server '$SERVER_NAME' already exists. Skipping creation."
    ADMIN_PASSWORD="<already-exists-check-key-vault>"
else
    ADMIN_PASSWORD=$(generate_password)
    info "Creating PostgreSQL Flexible Server '$SERVER_NAME'..."
    info "  SKU: $SKU ($TIER), Storage: ${STORAGE_SIZE}GB, Version: $PG_VERSION"

    az postgres flexible-server create \
        --name "$SERVER_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$LOCATION" \
        --admin-user "$ADMIN_USER" \
        --admin-password "$ADMIN_PASSWORD" \
        --sku-name "$SKU" \
        --tier "$TIER" \
        --storage-size "$STORAGE_SIZE" \
        --version "$PG_VERSION" \
        --public-access "None" \
        --tags project=sedin-presales environment=dev

    ok "PostgreSQL server '$SERVER_NAME' created."
fi

# ---------------------------------------------------------------------------
# Firewall: allow current client IP
# ---------------------------------------------------------------------------
info "Detecting current public IP address..."
MY_IP=$(curl -s https://api.ipify.org)
if [ -z "$MY_IP" ]; then
    warn "Could not detect public IP. Skipping client IP firewall rule."
else
    info "Adding firewall rule for current client IP ($MY_IP)..."
    az postgres flexible-server firewall-rule create \
        --name "$SERVER_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --rule-name "AllowClientIP" \
        --start-ip-address "$MY_IP" \
        --end-ip-address "$MY_IP" \
        2>/dev/null || warn "Firewall rule 'AllowClientIP' may already exist."
    ok "Firewall rule added for $MY_IP."
fi

# ---------------------------------------------------------------------------
# Firewall: allow Azure services
# ---------------------------------------------------------------------------
info "Adding firewall rule to allow Azure services..."
az postgres flexible-server firewall-rule create \
    --name "$SERVER_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --rule-name "AllowAzureServices" \
    --start-ip-address "0.0.0.0" \
    --end-ip-address "0.0.0.0" \
    2>/dev/null || warn "Firewall rule 'AllowAzureServices' may already exist."
ok "Azure services firewall rule added."

# ---------------------------------------------------------------------------
# Create database
# ---------------------------------------------------------------------------
info "Checking if database '$DB_NAME' exists..."
DB_EXISTS=$(az postgres flexible-server db show \
    --server-name "$SERVER_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --database-name "$DB_NAME" 2>/dev/null && echo "yes" || echo "no")

if [ "$DB_EXISTS" = "yes" ]; then
    warn "Database '$DB_NAME' already exists. Skipping."
else
    info "Creating database '$DB_NAME'..."
    az postgres flexible-server db create \
        --server-name "$SERVER_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --database-name "$DB_NAME"
    ok "Database '$DB_NAME' created."
fi

# ---------------------------------------------------------------------------
# Enable extensions: pgvector and uuid-ossp
# ---------------------------------------------------------------------------
info "Enabling 'pgvector' extension on the server..."
az postgres flexible-server parameter set \
    --server-name "$SERVER_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --name azure.extensions \
    --value "VECTOR,UUID-OSSP" \
    --output none
ok "Extensions pgvector and uuid-ossp enabled."

# ---------------------------------------------------------------------------
# Output connection info
# ---------------------------------------------------------------------------
FQDN=$(az postgres flexible-server show \
    --name "$SERVER_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --query "fullyQualifiedDomainName" -o tsv)

CONNECTION_STRING="Host=${FQDN};Port=5432;Database=${DB_NAME};Username=${ADMIN_USER};Password=${ADMIN_PASSWORD};SSL Mode=Require;"

echo ""
ok "Done - PostgreSQL provisioning complete."
echo "  Server:              $SERVER_NAME"
echo "  FQDN:                $FQDN"
echo "  Admin User:          $ADMIN_USER"
echo "  Admin Password:      $ADMIN_PASSWORD"
echo "  Database:            $DB_NAME"
echo "  Connection String:   $CONNECTION_STRING"
echo ""
warn "SAVE THE PASSWORD SECURELY. It will not be shown again."
