package com.schwab.agenticurl.orchestration;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

@Service
public class AgenticOrchestrator {
    private static final int MAX_RETRIES = 2;
    private final Map<String, WorkflowState> workflows = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public WorkflowState create(WorkflowRequest request) {
        String id = UUID.randomUUID().toString();
        ScenarioType scenario = request.scenario() == null ? ScenarioType.GREENFIELD : request.scenario();
        WorkflowState state = new WorkflowState(id, scenario, request.requirement());
        state.getStages().put(Stage.REQUIREMENTS, new StageState(Stage.REQUIREMENTS, List.of()));
        state.getStages().put(Stage.ARCHITECTURE, new StageState(Stage.ARCHITECTURE, List.of(Stage.REQUIREMENTS)));
        state.getStages().put(Stage.IMPLEMENTATION, new StageState(Stage.IMPLEMENTATION, List.of(Stage.ARCHITECTURE)));
        state.getStages().put(Stage.TESTING, new StageState(Stage.TESTING, List.of(Stage.IMPLEMENTATION)));
        state.getStages().put(Stage.DOCUMENTATION, new StageState(Stage.DOCUMENTATION, List.of(Stage.IMPLEMENTATION)));
        state.getStages().put(Stage.RELEASE, new StageState(Stage.RELEASE, List.of(Stage.TESTING, Stage.DOCUMENTATION)));
        state.audit("WORKFLOW_CREATED scenario=" + scenario);
        workflows.put(id, state);
        if (Boolean.TRUE.equals(request.autoApprove())) {
            WorkflowState s = new WorkflowState(
                    UUID.randomUUID().toString(),
                    request.scenario(),
                    request.requirement()
            );

            // Now s is available
            s.getApprovals().add(Stage.REQUIREMENTS);
            s.getApprovals().add(Stage.IMPLEMENTATION);
            s.getApprovals().add(Stage.RELEASE);

            s.audit("AUTO_APPROVAL_ENABLED demoMode=true");
            executor.submit(() -> run(id));
        } else {
            state.setStatus(WorkflowStatus.WAITING_APPROVAL);
            state.audit("HUMAN_GATE_REQUIRED before autonomous execution; approve REQUIREMENTS to start");
        }
        return state;
    }

    public WorkflowState get(String id) {
        WorkflowState s = workflows.get(id);
        if (s == null) throw new NoSuchElementException("Workflow not found: " + id);
        return s;
    }

    public void approve(String id, ApprovalRequest request) {
        WorkflowState s = get(id);
        if (request.stage() == null) throw new IllegalArgumentException("stage is required");
        if (request.approved()) {
            s.getApprovals().add(request.stage());
            s.audit("HUMAN_APPROVAL stage=" + request.stage() + " reason=" + safe(request.reason()));
            executor.submit(() -> run(id));
        } else {
            safeStop(s, "Human rejected gate for " + request.stage() + ": " + safe(request.reason()));
        }
    }

    public void safeStop(String id, String reason) { safeStop(get(id), reason); }

    public void replan(String id, String changedRequirement) {
        WorkflowState s = get(id);
        synchronized (s) {
            s.setStatus(WorkflowStatus.REPLANNING);
            s.audit("REPLAN_REQUESTED upstream requirement changed");
            s.getContext().put("replannedRequirement", changedRequirement);
            for (Stage stage : List.of(Stage.ARCHITECTURE, Stage.IMPLEMENTATION, Stage.TESTING, Stage.DOCUMENTATION, Stage.RELEASE)) {
                StageState st = s.getStages().get(stage);
                st.setStatus(StageStatus.PENDING); st.setOutput(null); st.setError(null);
            }
            s.audit("DOWNSTREAM_INVALIDATED stages=ARCHITECTURE,IMPLEMENTATION,TESTING,DOCUMENTATION,RELEASE");
            s.setStatus(WorkflowStatus.WAITING_APPROVAL);
        }
    }

    private void run(String id) {
        WorkflowState s = get(id);
        synchronized (s) {
            if (s.getStatus() == WorkflowStatus.SAFE_STOPPED || s.getStatus() == WorkflowStatus.SUCCEEDED) return;
            s.setStatus(WorkflowStatus.RUNNING);
            if (s.getStartedAt() == null) s.setStartedAt(Instant.now());
        }
        try {
            boolean progress;
            do {
                progress = false;
                List<Stage> ready = readyStages(s);
                List<Stage> parallel = ready.stream().filter(x -> x == Stage.TESTING || x == Stage.DOCUMENTATION).toList();
                if (!parallel.isEmpty()) {
                    List<Future<?>> futures = parallel.stream()
                            .map(stage -> (Future<?>) executor.submit(() -> executeStage(s, stage)))
                            .collect(Collectors.toList());
                    for (Future<?> f : futures) f.get();
                    progress = true;
                }
                for (Stage stage : readyStages(s)) {
                    if (stage == Stage.TESTING || stage == Stage.DOCUMENTATION) continue;
                    executeStage(s, stage);
                    progress = true;
                    if (s.getStatus() == WorkflowStatus.SAFE_STOPPED || s.getStatus() == WorkflowStatus.WAITING_APPROVAL) return;
                }
                if (allSucceeded(s)) {
                    s.setStatus(WorkflowStatus.SUCCEEDED); s.setFinishedAt(Instant.now());
                    s.audit("WORKFLOW_COMPLETED latencyMs=" + latency(s)); return;
                }
                if (hasApprovalBlock(s)) {
                    s.setStatus(WorkflowStatus.WAITING_APPROVAL);
                    s.audit("HUMAN_GATE_REQUIRED pending=" + pendingApprovalStage(s));
                    return;
                }
            } while (progress && s.getStatus() == WorkflowStatus.RUNNING);
        } catch (Exception e) {
            safeStop(s, "Orchestrator execution error: " + e.getMessage());
        }
    }

