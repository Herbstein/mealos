import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";

export async function verifyFirebaseIdToken(token) {
  const projectId = process.env.FIREBASE_PROJECT_ID;
  if (!projectId) throw new Error("FIREBASE_PROJECT_ID is required.");
  if (getApps().length === 0) initializeApp({ projectId });
  return getAuth().verifyIdToken(token);
}
