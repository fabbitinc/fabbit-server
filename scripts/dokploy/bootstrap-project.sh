#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/dokploy/bootstrap-project.sh project

Commands:
  project   Create the Dokploy project via CLI

Optional environment variables:
  DOKPLOY_PROJECT_NAME         Default: fabbit-server
  DOKPLOY_PROJECT_DESCRIPTION  Default: Fabbit production

Notes:
  - This script is for one-time bootstrap only.
  - It assumes `dokploy authenticate` has already been completed locally.
  - Deployment itself is expected to be triggered by Dokploy GitHub integration
    with branch=release, not by CLI.
  - Dokploy CLI v0.2.8 can create projects, but `app create` currently fails because
    the server requires `environmentId` and the CLI does not send it.
  - Create the application in the Dokploy UI after creating the project.
USAGE
}

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    exit 1
  fi
}

require_command() {
  if ! command -v dokploy >/dev/null 2>&1; then
    echo "Missing required command: dokploy" >&2
    echo "Install/authenticate Dokploy CLI first, then retry." >&2
    exit 1
  fi
}

create_project() {
  local project_name="${DOKPLOY_PROJECT_NAME:-fabbit-server}"
  local project_description="${DOKPLOY_PROJECT_DESCRIPTION:-Fabbit production}"

  require_command

  dokploy project create \
    --name "${project_name}" \
    --description "${project_description}" \
    --skipConfirm

  cat <<EOF

Project creation requested.
Next steps:
  1. Run: dokploy project list
  2. Open Dokploy UI and create the app inside this project
  3. Configure:
     - Git provider: GitHub
     - Repository: fabbitinc/fabbit-server
     - Branch: release
     - Auto deploy: enabled
     - Port: 10010
     - Health check: /health
EOF
}

main() {
  local command="${1:-}"

  case "${command}" in
    project)
      create_project
      ;;
    -h|--help|help|"")
      usage
      ;;
    *)
      echo "Unknown command: ${command}" >&2
      usage
      exit 1
      ;;
  esac
}

main "$@"
