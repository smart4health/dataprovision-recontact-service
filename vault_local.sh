#!/bin/bash
## From a new terminal set the env vars to access vault as root
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=root

## Enable approle authentication and
vault auth enable approle

## Add policy for recontact to authenticate and pull secrets
vault policy write app - << EOF
path "secret/data/recontact/*" {
  capabilities = [ "read" ]
}
EOF

vault policy write deployment - << EOF
path "auth/approle/role/recontact/*" {
  capabilities = [ "read", "update" ]
}
EOF

## Add a role for recontact using this policy with a short TTL
vault write auth/approle/role/recontact token_policies=app token_ttl=30s

## Add the secrets so recontact doesn't crash on startup
vault kv put secret/recontact/jira/encryption shared-cohort-key="0000000000000000000000000000000000000000000000000000000000000000"
vault kv put secret/recontact/jira/credentials username="username" password="password"
vault kv put secret/recontact/rds-credentials url="postgresql://localhost:5432/development?user=username&password=password"

## Create a initial deployment token
vault token create -field token -policy=deployment
