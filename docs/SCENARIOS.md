# Required Scenario Demonstrations

## 1. Greenfield

**Input:** `Build a URL shortener with analytics and reliability features.`

**Decomposition:** requirements → architecture → implementation → testing + documentation → release.

**Orchestration:** testing and documentation execute in parallel after implementation; release waits for both.

**Validation:** API integration tests, URL validation, analytics click increment, workflow audit and release gate.

**Run:** create a workflow with `scenario=GREENFIELD`. For a controlled demo use `autoApprove=false` and approve REQUIREMENTS, IMPLEMENTATION and RELEASE as each gate becomes available.

## 2. Brownfield

**Input:** `Enhance the existing shortener to add analytics and improve reliability.`

**Decomposition:** identify impacted API/service/data-flow modules → preserve existing redirect contract → add analytics → add validation/reliability tests → documentation → release.

**Orchestration:** the same DAG is reused, showing that the workflow is not tied to a greenfield-only sequence.

**Validation:** existing behavior remains covered by integration tests while new analytics behavior is verified.

## 3. Ambiguous

**Input:** `Need a shortener that is secure, fast and enterprise ready.`

**Ambiguities identified:** authentication model, URL retention, custom aliases, rate limits, analytics retention, compliance classification, expected traffic, availability target.

**Agent decision:** normalize only safe assumptions for the prototype; record unresolved items as decisions/risks rather than silently inventing requirements.

**Dynamic re-plan:** use `POST /api/workflows/{id}/replan` when stakeholder feedback changes the upstream requirement. Architecture and all downstream stages are invalidated and must be re-approved before autonomous continuation.
