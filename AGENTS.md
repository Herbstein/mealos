# Browser access

Do not control Chrome or Safari, or request permission to control them. Use CLI/API
integrations where available. If a task requires browser-only steps, give the user
clear instructions and let them perform those steps themselves.

# Backend deployment

Production `forkware-api` deploys from `kanki6315/mealos` on `main`, with service
root `/backend`. Use a pull request and merge for code deployments; do not upload
local working trees to Railway unless the user explicitly requests that exception.
Preserve the separate legacy `mealos` assistant service. See `backend/README.md`.
