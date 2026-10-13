# QA Failure Triage Agent — Test Strategy

## Objective

Validate the path from automated API failures to structured evidence, hybrid triage suggestions, and saved human QA reviews. Keep deterministic tests independent of local model availability and clearly separate implemented functionality from future coverage.

## Implemented Scope

| Area | Current implementation and validation |
| --- | --- |
| API automation | JUnit 5, REST Assured, and AssertJ tests against Automation Exercise: product listing, product search, missing search parameter rejection, and unsupported method handling. |
| Failure evidence | Registered JUnit extension and REST Assured filter capture test identity, exception details, the latest HTTP exchange, timing, and response body truncation at 3,000 characters. JSON is written under `target/failure-evidence/`. |
| Deterministic pre-triage | Conservative rules for explicit missing input, clearly incorrect expectations with successful responses, and ambiguous generic 5xx responses. Unit tests cover rules and fallback cases. |
| Hybrid triage | Clear rule results bypass inference; ambiguous results are routed to the model. Stub-based tests verify routing and request/response handling. |
| Ollama integration | Java 21 HttpClient calls local `qa-triage-hermes`; Jackson validates model output. A real-model integration test exists and is disabled by default. |
| JavaFX human review | Implemented file loading, suggestion display, final classification selection, QA comments, and JSON review saving under `target/qa-reviews/`. Writer tests cover persistence; interactive UI validation remains pending. |

The collector is enabled explicitly on evidence-enabled tests, not globally. Four disabled failure demos provide controlled evidence-generation scenarios. The Modelfile supplies persistent system instructions and examples without training or fine-tuning model weights.

## Test Layers and Execution

- **API tests:** exercise the public demo service and require internet access.
- **Rule engine unit tests:** use constructed evidence to check classifications, confidence, and routing flags.
- **Hybrid service tests:** use a local HTTP stub to verify bypass/routing, payload serialization, output validation, and HTTP error handling. They do not require Ollama.
- **Review writer unit tests:** use temporary directories to verify JSON fields, approval semantics, separate output files, and write errors. They do not initialize JavaFX.
- **Controlled failure demos:** disabled by default; temporarily enable one and run it alone to generate evidence. An intentional test failure is expected.
- **Real Ollama integration:** disabled by default; requires the running local service and custom model. Assert valid result structure rather than one exact classification.

Run the normal suite with `mvn test`. For manual validation, temporarily remove `@Disabled` from the selected demo or integration class, run it using `mvn -Dtest=ClassName test`, and restore the annotation afterward.

Latest validated normal suite: **33 passing tests, 5 skipped tests, 0 failures or errors**. This result covers automated tests; it does not establish interactive JavaFX validation or successful real-model inference.

## Planned / Future Scope

These items are not implemented:

- Selenium browser UI automation and related failure evidence.
- Screenshots on failure.
- Flaky-test analysis.
- A CI pipeline, such as GitHub Actions.

Broader API coverage, including login scenarios, and browser business flows such as registration, cart, and checkout remain possible later extensions; they are not current test coverage.

Portfolio screenshots of the reviewer and JSON outputs are also pending. These documentation images are separate from future automated screenshots on failure.

## Pending Manual Validation

- Run the disabled integration test with the real local model and inspect its output.
- Open evidence in the JavaFX UI, verify the suggestion, keep and override classifications, add a comment, and inspect saved review JSON.
- Check UI handling of invalid evidence and unavailable Ollama.

## Quality Principles

- Classify only what the evidence supports; preserve `UNKNOWN` for insufficient or ambiguous evidence.
- Do not treat a generic HTTP 5xx as proof of a product or environment defect.
- Keep the original suggestion and final human classification distinct; QA owns the final decision.
- Prefer deterministic assertions for rules and JSON contracts; do not require exact probabilistic model wording.
- Keep intentional failures and real-model checks out of normal suite runs.

## Outside Current Scope

Performance testing, security penetration testing, mobile testing, and direct database validation are outside the current scope. The public demo application does not expose database access. No database, dashboard, or authentication layer is implemented in the triage tool.