    private List<Stage> readyStages(WorkflowState s) {
        return Arrays.stream(Stage.values()).filter(stage -> {
            StageState st = s.getStages().get(stage);
            if (st.getStatus() != StageStatus.PENDING && st.getStatus() != StageStatus.READY) return false;
            if (requiresApproval(stage) && !s.getApprovals().contains(stage)) return false;
            return st.getDependencies().stream().map(Stage::valueOf)
                    .allMatch(dep -> s.getStages().get(dep).getStatus() == StageStatus.SUCCEEDED);
        }).toList();
    }

    private void executeStage(WorkflowState s, Stage stage) {
        StageState st = s.getStages().get(stage);
        st.setStatus(StageStatus.RUNNING); st.setStartedAt(Instant.now()); st.setAttempts(st.getAttempts() + 1);
        s.audit("STAGE_STARTED " + stage + " attempt=" + st.getAttempts());
        try {
            String output = generate(stage, s);
            st.setOutput(output); st.setStatus(StageStatus.SUCCEEDED); st.setFinishedAt(Instant.now());
            s.getContext().put(stage.name() + ".output", output);
            s.audit("STAGE_SUCCEEDED " + stage);
        } catch (RuntimeException e) {
            st.setError(e.getMessage());
            if (st.getAttempts() <= MAX_RETRIES) {
                st.setStatus(StageStatus.RETRYING); s.incRetry(); s.audit("RETRY_SCHEDULED " + stage + " reason=" + e.getMessage());
                st.setStatus(StageStatus.PENDING);
            } else {
                st.setStatus(StageStatus.FAILED); rollback(s, stage); safeStop(s, "Stage failed after bounded retries: " + stage);
            }
        }
        if (stage == Stage.IMPLEMENTATION && s.getScenario() != ScenarioType.AMBIGUOUS && !s.getApprovals().contains(Stage.IMPLEMENTATION)) {
            s.setStatus(WorkflowStatus.WAITING_APPROVAL);
            s.audit("HUMAN_GATE_REQUIRED before implementation is accepted");
        }
    }

    private String generate(Stage stage, WorkflowState s) {
        String req = (String) s.getContext().getOrDefault("replannedRequirement", s.getRequirement());
        return switch (stage) {
            case REQUIREMENTS -> "Normalized requirement\n- Intent: " + req + "\n- Acceptance: API, analytics, reliability, governance\n- Ambiguities: retention, auth, rate limits\n- Decision: safe defaults documented";
            case ARCHITECTURE -> "Architecture decision\n- URL API + redirect + analytics\n- H2 default / PostgreSQL-ready JPA\n- Agent DAG with gates, parallel test/docs, audit trail\n- Stateless API, stateful workflow registry\n- Human approval before implementation/release-impacting actions";
            case IMPLEMENTATION -> "Implementation artifact\n- Core shortening endpoint\n- Redirect endpoint increments analytics\n- Validation and centralized errors\n- Scenario: " + s.getScenario();
            case TESTING -> "Validation artifact\n- Unit: code generation/URL validation/service behavior\n- Integration: API create + analytics + redirect\n- Governance: bounded retry, safe-stop, approval, replan\n- Result: prototype test suite defined and executable";
            case DOCUMENTATION -> "Documentation artifact\n- README with setup/run/test instructions\n- Architecture and control-flow diagrams\n- Risks, trade-offs, assumptions, limitations\n- Three scenario walkthroughs";
            case RELEASE -> "Release-readiness gate\n- Required stages complete\n- Tests/documentation synchronized\n- Audit trail retained\n- Human approval: " + s.getApprovals().contains(Stage.RELEASE) + "\n- Decision: ready for controlled release";
        };
    }

    private boolean requiresApproval(Stage stage) { return stage == Stage.REQUIREMENTS || stage == Stage.IMPLEMENTATION || stage == Stage.RELEASE; }
    private boolean hasApprovalBlock(WorkflowState s) {
        return Arrays.stream(Stage.values()).anyMatch(x -> requiresApproval(x)
                && (s.getStages().get(x).getStatus() == StageStatus.PENDING || s.getStages().get(x).getStatus() == StageStatus.READY)
                && s.getStages().get(x).getDependencies().stream().map(Stage::valueOf)
                    .allMatch(dep -> s.getStages().get(dep).getStatus() == StageStatus.SUCCEEDED)
                && !s.getApprovals().contains(x));
    }
    private String pendingApprovalStage(WorkflowState s) {
        return Arrays.stream(Stage.values()).filter(x -> requiresApproval(x) && !s.getApprovals().contains(x)
                && s.getStages().get(x).getStatus() != StageStatus.SUCCEEDED).map(Enum::name).findFirst().orElse("unknown");
    }
    private boolean allSucceeded(WorkflowState s) { return Arrays.stream(Stage.values()).allMatch(x -> s.getStages().get(x).getStatus() == StageStatus.SUCCEEDED); }
    private void rollback(WorkflowState s, Stage failed) { s.incRollback(); s.audit("ROLLBACK_INITIATED failedStage=" + failed + " compensatingAction=downstream-invalidation"); }
    private void safeStop(WorkflowState s, String reason) { synchronized (s) { s.setStatus(WorkflowStatus.SAFE_STOPPED); s.setStopReason(reason); s.audit("SAFE_STOP " + reason); s.setFinishedAt(Instant.now()); } }
    private long latency(WorkflowState s) { return s.getStartedAt() == null ? 0 : Duration.between(s.getStartedAt(), s.getFinishedAt()).toMillis(); }
    private String safe(String x) { return x == null ? "" : x.replaceAll("[\r\n]", " "); }
}
