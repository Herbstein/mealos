mod support;
use forkware_backend::apple::verify_with_keys;
use serde_json::json;
use support::{AUDIENCE, Signing};
#[test]
fn validates_signature_and_all_identity_claims() {
    let signer = Signing::new();
    let claims = signer.claims("user", "expected");
    assert_eq!(
        verify_with_keys(&signer.sign(&claims), "expected", AUDIENCE, &signer.keys).unwrap(),
        "user"
    );
    for (key, value) in [
        ("aud", json!("another-app")),
        ("iss", json!("https://attacker.example")),
        ("nonce", json!("wrong")),
        ("exp", json!(1)),
        ("iat", json!(1)),
        ("iat", json!(chrono::Utc::now().timestamp() + 1000)),
        ("sub", json!("")),
    ] {
        let mut bad = claims.clone();
        bad[key] = value;
        assert!(
            verify_with_keys(&signer.sign(&bad), "expected", AUDIENCE, &signer.keys).is_err(),
            "{key}"
        );
    }
    for key in ["exp", "iat", "iss", "aud", "sub", "nonce"] {
        let mut bad = claims.clone();
        bad.as_object_mut().unwrap().remove(key);
        assert!(
            verify_with_keys(&signer.sign(&bad), "expected", AUDIENCE, &signer.keys).is_err(),
            "missing {key}"
        );
    }
    let forged = Signing::new();
    assert!(verify_with_keys(&forged.sign(&claims), "expected", AUDIENCE, &signer.keys).is_err());
    assert!(
        verify_with_keys(
            "eyJhbGciOiJub25lIn0.e30.",
            "expected",
            AUDIENCE,
            &signer.keys
        )
        .is_err()
    );
}
