#!/bin/bash
set -euo pipefail

bash "$(dirname "$0")/deploy/prod/prod-deploy.sh"
