import { defineRailway, postgres, project, service } from "railway/iac";

// Bootstrap a new staging environment. For the existing production project,
// import its configuration first and merge these settings (see ../README.md).
export const partial = "forkware-backend";

export default defineRailway(() => {
  const db = postgres("forkware-postgres");
  const api = service("forkware-api", {
    root: "/backend",
    build: { builder: "DOCKERFILE", dockerfilePath: "Dockerfile" },
    start: "forkware-backend serve",
    preDeploy: "forkware-backend migrate",
    healthcheck: "/ready",
    healthcheckTimeout: 30,
    deploy: {
      sleepApplication: true,
      restartPolicyType: "ON_FAILURE",
      restartPolicyMaxRetries: 3,
      drainingSeconds: "40"
    },
    env: {
      RUST_LOG: "info",
      APPLE_CLIENT_ID: "com.arjunakankipati.forkware",
      DATABASE_URL: db.env.DATABASE_URL
    }
  });
  return project("forkware", { resources: [db, api] });
});
